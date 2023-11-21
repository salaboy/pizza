package main

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
)

func TestPrepareOrder(t *testing.T) {
	order := Order{
		ID:        "123",
		Items:     []OrderItem{{Type: Pepperoni, Amount: 2}},
		OrderDate: Now(),
	}

	orderJSON, err := json.Marshal(order)
	assert.NoError(t, err)

	req, err := http.NewRequest("POST", "/prepare-order", bytes.NewBuffer(orderJSON))
	assert.NoError(t, err)

	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = req

	emitted := []Event{}
	emitter := NewMockEventEmitter(&emitted)
	kitchen := NewPizzaKitchen(emitter)
	handler := kitchen.prepareOrderHandler()
	handler(c)
	assert.Equal(t, http.StatusOK, w.Code)

	time.Sleep(17 * time.Second)

	assert.Equal(t, 2, len(emitted))
	assert.Equal(t, OrderInPreparation, emitted[0].Type)
	assert.Equal(t, OrderReady, emitted[1].Type)
}

func Now() time.Time {
	return time.Now().UTC()
}
