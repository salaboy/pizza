package main

import (
	"context"
	"fmt"
	"math/rand"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/dapr/go-sdk/client"
	"github.com/gin-gonic/gin"
)

const (
	msInSecond = 1000
)

var (
	PubSubName  = os.Getenv("PUB_SUB_NAME")
	PubSubTopic = os.Getenv("PUB_SUB_TOPIC")
)

func main() {
	ctx, cancel := context.WithCancel(context.Background())

	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)

	if PubSubName == "" {
		PubSubName = "pubsub"
	}

	if PubSubTopic == "" {
		PubSubTopic = "pizza-orders"
	}

	kitchen := NewPizzaKitchen(NewDaprEventEmitter())
	go kitchen.Serve()

	// Block until a signal is received or the context is canceled
	select {
	case sig := <-sigCh:
		fmt.Printf("Received signal: %v\n", sig)
		cancel() // Cancel the context on receiving a signal
	case <-ctx.Done():
		// Context canceled, exit the goroutine
		fmt.Println("Signal handling goroutine exiting.")
		return
	}
}

type EventEmitter interface {
	Emit(ctx context.Context, event Event)
	Close()
}

func NewDaprEventEmitter() EventEmitter {
	c, err := client.NewClient()
	if err != nil {
		fmt.Println("Error creating Dapr client:", err)
		return nil
	}

	return &DaprEventEmitter{client: c}
}

type DaprEventEmitter struct {
	client client.Client
}

func (d *DaprEventEmitter) Emit(ctx context.Context, event Event) {
	fmt.Printf("> Emitting Kitchen Event: %v\n", event)

	if err := d.client.PublishEvent(ctx, PubSubName, PubSubTopic, &event, client.PublishEventWithContentType("application/json")); err != nil {
		fmt.Println("Error publishing event:", err)
	}
}

func (d *DaprEventEmitter) Close() {
	d.client.Close()
}

type PizzaKitchen struct {
	emitter EventEmitter
}

func NewPizzaKitchen(emitter EventEmitter) *PizzaKitchen {
	return &PizzaKitchen{emitter: emitter}
}

func (pk *PizzaKitchen) Serve() {
	r := gin.Default()

	r.PUT("/prepare", pk.prepareOrderHandler())

	if err := r.Run(); err != nil {
		fmt.Println("Error starting server:", err)
	}
}

func (pk *PizzaKitchen) prepareOrderHandler() func(*gin.Context) {
	return func(c *gin.Context) {
		var order Order
		if err := c.ShouldBindJSON(&order); err != nil {
			c.JSON(400, gin.H{"error": err.Error()})
			return
		}

		go func() {
			// Emit Event
			time.Sleep(5 * time.Second)
			event := Event{Type: OrderInPreparation, Order: order, Service: "kitchen", Message: "The order is now in the kitchen."}
			pk.emitter.Emit(c.Request.Context(), event)

			for _, orderItem := range order.Items {
				pizzaPrepTime := rand.Intn(15 * msInSecond)
				fmt.Printf("Preparing this %s pizza will take: %d\n", orderItem.Type, pizzaPrepTime)
				time.Sleep(time.Duration(pizzaPrepTime) * time.Millisecond)
			}

			event = Event{Type: OrderReady, Order: order, Service: "kitchen", Message: "Your pizza is ready and waiting to be delivered."}
			pk.emitter.Emit(c.Request.Context(), event)
		}()

		c.JSON(200, gin.H{})
	}
}

type Event struct {
	Type    EventType `json:"type"`
	Order   Order     `json:"order"`
	Service string    `json:"service"`
	Message string    `json:"message"`
}

type Order struct {
	ID        string      `json:"id"`
	Items     []OrderItem `json:"items"`
	OrderDate time.Time   `json:"orderDate"`
}

type OrderItem struct {
	Type   PizzaType `json:"type"`
	Amount int       `json:"amount"`
}

type PizzaType string

const (
	Pepperoni  PizzaType = "pepperoni"
	Margherita PizzaType = "margherita"
	Hawaiian   PizzaType = "hawaiian"
	Vegetarian PizzaType = "vegetarian"
)

type EventType string

const (
	OrderPlaced         EventType = "order-placed"
	ItemsInStock        EventType = "items-in-stock"
	ItemsNotInStock     EventType = "items-not-in-stock"
	OrderInPreparation  EventType = "order-in-preparation"
	OrderReady          EventType = "order-ready"
	OrderOutForDelivery EventType = "order-out-for-delivery"
	OrderOnItsWay       EventType = "order-on-its-way"
	OrderCompleted      EventType = "order-completed"
)
