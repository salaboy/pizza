package io.diagrid.dapr;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import static java.util.Collections.singletonMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.State;
import io.dapr.client.domain.Metadata;

@SpringBootApplication
@RestController
public class PizzaStore {

  @Value("${dapr-http.base-url:http://localhost:3500}")
  private String daprHttp;

  private String STATE_STORE_NAME = "statestore";
  
  private String KEY = "orders";
  private static RestTemplate restTemplate;

  private final SimpMessagingTemplate simpMessagingTemplate;

  public static void main(String[] args) {
    SpringApplication.run(PizzaStore.class, args);

  }

  public PizzaStore(SimpMessagingTemplate simpMessagingTemplate) {
    this.simpMessagingTemplate = simpMessagingTemplate;
  }

  @PostMapping(path = "/events", consumes = "application/cloudevents+json")
  public void receiveEvents(@RequestBody CloudEvent<Event> event) {
    System.out.println("Received CloudEvent via Subscription: " + event.toString());
    Event pizzaEvent = event.getData();
    if(pizzaEvent.type.equals(EventType.ORDER_READY)){
      prepareOrderForDelivery(pizzaEvent.order);
    }
    emitWSEvent(event.getData());
  }

  private void emitWSEvent(Event event) {
    System.out.println("Emitting Event via WS: " + event.toString());
    simpMessagingTemplate.convertAndSend("/topic/events",
        event);
  }

  private void prepareOrderForDelivery(Order order){
    store(new Order(order.id, order.customer, order.items, order.orderDate, Status.delivery));
     // Emit Event
    Event event = new Event(EventType.ORDER_OUT_FOR_DELIVERY, order);

    emitWSEvent(event);

  }

  @PostMapping("/order")
  public ResponseEntity<Order> placeOrder(@RequestBody(required = true) Order order) throws Exception {
    // Store Order
    store(order);

    // Emit Event
    Event event = new Event(EventType.ORDER_PLACED, order);

    emitWSEvent(event);

    // Process Order, sent to kitcken
    callKitchenService(order);

    return ResponseEntity.ok(order);

  }

  @GetMapping("/order")
  public ResponseEntity<Orders> getOrders() {

    Orders orders = loadOrders();

    return ResponseEntity.ok(orders);
  }

  public record Customer(@JsonProperty String name, @JsonProperty String email) {
  }

  public record OrderItem(@JsonProperty PizzaType type, @JsonProperty int amount) {
  }

  public enum PizzaType {
    pepperoni, margherita, hawaiian, vegetarian
  }

  public enum Status {
    created, placed, notplaced, instock, notinstock, inpreparation, delivery, completed, failed
  }

  public record Event(EventType type, Order order) {
  }

  public enum EventType {

    ORDER_PLACED("order-placed"),
    ITEMS_IN_STOCK("items-in-stock"),
    ITEMS_NOT_IN_STOCK("items-not-in-stock"),
    ORDER_IN_PREPARATION("order-in-preparation"),
    ORDER_READY("order-ready"),
    ORDER_OUT_FOR_DELIVERY("order-out-for-delivery"),
    ORDER_COMPLETED("order-completed");

    private String type;

    EventType(String type) {
      this.type = type;
    }

    @JsonValue
    public String getType() {
      return type;
    }
  }

  public record KitchenResponse(@JsonProperty String message, @JsonProperty String orderId) {
  }

  private record Orders(@JsonProperty List<Order> orders) {
  }

  public record Order(@JsonProperty String id, @JsonProperty Customer customer, @JsonProperty List<OrderItem> items,
      @JsonProperty Date orderDate, @JsonProperty Status status) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Order(String id, Customer customer, List<OrderItem> items, Date orderDate, Status status) {
      if (id == null) {
        this.id = UUID.randomUUID().toString();
      } else {
        this.id = id;
      }
      this.customer = customer;
      this.items = items;
      if (orderDate == null) {
        this.orderDate = new Date();
      } else {
        this.orderDate = orderDate;
      }
      if (status == null) {
        this.status = Status.created;
      } else {
        this.status = status;
      }
    }

    public Order(Customer customer, List<OrderItem> items, Date orderDate, Status status) {
      this(UUID.randomUUID().toString(), customer, items, orderDate, status);
    }

    public Order(Customer customer, List<OrderItem> items) {
      this(UUID.randomUUID().toString(), customer, items, new Date(), Status.created);
    }

    public Order(Order order) {
      this(order.id, order.customer, order.items, order.orderDate, order.status);
    }
  }

  private void store(Order order) {
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      Orders orders = new Orders(new ArrayList<Order>());
      State<Orders> ordersState = client.getState(STATE_STORE_NAME, KEY, null, Orders.class).block();
      if (ordersState.getValue() != null && ordersState.getValue().orders.isEmpty()) {
        orders.orders.addAll(ordersState.getValue().orders);
      }
      orders.orders.add(order);
      // Save state
      client.saveState(STATE_STORE_NAME, KEY, orders);

    } catch (Exception ex) {
      ex.printStackTrace();
    }

  }

  private void callKitchenService(Order order) {
    restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("dapr-app-id", "kitchen-service");
    HttpEntity<Order> request = new HttpEntity<Order>(order, headers);
    restTemplate.put(
        daprHttp + "/prepare", request);
  }

  private Orders loadOrders() {
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      State<Orders> ordersState = client.getState(STATE_STORE_NAME, KEY, null, Orders.class).block();
      return ordersState.getValue();

    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;

  }

}
