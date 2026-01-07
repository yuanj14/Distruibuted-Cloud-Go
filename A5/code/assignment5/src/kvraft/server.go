package raftkv

import (
	"encoding/gob"
	"log"
	"src/labrpc"
	"src/raft"
	"sync"
	"time"
)

const Debug = 0

func DPrintf(format string, a ...interface{}) (n int, err error) {
	if Debug > 0 {
		log.Printf(format, a...)
	}
	return
}

type Op struct {
	Operation string // "Get", "Put", or "Append"
	Key       string
	Value     string
	ClientId  int64
	RequestId int
}

type RaftKV struct {
	mu      sync.Mutex
	me      int
	rf      *raft.Raft
	applyCh chan raft.ApplyMsg

	maxraftstate int // snapshot if log grows this big

	kvStore       map[string]string  // the key-value store
	lastApplied   map[int64]int      // last applied request ID for each client
	notifyCh      map[int]chan Op    // notification channels for waiting RPCs
	lastAppliedIndex int             // last applied log index
}

func (kv *RaftKV) Get(args *GetArgs, reply *GetReply) {
	// Create operation
	op := Op{
		Operation: "Get",
		Key:       args.Key,
		ClientId:  args.ClientId,
		RequestId: args.RequestId,
	}

	// Try to append to Raft log
	index, _, isLeader := kv.rf.Start(op)
	if !isLeader {
		reply.WrongLeader = true
		return
	}

	// Wait for operation to be applied
	kv.mu.Lock()
	ch := make(chan Op, 1)
	kv.notifyCh[index] = ch
	kv.mu.Unlock()

	// Wait for result or timeout
	select {
	case appliedOp := <-ch:
		// Check if this is the operation we submitted
		if appliedOp.ClientId == op.ClientId && appliedOp.RequestId == op.RequestId {
			kv.mu.Lock()
			value, exists := kv.kvStore[args.Key]
			kv.mu.Unlock()
			
			reply.WrongLeader = false
			if exists {
				reply.Err = OK
				reply.Value = value
			} else {
				reply.Err = ErrNoKey
				reply.Value = ""
			}
		} else {
			// Wrong operation committed at this index (we lost leadership)
			reply.WrongLeader = true
		}
	case <-time.After(1000 * time.Millisecond):
		reply.WrongLeader = true
	}

	// Clean up
	kv.mu.Lock()
	delete(kv.notifyCh, index)
	kv.mu.Unlock()
}

func (kv *RaftKV) PutAppend(args *PutAppendArgs, reply *PutAppendReply) {
	// Create operation
	op := Op{
		Operation: args.Op,
		Key:       args.Key,
		Value:     args.Value,
		ClientId:  args.ClientId,
		RequestId: args.RequestId,
	}

	// Try to append to Raft log
	index, _, isLeader := kv.rf.Start(op)
	if !isLeader {
		reply.WrongLeader = true
		return
	}

	// Wait for operation to be applied
	kv.mu.Lock()
	ch := make(chan Op, 1)
	kv.notifyCh[index] = ch
	kv.mu.Unlock()

	// Wait for result or timeout
	select {
	case appliedOp := <-ch:
		// Check if this is the operation we submitted
		if appliedOp.ClientId == op.ClientId && appliedOp.RequestId == op.RequestId {
			reply.WrongLeader = false
			reply.Err = OK
		} else {
			// Wrong operation committed at this index (we lost leadership)
			reply.WrongLeader = true
		}
	case <-time.After(1000 * time.Millisecond):
		reply.WrongLeader = true
	}

	// Clean up
	kv.mu.Lock()
	delete(kv.notifyCh, index)
	kv.mu.Unlock()
}

//
// the tester calls Kill() when a RaftKV instance won't
// be needed again. you are not required to do anything
// in Kill(), but it might be convenient to (for example)
// turn off debug output from this instance.
//
func (kv *RaftKV) Kill() {
	kv.rf.Kill()
	// Close apply channel to stop applyLoop
	// Note: applyCh is managed by Raft, so we don't close it here
}

//
// Apply committed log entries to state machine
//
func (kv *RaftKV) applyLoop() {
	for msg := range kv.applyCh {
		if msg.UseSnapshot {
			// Snapshot handling - not required for basic assignment
			continue
		}
		
		kv.mu.Lock()
		
		// Type assertion with check to prevent panic
		op, ok := msg.Command.(Op)
		if !ok {
			kv.mu.Unlock()
			DPrintf("KV[%d] received non-Op command at index %d", kv.me, msg.Index)
			continue
		}
		
		// Check for duplicate detection (at-most-once semantics)
		lastReq, exists := kv.lastApplied[op.ClientId]
		isDuplicate := exists && op.RequestId <= lastReq
		
		if !isDuplicate {
			// Apply the operation to state machine
			switch op.Operation {
			case "Put":
				kv.kvStore[op.Key] = op.Value
			case "Append":
				kv.kvStore[op.Key] += op.Value
			case "Get":
				// Read-only, no state change
			}
			kv.lastApplied[op.ClientId] = op.RequestId
		}
		
		kv.lastAppliedIndex = msg.Index
		
		// Notify waiting RPC handler (if any)
		if ch, ok := kv.notifyCh[msg.Index]; ok {
			select {
			case ch <- op:
			default:
				// Channel full or closed, continue
			}
		}
		
		kv.mu.Unlock()
	}
}

//
// servers[] contains the ports of the set of
// servers that will cooperate via Raft to
// form the fault-tolerant key/value service.
// me is the index of the current server in servers[].
// the k/v server should store snapshots with persister.SaveSnapshot(),
// and Raft should save its state (including log) with persister.SaveRaftState().
// the k/v server should snapshot when Raft's saved state exceeds maxraftstate bytes,
// in order to allow Raft to garbage-collect its log. if maxraftstate is -1,
// you don't need to snapshot.
// StartKVServer() must return quickly, so it should start goroutines
// for any long-running work.
//
func StartKVServer(servers []*labrpc.ClientEnd, me int, persister *raft.Persister, maxraftstate int) *RaftKV {
	// call gob.Register on structures you want
	// Go's RPC library to marshall/unmarshall.
	gob.Register(Op{})

	kv := new(RaftKV)
	kv.me = me
	kv.maxraftstate = maxraftstate

	kv.kvStore = make(map[string]string)
	kv.lastApplied = make(map[int64]int)
	kv.notifyCh = make(map[int]chan Op)
	kv.lastAppliedIndex = 0

	kv.applyCh = make(chan raft.ApplyMsg)
	kv.rf = raft.Make(servers, me, persister, kv.applyCh)

	// Start background goroutine to apply committed entries
	go kv.applyLoop()

	return kv
}
