package raft

//
// this is an outline of the API that raft must expose to
// the service (or tester). see comments below for
// each of these functions for more details.
//
// rf = Make(...)
//   create a new Raft server.
// rf.Start(command interface{}) (index, term, isleader)
//   start agreement on a new log entry
// rf.GetState() (term, isLeader)
//   ask a Raft for its current term, and whether it thinks it is leader
// ApplyMsg
//   each time a new entry is committed to the log, each Raft peer
//   should send an ApplyMsg to the service (or tester)
//   in the same server.
//

import (
	"bytes"
	"encoding/gob"
	"math/rand"
	"src/labrpc"
	"sync"
	"time"
)

//
// as each Raft peer becomes aware that successive log entries are
// committed, the peer should send an ApplyMsg to the service (or
// tester) on the same server, via the applyCh passed to Make().
//
type ApplyMsg struct {
	Index       int
	Command     interface{}
	UseSnapshot bool   // ignore for lab2; only used in lab3
	Snapshot    []byte // ignore for lab2; only used in lab3
}

//
// A Go object implementing a single Raft peer.
//
type Raft struct {
	mu        sync.Mutex
	peers     []*labrpc.ClientEnd
	persister *Persister
	me        int // index into peers[]

	// Your data here.
	// Look at the paper's Figure 2 for a description of what
	// state a Raft server must maintain.

	// Persistent state on all servers (Updated on stable storage before responding to RPCs)
	currentTerm int // latest term server has seen (initialized to 0 on first boot, increases monotonically)
	votedFor    int // candidateId that received vote in current term (or -1 if none)
	log         []LogEntry // log entries; each entry contains command for state machine, and term when entry was received by leader (first index is 1)

	// Volatile state on all servers
	commitIndex int // index of highest log entry known to be committed (initialized to 0, increases monotonically)
	lastApplied int // index of highest log entry applied to state machine (initialized to 0, increases monotonically)

	// Volatile state on leaders (Reinitialized after election)
	nextIndex  []int // for each server, index of the next log entry to send to that server (initialized to leader last log index + 1)
	matchIndex []int // for each server, index of highest log entry known to be replicated on server (initialized to 0, increases monotonically)

	// Additional state for implementation
	state           string    // "Follower", "Candidate", "Leader"
	electionTimer  *time.Timer
	heartbeatTimer *time.Timer
	applyCh        chan ApplyMsg
}

// Log entry structure
type LogEntry struct {
	Term    int
	Command interface{}
}

// return currentTerm and whether this server
// believes it is the leader.
func (rf *Raft) GetState() (int, bool) {
	rf.mu.Lock()
	defer rf.mu.Unlock()

	term := rf.currentTerm
	isleader := rf.state == "Leader"
	return term, isleader
}

//
// save Raft's persistent state to stable storage,
// where it can later be retrieved after a crash and restart.
// see paper's Figure 2 for a description of what should be persistent.
//
func (rf *Raft) persist() {
	w := new(bytes.Buffer)
	e := gob.NewEncoder(w)
	e.Encode(rf.currentTerm)
	e.Encode(rf.votedFor)
	e.Encode(rf.log)
	data := w.Bytes()
	rf.persister.SaveRaftState(data)
}

//
// restore previously persisted state.
//
func (rf *Raft) readPersist(data []byte) {
	if data == nil || len(data) < 1 { // bootstrap without any state?
		return
	}
	r := bytes.NewBuffer(data)
	d := gob.NewDecoder(r)
	d.Decode(&rf.currentTerm)
	d.Decode(&rf.votedFor)
	d.Decode(&rf.log)
}

//
// RequestVote RPC arguments structure.
//
type RequestVoteArgs struct {
	Term         int // candidate's term
	CandidateId  int // candidate requesting vote
	LastLogIndex int // index of candidate's last log entry
	LastLogTerm  int // term of candidate's last log entry
}

//
// RequestVote RPC reply structure.
//
type RequestVoteReply struct {
	Term        int  // currentTerm, for candidate to update itself
	VoteGranted bool // true means candidate received vote
}

//
// AppendEntries RPC arguments structure.
//
type AppendEntriesArgs struct {
	Term         int        // leader's term
	LeaderId     int        // so follower can redirect clients
	PrevLogIndex int        // index of log entry immediately preceding new ones
	PrevLogTerm  int        // term of prevLogIndex entry
	Entries      []LogEntry // log entries to store (empty for heartbeat; may send more than one for efficiency)
	LeaderCommit int        // leader's commitIndex
}

//
// AppendEntries RPC reply structure.
//
type AppendEntriesReply struct {
	Term          int  // currentTerm, for leader to update itself
	Success       bool // true if follower contained entry matching prevLogIndex and prevLogTerm
	ConflictIndex int  // optimization for fast backup
	ConflictTerm  int  // optimization for fast backup
}

//
// RequestVote RPC handler.
//
func (rf *Raft) RequestVote(args *RequestVoteArgs, reply *RequestVoteReply) {
	rf.mu.Lock()
	defer rf.mu.Unlock()

	// Initialize reply with current term
	reply.Term = rf.currentTerm
	reply.VoteGranted = false

	// Rule 1: Reply false if term < currentTerm
	if args.Term < rf.currentTerm {
		return
	}

	// If RPC request contains term T > currentTerm: set currentTerm = T, convert to follower
	if args.Term > rf.currentTerm {
		rf.currentTerm = args.Term
		rf.votedFor = -1
		rf.state = "Follower"
		rf.persist()
		reply.Term = rf.currentTerm
	}

	// Rule 2: If votedFor is null or candidateId, and candidate's log is at least as up-to-date as receiver's log, grant vote
	lastLogIndex := len(rf.log) - 1
	lastLogTerm := 0
	if lastLogIndex >= 0 {
		lastLogTerm = rf.log[lastLogIndex].Term
	}

	// Check if candidate's log is at least as up-to-date
	upToDate := false
	if args.LastLogTerm > lastLogTerm {
		upToDate = true
	} else if args.LastLogTerm == lastLogTerm && args.LastLogIndex >= lastLogIndex {
		upToDate = true
	}

	if (rf.votedFor == -1 || rf.votedFor == args.CandidateId) && upToDate {
		reply.VoteGranted = true
		rf.votedFor = args.CandidateId
		rf.persist()
		// Reset election timer when granting vote
		rf.resetElectionTimer()
	}
}

//
// example code to send a RequestVote RPC to a server.
// server is the index of the target server in rf.peers[].
// expects RPC arguments in args.
// fills in *reply with RPC reply, so caller should
// pass &reply.
// the types of the args and reply passed to Call() must be
// the same as the types of the arguments declared in the
// handler function (including whether they are pointers).
//
// returns true if labrpc says the RPC was delivered.
//
// if you're having trouble getting RPC to work, check that you've
// capitalized all field names in structs passed over RPC, and
// that the caller passes the address of the reply struct with &, not
// the struct itself.
//
func (rf *Raft) sendRequestVote(server int, args *RequestVoteArgs, reply *RequestVoteReply) bool {
	ok := rf.peers[server].Call("Raft.RequestVote", args, reply)
	return ok
}

//
// the service using Raft (e.g. a k/v server) wants to start
// agreement on the next command to be appended to Raft's log. if this
// server isn't the leader, returns false. otherwise start the
// agreement and return immediately. there is no guarantee that this
// command will ever be committed to the Raft log, since the leader
// may fail or lose an election.
//
// the first return value is the index that the command will appear at
// if it's ever committed. the second return value is the current
// term. the third return value is true if this server believes it is
// the leader.
//
func (rf *Raft) Start(command interface{}) (int, int, bool) {
	rf.mu.Lock()
	defer rf.mu.Unlock()

	index := -1
	term := rf.currentTerm
	isLeader := rf.state == "Leader"

	if isLeader {
		// Append entry to local log
		index = len(rf.log)
		rf.log = append(rf.log, LogEntry{Term: rf.currentTerm, Command: command})
		rf.persist()
	}

	return index, term, isLeader
}

//
// AppendEntries RPC handler.
//
func (rf *Raft) AppendEntries(args *AppendEntriesArgs, reply *AppendEntriesReply) {
	rf.mu.Lock()
	defer rf.mu.Unlock()

	// Initialize reply
	reply.Term = rf.currentTerm
	reply.Success = false
	reply.ConflictIndex = -1
	reply.ConflictTerm = -1

	// Rule 1: Reply false if term < currentTerm
	if args.Term < rf.currentTerm {
		return
	}

	// If RPC request contains term T > currentTerm: set currentTerm = T, convert to follower
	if args.Term > rf.currentTerm {
		rf.currentTerm = args.Term
		rf.votedFor = -1
		rf.persist()
	}

	// Convert to follower and reset election timer on valid AppendEntries
	rf.state = "Follower"
	rf.resetElectionTimer()
	reply.Term = rf.currentTerm

	// Rule 2: Reply false if log doesn't contain an entry at prevLogIndex whose term matches prevLogTerm
	if args.PrevLogIndex >= len(rf.log) {
		reply.ConflictIndex = len(rf.log)
		reply.ConflictTerm = -1
		return
	}
	
	if args.PrevLogIndex > 0 && rf.log[args.PrevLogIndex].Term != args.PrevLogTerm {
		reply.ConflictTerm = rf.log[args.PrevLogIndex].Term
		// Find the first index of this term
		for i := args.PrevLogIndex; i > 0; i-- {
			if rf.log[i].Term == reply.ConflictTerm {
				reply.ConflictIndex = i
			} else {
				break
			}
		}
		return
	}

	// Rule 3 & 4: If an existing entry conflicts with a new one (same index but different terms),
	// delete the existing entry and all that follow it, then append new entries
	if len(args.Entries) > 0 {
		// Find conflict
		conflictIndex := -1
		conflictEntryIndex := -1
		for i, entry := range args.Entries {
			logIndex := args.PrevLogIndex + 1 + i
			if logIndex >= len(rf.log) {
				// Need to append
				rf.log = append(rf.log, args.Entries[i:]...)
				rf.persist()
				break
			} else if rf.log[logIndex].Term != entry.Term {
				// Conflict found
				conflictIndex = logIndex
				conflictEntryIndex = i
				break
			}
		}
		
		if conflictIndex >= 0 {
			// Delete conflicting entries and append new ones
			rf.log = rf.log[:conflictIndex]
			rf.log = append(rf.log, args.Entries[conflictEntryIndex:]...)
			rf.persist()
		}
	}

	// Rule 5: If leaderCommit > commitIndex, set commitIndex = min(leaderCommit, index of last new entry)
	if args.LeaderCommit > rf.commitIndex {
		rf.commitIndex = min(args.LeaderCommit, len(rf.log)-1)
		go rf.applyCommittedEntries()
	}

	reply.Success = true
}

//
// send AppendEntries RPC to a server.
//
func (rf *Raft) sendAppendEntries(server int, args *AppendEntriesArgs, reply *AppendEntriesReply) bool {
	ok := rf.peers[server].Call("Raft.AppendEntries", args, reply)
	return ok
}

//
// Helper function to get minimum of two integers
//
func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}

//
// Apply committed entries to state machine
//
func (rf *Raft) applyCommittedEntries() {
	rf.mu.Lock()
	defer rf.mu.Unlock()

	for rf.lastApplied < rf.commitIndex {
		rf.lastApplied++
		if rf.lastApplied < len(rf.log) {
			msg := ApplyMsg{
				Index:   rf.lastApplied,
				Command: rf.log[rf.lastApplied].Command,
			}
			rf.applyCh <- msg
		}
	}
}

//
// Reset election timer with random timeout
//
func (rf *Raft) resetElectionTimer() {
	// Random timeout between 150-300ms
	timeout := time.Duration(150+rand.Intn(150)) * time.Millisecond
	
	if rf.electionTimer == nil {
		rf.electionTimer = time.NewTimer(timeout)
	} else {
		if !rf.electionTimer.Stop() {
			select {
			case <-rf.electionTimer.C:
			default:
			}
		}
		rf.electionTimer.Reset(timeout)
	}
}

//
// Start election process
//
func (rf *Raft) startElection() {
	rf.mu.Lock()
	
	// Convert to candidate
	rf.state = "Candidate"
	rf.currentTerm++
	rf.votedFor = rf.me
	rf.persist()
	rf.resetElectionTimer()
	
	currentTerm := rf.currentTerm
	lastLogIndex := len(rf.log) - 1
	lastLogTerm := 0
	if lastLogIndex >= 0 {
		lastLogTerm = rf.log[lastLogIndex].Term
	}
	
	rf.mu.Unlock()

	// Send RequestVote RPCs to all other servers
	votes := 1 // Vote for self
	
	for i := range rf.peers {
		if i != rf.me {
			go func(server int) {
				args := &RequestVoteArgs{
					Term:         currentTerm,
					CandidateId:  rf.me,
					LastLogIndex: lastLogIndex,
					LastLogTerm:  lastLogTerm,
				}
				reply := &RequestVoteReply{}
				
				if rf.sendRequestVote(server, args, reply) {
					rf.mu.Lock()
					defer rf.mu.Unlock()
					
					// Check if we're still a candidate and in the same term
					if rf.state != "Candidate" || rf.currentTerm != currentTerm {
						return
					}
					
					// If reply term is higher, convert to follower
					if reply.Term > rf.currentTerm {
						rf.currentTerm = reply.Term
						rf.votedFor = -1
						rf.state = "Follower"
						rf.persist()
						rf.resetElectionTimer()
						return
					}
					
					// Count votes
					if reply.VoteGranted {
						votes++
						// Check if we have majority
						if votes > len(rf.peers)/2 {
							rf.becomeLeader()
						}
					}
				}
			}(i)
		}
	}
}

//
// Become leader
//
func (rf *Raft) becomeLeader() {
	if rf.state != "Candidate" {
		return
	}
	
	rf.state = "Leader"
	
	// Initialize leader state
	for i := range rf.nextIndex {
		rf.nextIndex[i] = len(rf.log)
	}
	for i := range rf.matchIndex {
		rf.matchIndex[i] = 0
	}
	
	// Send initial heartbeats
	go rf.broadcastAppendEntries()
	
	// Start heartbeat timer
	if rf.heartbeatTimer != nil {
		rf.heartbeatTimer.Stop()
	}
	rf.heartbeatTimer = time.NewTimer(50 * time.Millisecond)
}

//
// Send AppendEntries to all followers (Heartbeats or Log Replication)
//
func (rf *Raft) broadcastAppendEntries() {
	rf.mu.Lock()
	defer rf.mu.Unlock()
	
	if rf.state != "Leader" {
		return
	}
	
	for i := range rf.peers {
		if i != rf.me {
			go func(server int) {
				rf.mu.Lock()
				if rf.state != "Leader" {
					rf.mu.Unlock()
					return
				}
				
				nextIdx := rf.nextIndex[server]
				prevLogIndex := nextIdx - 1
				prevLogTerm := 0
				if prevLogIndex >= 0 && prevLogIndex < len(rf.log) {
					prevLogTerm = rf.log[prevLogIndex].Term
				}
				
				// Prepare entries to send
				var entries []LogEntry
				if nextIdx < len(rf.log) {
					entries = make([]LogEntry, len(rf.log)-nextIdx)
					copy(entries, rf.log[nextIdx:])
				} else {
					entries = []LogEntry{}
				}

				args := &AppendEntriesArgs{
					Term:         rf.currentTerm,
					LeaderId:     rf.me,
					PrevLogIndex: prevLogIndex,
					PrevLogTerm:  prevLogTerm,
					Entries:      entries,
					LeaderCommit: rf.commitIndex,
				}
				rf.mu.Unlock()
				
				reply := &AppendEntriesReply{}
				if rf.sendAppendEntries(server, args, reply) {
					rf.mu.Lock()
					defer rf.mu.Unlock()
					
					if rf.state != "Leader" || rf.currentTerm != args.Term {
						return
					}
					
					if reply.Term > rf.currentTerm {
						rf.currentTerm = reply.Term
						rf.votedFor = -1
						rf.state = "Follower"
						rf.persist()
						rf.resetElectionTimer()
						return
					}
					
					if reply.Success {
						// Update matchIndex and nextIndex
						newMatchIndex := args.PrevLogIndex + len(args.Entries)
						if newMatchIndex > rf.matchIndex[server] {
							rf.matchIndex[server] = newMatchIndex
							rf.nextIndex[server] = newMatchIndex + 1
							
							// Check if we can commit new entries
							rf.updateCommitIndex()
						}
					} else {
						// Decrement nextIndex and retry later
						// Optimization: fast backup
						if reply.ConflictTerm != -1 {
							// Find the last entry in leader's log with ConflictTerm
							found := false
							for i := len(rf.log) - 1; i > 0; i-- {
								if rf.log[i].Term == reply.ConflictTerm {
									rf.nextIndex[server] = i + 1
									found = true
									break
								}
							}
							if !found {
								rf.nextIndex[server] = reply.ConflictIndex
							}
						} else {
							rf.nextIndex[server] = reply.ConflictIndex
						}
						
						// Ensure nextIndex is valid (at least 1)
						if rf.nextIndex[server] < 1 {
							rf.nextIndex[server] = 1
						}
					}
				}
			}(i)
		}
	}
}

//
// Check if we can update commitIndex
//
func (rf *Raft) updateCommitIndex() {
	for N := len(rf.log) - 1; N > rf.commitIndex; N-- {
		if rf.log[N].Term == rf.currentTerm {
			count := 1 // Count self
			for i := range rf.peers {
				if i != rf.me && rf.matchIndex[i] >= N {
					count++
				}
			}
			if count > len(rf.peers)/2 {
				rf.commitIndex = N
				go rf.applyCommittedEntries()
				break
			}
		}
	}
}

//
// Main ticker goroutine
//
func (rf *Raft) ticker() {
	for {
		rf.mu.Lock()
		state := rf.state
		rf.mu.Unlock()
		
		if state == "Leader" {
			// Leader sends heartbeats
			select {
			case <-rf.heartbeatTimer.C:
				go rf.broadcastAppendEntries()
				rf.heartbeatTimer.Reset(50 * time.Millisecond)
			default:
			}
		} else {
			// Follower or candidate checks for election timeout
			select {
			case <-rf.electionTimer.C:
				rf.startElection()
			default:
			}
		}
		
		time.Sleep(10 * time.Millisecond)
	}
}

//
// the tester calls Kill() when a Raft instance won't
// be needed again. you are not required to do anything
// in Kill(), but it might be convenient to (for example)
// turn off debug output from this instance.
//
func (rf *Raft) Kill() {
	// Your code here, if desired.
	rf.mu.Lock()
	defer rf.mu.Unlock()
	
	if rf.electionTimer != nil {
		rf.electionTimer.Stop()
	}
	if rf.heartbeatTimer != nil {
		rf.heartbeatTimer.Stop()
	}
}

//
// the service or tester wants to create a Raft server. the ports
// of all the Raft servers (including this one) are in peers[]. this
// server's port is peers[me]. all the servers' peers[] arrays
// have the same order. persister is a place for this server to
// save its persistent state, and also initially holds the most
// recent saved state, if any. applyCh is a channel on which the
// tester or service expects Raft to send ApplyMsg messages.
// Make() must return quickly, so it should start goroutines
// for any long-running work.
//
func Make(peers []*labrpc.ClientEnd, me int,
	persister *Persister, applyCh chan ApplyMsg) *Raft {
	rf := &Raft{}
	rf.peers = peers
	rf.persister = persister
	rf.me = me
	rf.applyCh = applyCh

	// Initialize Raft state
	rf.currentTerm = 0
	rf.votedFor = -1
	rf.log = make([]LogEntry, 1) // log index starts at 1, so add a dummy entry at index 0
	rf.log[0] = LogEntry{Term: 0}

	rf.commitIndex = 0
	rf.lastApplied = 0

	rf.nextIndex = make([]int, len(peers))
	rf.matchIndex = make([]int, len(peers))

	rf.state = "Follower"

	// initialize from state persisted before a crash
	rf.readPersist(persister.ReadRaftState())

	// Start election timer
	rf.resetElectionTimer()

	// Start background goroutine for election timeout and heartbeat
	go rf.ticker()

	return rf
}
