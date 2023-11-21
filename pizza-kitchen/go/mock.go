package main

import "context"

func NewMockEventEmitter(emitted *[]Event) *MockEventEmitter {
	return &MockEventEmitter{
		emitted: emitted,
	}
}

type MockEventEmitter struct {
	emitted *[]Event
}

func (m *MockEventEmitter) Emit(ctx context.Context, event Event) {
	*m.emitted = append(*m.emitted, event)
}

func (m *MockEventEmitter) Close() {
}
