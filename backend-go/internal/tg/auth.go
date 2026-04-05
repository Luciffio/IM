package tg

import (
	"bufio"
	"context"
	"fmt"
	"os"
	"strings"

	"github.com/gotd/td/telegram/auth"
	"github.com/gotd/td/tg"
)

// ConsoleAuth implements auth.UserAuthenticator via stdin/stdout.
type ConsoleAuth struct {
	PhoneNumber string
}

func (c ConsoleAuth) Phone(_ context.Context) (string, error) {
	if c.PhoneNumber != "" {
		return c.PhoneNumber, nil
	}
	fmt.Print("Enter phone number (with country code, e.g. +79001234567): ")
	return readLine()
}

func (c ConsoleAuth) Password(_ context.Context) (string, error) {
	fmt.Print("Enter 2FA password: ")
	return readLine()
}

func (c ConsoleAuth) Code(_ context.Context, _ *tg.AuthSentCode) (string, error) {
	fmt.Print("Enter the code you received: ")
	return readLine()
}

func (c ConsoleAuth) AcceptTermsOfService(_ context.Context, tos tg.HelpTermsOfService) error {
	fmt.Printf("Terms of Service: %s\nAccepted.\n", tos.Text)
	return nil
}

func (c ConsoleAuth) SignUp(_ context.Context) (auth.UserInfo, error) {
	fmt.Print("First name: ")
	first, err := readLine()
	if err != nil {
		return auth.UserInfo{}, err
	}
	fmt.Print("Last name: ")
	last, err := readLine()
	if err != nil {
		return auth.UserInfo{}, err
	}
	return auth.UserInfo{FirstName: first, LastName: last}, nil
}

func readLine() (string, error) {
	reader := bufio.NewReader(os.Stdin)
	line, err := reader.ReadString('\n')
	if err != nil {
		return "", err
	}
	return strings.TrimSpace(line), nil
}
