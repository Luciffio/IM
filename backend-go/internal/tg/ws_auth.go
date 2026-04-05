package tg

import (
	"context"
	"fmt"

	"github.com/gotd/td/telegram/auth"
	"github.com/gotd/td/tg"

	"im-backend/internal/bridge"
)

// WsAuth implements auth.UserAuthenticator driven by WebSocket commands
// instead of stdin. Each getter blocks on the corresponding channel until
// the frontend sends the required value.
type WsAuth struct {
	hub        *bridge.Hub
	phoneCh    chan string // auth_phone command
	codeCh     chan string // auth_code command
	passwordCh chan string // auth_password command
}

func NewWsAuth(hub *bridge.Hub) *WsAuth {
	return &WsAuth{
		hub:        hub,
		phoneCh:    make(chan string, 1),
		codeCh:     make(chan string, 1),
		passwordCh: make(chan string, 1),
	}
}

// Dispatch routes an incoming WebSocket Command to the correct channel.
// Call this from hub.OnCommand.
func (a *WsAuth) Dispatch(cmd bridge.Command) {
	switch cmd.Type {
	case "auth_phone":
		select {
		case a.phoneCh <- cmd.Phone:
		default:
		}
	case "auth_code":
		select {
		case a.codeCh <- cmd.Code:
		default:
		}
	case "auth_password":
		select {
		case a.passwordCh <- cmd.Password:
		default:
		}
	}
}

// ── auth.UserAuthenticator interface ─────────────────────────────────────────

func (a *WsAuth) Phone(ctx context.Context) (string, error) {
	// OnConnect already broadcasts auth_phone_needed; just wait.
	select {
	case phone := <-a.phoneCh:
		return phone, nil
	case <-ctx.Done():
		return "", ctx.Err()
	}
}

func (a *WsAuth) Code(ctx context.Context, _ *tg.AuthSentCode) (string, error) {
	a.hub.Broadcast(map[string]any{"type": "auth_code_needed"})
	select {
	case code := <-a.codeCh:
		return code, nil
	case <-ctx.Done():
		return "", ctx.Err()
	}
}

func (a *WsAuth) Password(ctx context.Context) (string, error) {
	a.hub.Broadcast(map[string]any{"type": "auth_2fa_needed"})
	select {
	case pw := <-a.passwordCh:
		return pw, nil
	case <-ctx.Done():
		return "", ctx.Err()
	}
}

func (a *WsAuth) AcceptTermsOfService(_ context.Context, tos tg.HelpTermsOfService) error {
	// Auto-accept for headless operation.
	_ = tos
	return nil
}

func (a *WsAuth) SignUp(_ context.Context) (auth.UserInfo, error) {
	return auth.UserInfo{}, fmt.Errorf("sign-up not supported via this client")
}
