package main

import (
	"context"
	"encoding/json"
	"log"
	"net"
	"os"
	"path/filepath"
	"strconv"
	"sync"

	"github.com/gotd/td/session"
	"github.com/gotd/td/telegram"
	"github.com/gotd/td/telegram/auth"
	"github.com/gotd/td/telegram/updates"
	"github.com/gotd/td/tg"

	"im-backend/internal/bridge"
	imtg "im-backend/internal/tg"
)

// authStatus tracks whether Telegram auth has completed.
type authStatus struct {
	mu            sync.RWMutex
	authenticated bool
	name          string
}

func (s *authStatus) setOK(name string) {
	s.mu.Lock()
	s.authenticated = true
	s.name = name
	s.mu.Unlock()
}

func (s *authStatus) isOK() (bool, string) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.authenticated, s.name
}

// apiSession holds live API resources, available only after a successful auth.
type apiSession struct {
	mu        sync.RWMutex
	api       *tg.Client
	ctx       context.Context
	peerCache map[int64]tg.InputPeerClass
}

func (s *apiSession) set(ctx context.Context, api *tg.Client, peers map[int64]tg.InputPeerClass) {
	s.mu.Lock()
	s.ctx = ctx
	s.api = api
	for k, v := range peers {
		s.peerCache[k] = v
	}
	s.mu.Unlock()
}

func (s *apiSession) get() (context.Context, *tg.Client) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.ctx, s.api
}

func (s *apiSession) peer(chatID int64) (tg.InputPeerClass, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	p, ok := s.peerCache[chatID]
	return p, ok
}

func (s *apiSession) updatePeers(peers map[int64]tg.InputPeerClass) {
	s.mu.Lock()
	for k, v := range peers {
		s.peerCache[k] = v
	}
	s.mu.Unlock()
}

func main() {
	// ── Credentials ──────────────────────────────────────────────────────────
	appIDStr := mustEnv("TG_APP_ID")
	appHash := mustEnv("TG_APP_HASH")
	appID, err := strconv.Atoi(appIDStr)
	if err != nil {
		log.Fatalf("TG_APP_ID must be numeric: %v", err)
	}

	// ── Print local IPs so the user knows what to type in the app ───────────
	printLocalAddresses()

	// ── Shared state ─────────────────────────────────────────────────────────
	status := &authStatus{}
	sess := &apiSession{peerCache: make(map[int64]tg.InputPeerClass)}

	// ── WebSocket bridge ─────────────────────────────────────────────────────
	hub := bridge.NewHub()
	wsAuth := imtg.NewWsAuth(hub)

	hub.OnConnect = func(conn *bridge.Conn) {
		if ok, name := status.isOK(); ok {
			bridge.Send(conn, map[string]any{"type": "auth_ok", "name": name})
		} else {
			bridge.Send(conn, map[string]any{"type": "auth_phone_needed"})
		}
	}

	// Single command router — handles both auth and post-auth commands.
	hub.OnCommand = func(cmd bridge.Command) {
		switch cmd.Type {

		// ── Auth commands ────────────────────────────────────────────────────
		case "auth_phone", "auth_code", "auth_password":
			wsAuth.Dispatch(cmd)

		// ── Send a message ───────────────────────────────────────────────────
		case "send_message":
			sCtx, sApi := sess.get()
			if sApi == nil {
				return
			}
			peer, ok := sess.peer(cmd.ChatID)
			if !ok {
				log.Printf("send_message: no peer for chatId=%d", cmd.ChatID)
				return
			}
			chatID, text := cmd.ChatID, cmd.Text
			go func() {
				if err := imtg.SendMessage(sCtx, sApi, peer, text); err != nil {
					log.Printf("send_message error: %v", err)
					hub.Broadcast(map[string]any{
						"type":   "send_error",
						"chatId": chatID,
						"message": err.Error(),
					})
				}
			}()

		// ── Fetch message history ────────────────────────────────────────────
		case "get_messages":
			sCtx, sApi := sess.get()
			if sApi == nil {
				return
			}
			peer, ok := sess.peer(cmd.ChatID)
			if !ok {
				log.Printf("get_messages: no peer for chatId=%d", cmd.ChatID)
				return
			}
			chatID := cmd.ChatID
			go func() {
				msgs, err := imtg.GetMessages(sCtx, sApi, peer, chatID)
				if err != nil {
					log.Printf("get_messages error: %v", err)
					return
				}
				hub.Broadcast(map[string]any{
					"type":   "messages",
					"chatId": chatID,
					"data":   msgs,
				})
			}()

		// ── Refresh chat list ────────────────────────────────────────────────
		case "get_chats":
			sCtx, sApi := sess.get()
			if sApi == nil {
				return
			}
			go func() {
				chats, peers, err := imtg.FetchChats(sCtx, sApi)
				if err != nil {
					log.Printf("get_chats error: %v", err)
					return
				}
				sess.updatePeers(peers)
				hub.Broadcast(map[string]any{"type": "chats", "data": chats})
			}()
		}
	}

	go hub.Start(":8080")

	// ── Session storage ───────────────────────────────────────────────────────
	sessionPath := os.Getenv("TG_SESSION_DIR")
	if sessionPath == "" {
		sessionPath = "session"
	}
	if err := os.MkdirAll(sessionPath, 0700); err != nil {
		log.Fatalf("create session dir: %v", err)
	}
	storage := &session.FileStorage{Path: filepath.Join(sessionPath, "session.json")}

	// ── Runtime maps (populated lazily from updates) ──────────────────────────
	var (
		userMapMu    sync.RWMutex
		userMap      = make(map[int64]*tg.User)
		chatTitleMap = make(map[int64]string)
		selfID       int64
	)

	// ── Update dispatcher ─────────────────────────────────────────────────────
	dispatcher := tg.NewUpdateDispatcher()
	dispatcher.OnNewMessage(func(ctx context.Context, e tg.Entities, u *tg.UpdateNewMessage) error {
		userMapMu.Lock()
		for _, usr := range e.Users {
			userMap[usr.ID] = usr
		}
		for id, ch := range e.Channels {
			chatTitleMap[id] = ch.Title
		}
		for id, ch := range e.Chats {
			chatTitleMap[id] = ch.Title
		}
		snapshot := make(map[int64]*tg.User, len(userMap))
		for k, v := range userMap {
			snapshot[k] = v
		}
		titleSnapshot := make(map[int64]string, len(chatTitleMap))
		for k, v := range chatTitleMap {
			titleSnapshot[k] = v
		}
		userMapMu.Unlock()

		msg, ok := imtg.MessageFromUpdate(u, snapshot, titleSnapshot, selfID)
		if !ok {
			return nil
		}

		data, _ := json.Marshal(msg)
		log.Printf("MSG %s", data)
		hub.Broadcast(map[string]any{"type": "message", "data": msg})
		return nil
	})

	gaps := updates.New(updates.Config{Handler: dispatcher})

	// ── Telegram client ───────────────────────────────────────────────────────
	client := telegram.NewClient(appID, appHash, telegram.Options{
		SessionStorage: storage,
		UpdateHandler:  gaps,
	})

	ctx := context.Background()

	if err := client.Run(ctx, func(ctx context.Context) error {
		flow := auth.NewFlow(wsAuth, auth.SendCodeOptions{})
		if err := client.Auth().IfNecessary(ctx, flow); err != nil {
			hub.Broadcast(map[string]any{"type": "auth_error", "message": err.Error()})
			return err
		}

		self, err := client.Self(ctx)
		if err != nil {
			return err
		}
		selfID = self.ID
		name := self.FirstName
		if self.LastName != "" {
			name += " " + self.LastName
		}
		log.Printf("Logged in as: %s (id=%d)", name, self.ID)

		status.setOK(name)
		hub.Broadcast(map[string]any{"type": "auth_ok", "name": name})

		api := tg.NewClient(client)

		// Fetch initial chat list and populate peer cache.
		chats, peers, err := imtg.FetchChats(ctx, api)
		if err != nil {
			log.Printf("fetch chats warning: %v", err)
		} else {
			data, _ := json.Marshal(chats)
			log.Printf("CHATS %s", data)
			hub.Broadcast(map[string]any{"type": "chats", "data": chats})
		}

		// Activate post-auth API session.
		sess.set(ctx, api, peers)

		return gaps.Run(ctx, api, selfID, updates.AuthOptions{
			OnStart: func(ctx context.Context) {
				log.Println("Listening for updates...")
			},
		})
	}); err != nil {
		log.Fatalf("client run: %v", err)
	}
}

// printLocalAddresses logs all non-loopback IPv4 addresses in WebSocket URL form.
func printLocalAddresses() {
	ifaces, err := net.Interfaces()
	if err != nil {
		log.Printf("cannot enumerate network interfaces: %v", err)
		return
	}
	log.Println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
	log.Println("  Use one of these addresses in the app:")
	found := false
	for _, iface := range ifaces {
		if iface.Flags&net.FlagUp == 0 || iface.Flags&net.FlagLoopback != 0 {
			continue
		}
		addrs, _ := iface.Addrs()
		for _, addr := range addrs {
			var ip net.IP
			switch v := addr.(type) {
			case *net.IPNet:
				ip = v.IP
			case *net.IPAddr:
				ip = v.IP
			}
			if ip == nil || ip.To4() == nil || ip.IsLoopback() {
				continue
			}
			log.Printf("    ws://%s:8080/ws  (%s)", ip, iface.Name)
			found = true
		}
	}
	if !found {
		log.Println("    (no local IPv4 found — check your network)")
	}
	log.Println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
}

func mustEnv(key string) string {
	v := os.Getenv(key)
	if v == "" {
		log.Fatalf("env variable %s is required", key)
	}
	return v
}
