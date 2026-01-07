package chandy_lamport

import "log"

// The main participant of the distributed snapshot protocol.
// Servers exchange token messages and marker messages among each other.
// Token messages represent the transfer of tokens from one server to another.
// Marker messages represent the progress of the snapshot process. The bulk of
// the distributed protocol is implemented in `HandlePacket` and `StartSnapshot`.
type Server struct {
	Id            string
	Tokens        int
	sim           *Simulator
	outboundLinks map[string]*Link // key = link.dest
	inboundLinks  map[string]*Link // key = link.src
	// TODO: ADD MORE FIELDS HERE
	// Snapshot state for each snapshot ID (snapshotId -> *SnapshotState)
	snapshots *SyncMap
	// Track whether we've received marker for each snapshot from each inbound link
	// (snapshotId -> map[string]bool where key is src server)
	markersReceived *SyncMap
}

// A unidirectional communication channel between two servers
// Each link contains an event queue (as opposed to a packet queue)
type Link struct {
	src    string
	dest   string
	events *Queue
}

func NewServer(id string, tokens int, sim *Simulator) *Server {
	return &Server{
		id,
		tokens,
		sim,
		make(map[string]*Link),
		make(map[string]*Link),
		NewSyncMap(),
		NewSyncMap(),
	}
}

// Add a unidirectional link to the destination server
func (server *Server) AddOutboundLink(dest *Server) {
	if server == dest {
		return
	}
	l := Link{server.Id, dest.Id, NewQueue()}
	server.outboundLinks[dest.Id] = &l
	dest.inboundLinks[server.Id] = &l
}

// Send a message on all of the server's outbound links
func (server *Server) SendToNeighbors(message interface{}) {
	for _, serverId := range getSortedKeys(server.outboundLinks) {
		link := server.outboundLinks[serverId]
		server.sim.logger.RecordEvent(
			server,
			SentMessageEvent{server.Id, link.dest, message})
		link.events.Push(SendMessageEvent{
			server.Id,
			link.dest,
			message,
			server.sim.GetReceiveTime()})
	}
}

// Send a number of tokens to a neighbor attached to this server
func (server *Server) SendTokens(numTokens int, dest string) {
	if server.Tokens < numTokens {
		log.Fatalf("Server %v attempted to send %v tokens when it only has %v\n",
			server.Id, numTokens, server.Tokens)
	}
	message := TokenMessage{numTokens}
	server.sim.logger.RecordEvent(server, SentMessageEvent{server.Id, dest, message})
	// Update local state before sending the tokens
	server.Tokens -= numTokens
	link, ok := server.outboundLinks[dest]
	if !ok {
		log.Fatalf("Unknown dest ID %v from server %v\n", dest, server.Id)
	}
	link.events.Push(SendMessageEvent{
		server.Id,
		dest,
		message,
		server.sim.GetReceiveTime()})
}

// Callback for when a message is received on this server.
// When the snapshot algorithm completes on this server, this function
// should notify the simulator by calling `sim.NotifySnapshotComplete`.
func (server *Server) HandlePacket(src string, message interface{}) {
	// TODO: IMPLEMENT ME
	switch msg := message.(type) {
	case TokenMessage:
		// Regular token message
		server.Tokens += msg.numTokens
		
		// If we're in the middle of any snapshots, record this message
		server.markersReceived.Range(func(key, value interface{}) bool {
			snapshotId := key.(int)
			markers := value.(map[string]bool)
			// If we haven't received marker from src for this snapshot yet, record the message
			if !markers[src] {
				snapshotVal, _ := server.snapshots.Load(snapshotId)
				snapshot := snapshotVal.(*SnapshotState)
				snapshot.messages = append(snapshot.messages, &SnapshotMessage{src, server.Id, msg})
			}
			return true
		})
		
	case MarkerMessage:
		snapshotId := msg.snapshotId
		
		// Check if this is the first marker we've received for this snapshot
		_, exists := server.markersReceived.Load(snapshotId)
		if !exists {
			// First marker for this snapshot - start recording
			server.StartSnapshot(snapshotId)
		}
		
		// Mark that we've received a marker from src for this snapshot
		markersVal, _ := server.markersReceived.Load(snapshotId)
		markers := markersVal.(map[string]bool)
		markers[src] = true
		
		// Check if we've received markers from all inbound links
		if len(markers) == len(server.inboundLinks) {
			server.sim.NotifySnapshotComplete(server.Id, snapshotId)
		}
	}
}

// Start the chandy-lamport snapshot algorithm on this server.
// This should be called only once per server.
func (server *Server) StartSnapshot(snapshotId int) {
	// TODO: IMPLEMENT ME
	// Record local state (tokens)
	snapshot := &SnapshotState{
		id:       snapshotId,
		tokens:   make(map[string]int),
		messages: make([]*SnapshotMessage, 0),
	}
	snapshot.tokens[server.Id] = server.Tokens
	server.snapshots.Store(snapshotId, snapshot)
	
	// Initialize marker tracking for this snapshot
	markers := make(map[string]bool)
	server.markersReceived.Store(snapshotId, markers)
	
	// Send marker messages on all outbound links
	server.SendToNeighbors(MarkerMessage{snapshotId})
	
	// Check if snapshot is complete (no inbound links or all markers received)
	if len(server.inboundLinks) == 0 {
		server.sim.NotifySnapshotComplete(server.Id, snapshotId)
	}
}
