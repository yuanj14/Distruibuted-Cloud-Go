package raftkv

import (
	"crypto/rand"
	"math/big"
	"src/labrpc"
)

type Clerk struct {
	servers   []*labrpc.ClientEnd
	clientId  int64 // unique identifier for this client
	requestId int   // monotonically increasing request counter
	leaderId  int   // cached leader ID for optimization
}

func nrand() int64 {
	max := big.NewInt(int64(1) << 62)
	bigx, _ := rand.Int(rand.Reader, max)
	x := bigx.Int64()
	return x
}

func MakeClerk(servers []*labrpc.ClientEnd) *Clerk {
	ck := new(Clerk)
	ck.servers = servers
	ck.clientId = nrand()
	ck.requestId = 0
	ck.leaderId = 0
	return ck
}

//
// fetch the current value for a key.
// returns "" if the key does not exist.
// keeps trying forever in the face of all other errors.
//
func (ck *Clerk) Get(key string) string {
	ck.requestId++
	args := GetArgs{
		Key:       key,
		ClientId:  ck.clientId,
		RequestId: ck.requestId,
	}

	for {
		reply := GetReply{}
		ok := ck.servers[ck.leaderId].Call("RaftKV.Get", &args, &reply)
		
		if ok && !reply.WrongLeader {
			if reply.Err == OK {
				return reply.Value
			} else if reply.Err == ErrNoKey {
				return ""
			}
		}
		
		// Try next server
		ck.leaderId = (ck.leaderId + 1) % len(ck.servers)
	}
}

//
// shared by Put and Append.
//
func (ck *Clerk) PutAppend(key string, value string, op string) {
	ck.requestId++
	args := PutAppendArgs{
		Key:       key,
		Value:     value,
		Op:        op,
		ClientId:  ck.clientId,
		RequestId: ck.requestId,
	}

	for {
		reply := PutAppendReply{}
		ok := ck.servers[ck.leaderId].Call("RaftKV.PutAppend", &args, &reply)
		
		if ok && !reply.WrongLeader && reply.Err == OK {
			return
		}
		
		// Try next server
		ck.leaderId = (ck.leaderId + 1) % len(ck.servers)
	}
}

func (ck *Clerk) Put(key string, value string) {
	ck.PutAppend(key, value, "Put")
}

func (ck *Clerk) Append(key string, value string) {
	ck.PutAppend(key, value, "Append")
}
