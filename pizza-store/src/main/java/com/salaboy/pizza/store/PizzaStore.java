package com.salaboy.pizza.store;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import com.salaboy.pizza.store.model.*;
import com.salaboy.pizza.store.workflow.PizzaOrderWorkflow;
import io.dapr.spring.workflows.config.EnableDaprWorkflows;
import io.dapr.workflows.client.DaprWorkflowClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.State;
import io.dapr.spring.boot.autoconfigure.client.DaprClientProperties;
import io.dapr.spring.boot.autoconfigure.client.DaprConnectionDetails;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@EnableDaprWorkflows
public class PizzaStore {

  @Autowired
  private DaprClient daprClient;

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private DaprConnectionDetails daprConnectionDetails;

  @Value("${STATE_STORE_NAME:kvstore}")
  private String STATE_STORE_NAME;

  @Value("${PUBLIC_IP:localhost:8080}")
  private String publicIp;

  public static WorkflowPayload payload;

  @GetMapping("/server-info")
  public Info getInfo(){
    return new Info(publicIp);
  }

  public record Info(String publicIp){}

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
    emitWSEvent(event.getData());
    System.out.println("Received CloudEvent via Subscription: " + event.toString());
    Event pizzaEvent = event.getData();
    if(pizzaEvent.type().equals(EventType.ORDER_READY)){
      // Emit Event
      Event wsevent = new Event(EventType.ORDER_OUT_FOR_DELIVERY, pizzaEvent.order(), "store", "Delivery in progress.");
      emitWSEvent(wsevent);
      daprWorkflowClient.raiseEvent(pizzaEvent.order().workflowId(), "KitchenDone", pizzaEvent.order());
    }
    if(pizzaEvent.type().equals(EventType.ORDER_COMPLETED)){
      daprWorkflowClient.raiseEvent(pizzaEvent.order().workflowId(), "PizzaDelivered", pizzaEvent.order());
    }
  }

  private void emitWSEvent(Event event) {
    System.out.println("Emitting Event via WS: " + event.toString());
    simpMessagingTemplate.convertAndSend("/topic/events",
        event);
  }


  @PostMapping("/order")
  public ResponseEntity<OrderPayload> placeOrder(@RequestBody(required = true) OrderPayload order, Map<String, String> headers) throws Exception {
    new Thread(new Runnable() {
      @Override
      public void run() {
        // Emit Event
        Event event = new Event(EventType.ORDER_PLACED, order, "store", "We received the payment your order is confirmed.");
        emitWSEvent(event);

        startPizzaWorkflow(order);

      }
    }).start();

    return ResponseEntity.ok(order);

  }

  private void startPizzaWorkflow(OrderPayload order) {
    payload = new WorkflowPayload(order);
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(PizzaOrderWorkflow.class, payload);
    System.out.printf("scheduled new workflow instance of OrderProcessingWorkflow with instance ID: %s%n",
            instanceId);
    try {
      daprWorkflowClient.waitForInstanceStart(instanceId, Duration.ofSeconds(10), false);
      System.out.printf("workflow instance %s started%n", instanceId);
    } catch (TimeoutException e) {
      System.out.printf("workflow instance %s did not start within 10 seconds%n", instanceId);
    }
  }

  @GetMapping("/order")
  public ResponseEntity<Orders> getOrders() {

    Orders orders = loadOrders();

    return ResponseEntity.ok(orders);
  }

  private void store(OrderPayload order) {
    try {
      Orders orders = new Orders(new ArrayList<OrderPayload>());
      State<Orders> ordersState = daprClient.getState(STATE_STORE_NAME, KEY, null, Orders.class).block();
      if (ordersState.getValue() != null && ordersState.getValue().orders().isEmpty()) {
        orders.orders().addAll(ordersState.getValue().orders());
      }
      orders.orders().add(order);
      // Save state
      daprClient.saveState(STATE_STORE_NAME, KEY, orders).block();

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
    System.out.println("Calling Kitchen service at: " + daprConnectionDetails.getHttpEndpoint() + "/prepare");
    ResponseEntity<String> put = restTemplate
            .exchange(daprConnectionDetails.getHttpEndpoint() + "/prepare", HttpMethod.PUT, request, String.class);
    System.out.println("I called the Kitchen Service and the status code is: " + put.getStatusCode());

  }

  private void callDeliveryService(Order order) {
    restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("dapr-app-id", "delivery-service");
    HttpEntity<Order> request = new HttpEntity<Order>(order, headers);
    System.out.println("Calling Delivery service at: " + daprConnectionDetails.getHttpEndpoint() + "/deliver");
    ResponseEntity<String> put = restTemplate
            .exchange(daprConnectionDetails.getHttpEndpoint() + "/deliver", HttpMethod.PUT, request, String.class);
    System.out.println("I called the Delivery Service and the status code is: " + put.getStatusCode());
  }

  protected Orders loadOrders() {
    try {
      State<Orders> ordersState = daprClient.getState(STATE_STORE_NAME, KEY, null, Orders.class).block();
      return ordersState.getValue();

    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;

  }

}
