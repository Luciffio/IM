package bridge

import (
	"encoding/json"
	"log"
	"net/http"
	"sync"

	"github.com/gorilla/websocket"
)

// Conn re-exports websocket.Conn so callers don't need to import gorilla directly.
type Conn = websocket.Conn

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool { return true },
}

// Command is a JSON message sent from a WebSocket client to the server.
type Command struct {
	Type     string `json:"type"`
	Phone    string `json:"phone,omitempty"`
	Code     string `json:"code,omitempty"`
	Password string `json:"password,omitempty"`
	ChatID   int64  `json:"chatId,omitempty"`
	Text     string `json:"text,omitempty"`
}

// Hub manages all active WebSocket connections.
type Hub struct {
	mu        sync.Mutex
	clients   map[*websocket.Conn]struct{}
	OnCommand func(cmd Command)      // called when a client sends a command
	OnConnect func(*websocket.Conn) // called just before the read loop starts
}

func NewHub() *Hub {
	return &Hub{clients: make(map[*websocket.Conn]struct{})}
}

// ServeHTTP upgrades the connection, calls OnConnect, then runs the read loop.
func (h *Hub) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("ws upgrade: %v", err)
		return
	}
	log.Printf("ws client connected (%s)", conn.RemoteAddr())

	// Send initial state BEFORE adding to broadcast pool (no concurrent write yet).
	if h.OnConnect != nil {
		h.OnConnect(conn)
	}

	h.mu.Lock()
	h.clients[conn] = struct{}{}
	h.mu.Unlock()

	// Read loop — receive commands from this client.
	for {
		_, raw, err := conn.ReadMessage()
		if err != nil {
			break
		}
		if h.OnCommand != nil {
			var cmd Command
			if jsonErr := json.Unmarshal(raw, &cmd); jsonErr == nil {
				h.OnCommand(cmd)
			}
		}
	}

	h.mu.Lock()
	delete(h.clients, conn)
	h.mu.Unlock()
	conn.Close()
	log.Printf("ws client disconnected (%s)", conn.RemoteAddr())
}

// Broadcast serialises v as JSON and sends it to every connected client.
func (h *Hub) Broadcast(v any) {
	data, err := json.Marshal(v)
	if err != nil {
		log.Printf("broadcast marshal: %v", err)
		return
	}
	h.mu.Lock()
	defer h.mu.Unlock()
	for conn := range h.clients {
		if err := conn.WriteMessage(websocket.TextMessage, data); err != nil {
			log.Printf("broadcast write: %v", err)
		}
	}
}

// Send serialises v as JSON and sends it to a single connection.
// Safe to call before the connection is added to the broadcast pool.
func Send(conn *websocket.Conn, v any) {
	data, err := json.Marshal(v)
	if err != nil {
		return
	}
	conn.WriteMessage(websocket.TextMessage, data) //nolint:errcheck
}

// Start listens on addr (e.g. ":8080") and serves the /ws endpoint.
func (h *Hub) Start(addr string) {
	mux := http.NewServeMux()
	mux.Handle("/ws", h)
	log.Printf("WebSocket bridge listening on ws://%s/ws", addr)
	if err := http.ListenAndServe(addr, mux); err != nil {
		log.Fatalf("ws server: %v", err)
	}
}
