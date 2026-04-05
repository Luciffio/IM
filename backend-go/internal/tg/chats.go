package tg

import (
	"context"
	"fmt"
	"time"

	"github.com/gotd/td/tg"
)

// FetchChats returns the 20 most-recent dialogs and an InputPeer cache
// keyed by chat ID so callers can send messages or fetch history later.
func FetchChats(ctx context.Context, api *tg.Client) ([]ChatPreview, map[int64]tg.InputPeerClass, error) {
	dialogs, err := api.MessagesGetDialogs(ctx, &tg.MessagesGetDialogsRequest{
		Limit:      20,
		OffsetPeer: &tg.InputPeerEmpty{},
	})
	if err != nil {
		return nil, nil, fmt.Errorf("get dialogs: %w", err)
	}
	previews, peers := parseDialogs(dialogs)
	return previews, peers, nil
}

// SendMessage sends a text message to the given peer.
func SendMessage(ctx context.Context, api *tg.Client, peer tg.InputPeerClass, text string) error {
	_, err := api.MessagesSendMessage(ctx, &tg.MessagesSendMessageRequest{
		Peer:     peer,
		Message:  text,
		RandomID: time.Now().UnixNano(),
	})
	if err != nil {
		return fmt.Errorf("send message: %w", err)
	}
	return nil
}

// GetMessages fetches the last 30 messages from a chat.
func GetMessages(ctx context.Context, api *tg.Client, peer tg.InputPeerClass, chatID int64) ([]Message, error) {
	raw, err := api.MessagesGetHistory(ctx, &tg.MessagesGetHistoryRequest{
		Peer:  peer,
		Limit: 30,
	})
	if err != nil {
		return nil, fmt.Errorf("get history: %w", err)
	}
	return parseHistory(raw, chatID), nil
}

// ── Parsing helpers ───────────────────────────────────────────────────────────

func parseDialogs(raw tg.MessagesDialogsClass) ([]ChatPreview, map[int64]tg.InputPeerClass) {
	var (
		msgs    []tg.MessageClass
		users   []tg.UserClass
		chats   []tg.ChatClass
		dialogs []tg.Dialog
	)

	switch d := raw.(type) {
	case *tg.MessagesDialogs:
		msgs, users, chats = d.Messages, d.Users, d.Chats
		for i := range d.Dialogs {
			if dlg, ok := d.Dialogs[i].(*tg.Dialog); ok {
				dialogs = append(dialogs, *dlg)
			}
		}
	case *tg.MessagesDialogsSlice:
		msgs, users, chats = d.Messages, d.Users, d.Chats
		for i := range d.Dialogs {
			if dlg, ok := d.Dialogs[i].(*tg.Dialog); ok {
				dialogs = append(dialogs, *dlg)
			}
		}
	}

	// Build lookup maps
	userMap := make(map[int64]*tg.User, len(users))
	for _, u := range users {
		if usr, ok := u.(*tg.User); ok {
			userMap[usr.ID] = usr
		}
	}
	chatMap := make(map[int64]string, len(chats))
	channelMap := make(map[int64]*tg.Channel, len(chats))
	for _, c := range chats {
		switch ch := c.(type) {
		case *tg.Chat:
			chatMap[ch.ID] = ch.Title
		case *tg.Channel:
			chatMap[ch.ID] = ch.Title
			channelMap[ch.ID] = ch
		}
	}
	msgMap := make(map[int]tg.MessageClass, len(msgs))
	for _, m := range msgs {
		if msg, ok := m.(*tg.Message); ok {
			msgMap[msg.ID] = msg
		}
	}

	previews := make([]ChatPreview, 0, len(dialogs))
	peers := make(map[int64]tg.InputPeerClass, len(dialogs))

	for _, dlg := range dialogs {
		topMsg, ok := msgMap[dlg.TopMessage]
		if !ok {
			continue
		}
		msg, ok := topMsg.(*tg.Message)
		if !ok {
			continue
		}

		ts := time.Unix(int64(msg.Date), 0)
		preview := ChatPreview{
			LastMessage: msg.Message,
			Timestamp:   ts,
		}

		switch peer := dlg.Peer.(type) {
		case *tg.PeerUser:
			preview.ID = peer.UserID
			if u, ok := userMap[peer.UserID]; ok {
				preview.Title = userName(u)
				peers[peer.UserID] = &tg.InputPeerUser{
					UserID:     peer.UserID,
					AccessHash: u.AccessHash,
				}
			}
		case *tg.PeerChat:
			preview.ID = peer.ChatID
			preview.Title = chatMap[peer.ChatID]
			peers[peer.ChatID] = &tg.InputPeerChat{ChatID: peer.ChatID}
		case *tg.PeerChannel:
			preview.ID = peer.ChannelID
			preview.Title = chatMap[peer.ChannelID]
			if ch, ok := channelMap[peer.ChannelID]; ok {
				peers[peer.ChannelID] = &tg.InputPeerChannel{
					ChannelID:  peer.ChannelID,
					AccessHash: ch.AccessHash,
				}
			}
		}

		if msg.Out {
			preview.IsIncoming = false
			preview.Sender = "You"
		} else {
			preview.IsIncoming = true
			switch from := msg.FromID.(type) {
			case *tg.PeerUser:
				if u, ok := userMap[from.UserID]; ok {
					preview.Sender = userName(u)
				}
			default:
				preview.Sender = preview.Title
			}
		}

		previews = append(previews, preview)
	}
	return previews, peers
}

func parseHistory(raw tg.MessagesMessagesClass, chatID int64) []Message {
	var msgs []tg.MessageClass
	var users []tg.UserClass

	switch h := raw.(type) {
	case *tg.MessagesMessages:
		msgs, users = h.Messages, h.Users
	case *tg.MessagesMessagesSlice:
		msgs, users = h.Messages, h.Users
	case *tg.MessagesChannelMessages:
		msgs, users = h.Messages, h.Users
	default:
		return nil
	}

	uMap := make(map[int64]*tg.User, len(users))
	for _, u := range users {
		if usr, ok := u.(*tg.User); ok {
			uMap[usr.ID] = usr
		}
	}

	result := make([]Message, 0, len(msgs))
	for _, m := range msgs {
		msg, ok := m.(*tg.Message)
		if !ok || msg.Message == "" {
			continue
		}
		out := Message{
			MsgID:      msg.ID,
			ChatID:     chatID,
			Text:       msg.Message,
			Timestamp:  time.Unix(int64(msg.Date), 0),
			IsIncoming: !msg.Out,
		}
		if msg.Out {
			out.Sender = "You"
		} else if from, ok := msg.FromID.(*tg.PeerUser); ok {
			if u, ok := uMap[from.UserID]; ok {
				out.Sender = userName(u)
			}
		}
		result = append(result, out)
	}

	// TG returns newest-first → reverse to chronological order
	for i, j := 0, len(result)-1; i < j; i, j = i+1, j-1 {
		result[i], result[j] = result[j], result[i]
	}
	return result
}

func userName(u *tg.User) string {
	name := u.FirstName
	if u.LastName != "" {
		name += " " + u.LastName
	}
	if name == "" && u.Username != "" {
		name = "@" + u.Username
	}
	return name
}

// MessageFromUpdate extracts a Message from an incoming update.
func MessageFromUpdate(upd tg.UpdateClass, userMap map[int64]*tg.User, chatTitleMap map[int64]string, selfID int64) (*Message, bool) {
	msg, ok := upd.(*tg.UpdateNewMessage)
	if !ok {
		return nil, false
	}
	m, ok := msg.Message.(*tg.Message)
	if !ok {
		return nil, false
	}

	out := &Message{
		MsgID:      m.ID,
		Text:       m.Message,
		Timestamp:  time.Unix(int64(m.Date), 0),
		IsIncoming: !m.Out,
	}

	switch peer := m.PeerID.(type) {
	case *tg.PeerUser:
		out.ChatID = peer.UserID
		if u, ok := userMap[peer.UserID]; ok {
			out.ChatTitle = userName(u)
		}
	case *tg.PeerChat:
		out.ChatID = peer.ChatID
		out.ChatTitle = chatTitleMap[peer.ChatID]
	case *tg.PeerChannel:
		out.ChatID = peer.ChannelID
		out.ChatTitle = chatTitleMap[peer.ChannelID]
	}

	if m.Out {
		out.Sender = "You"
	} else {
		switch from := m.FromID.(type) {
		case *tg.PeerUser:
			if u, ok := userMap[from.UserID]; ok {
				out.Sender = userName(u)
			} else {
				out.Sender = out.ChatTitle
			}
		default:
			out.Sender = out.ChatTitle
		}
	}

	return out, true
}
