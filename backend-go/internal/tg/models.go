package tg

import "time"

// Message is the wire format sent over WebSocket to the frontend.
type Message struct {
	MsgID      int       `json:"msgId"`
	ChatID     int64     `json:"chatId"`
	ChatTitle  string    `json:"chatTitle"`
	Sender     string    `json:"sender"`
	Text       string    `json:"text"`
	Timestamp  time.Time `json:"timestamp"`
	IsIncoming bool      `json:"isIncoming"`
}

// ChatPreview is used for the initial chat list dump.
type ChatPreview struct {
	ID          int64     `json:"id"`
	Title       string    `json:"title"`
	LastMessage string    `json:"lastMessage"`
	Timestamp   time.Time `json:"timestamp"`
	IsIncoming  bool      `json:"isIncoming"`
	Sender      string    `json:"sender"`
}
