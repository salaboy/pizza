package com.salaboy.pizza.store;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import com.salaboy.pizza.store.model.*;
import com.salaboy.pizza.store.workflow.PizzaOrderAgenticWorkflow;
import com.salaboy.pizza.store.workflow.PizzaOrderWorkflow;
import io.dapr.spring.workflows.config.EnableDaprWorkflows;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.grpc.netty.shaded.io.netty.handler.codec.compression.ZstdOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
    System.out.println("Received CloudEvent via Subscription: " + event.getData());
    Event pizzaEvent = event.getData();

    if (pizzaEvent.type().equals(EventType.ORDER_READY)){
      // Emit Event
      Event wsevent = new Event(EventType.ORDER_OUT_FOR_DELIVERY, pizzaEvent.order(), "store", "Delivery in progress.");
      emitWSEvent(wsevent);
      daprWorkflowClient.raiseEvent(pizzaEvent.order().workflowId(), "KitchenDone", pizzaEvent.order());
    }
    if (pizzaEvent.type().equals(EventType.ORDER_COMPLETED)){
      daprWorkflowClient.raiseEvent(pizzaEvent.order().workflowId(), "PizzaDelivered", pizzaEvent.order());
    }
  }

  private void emitWSEvent(Event event) {
    System.out.println("Emitting Event via WS: " + event.toString());
    simpMessagingTemplate.convertAndSend("/topic/events", event);
  }


  @PostMapping("/order")
  public ResponseEntity<OrderPayload> placeOrder(@RequestBody(required = true) OrderPayload order) throws Exception {
    String instanceId = startPizzaWorkflow(order);
    OrderPayload processingOrder = new OrderPayload(order.id(), order.customer(), order.items(), order.orderDate(), order.status(), instanceId);
    // Emit Event
    Event event = new Event(EventType.ORDER_PLACED, processingOrder, "store", "We received the payment your order is confirmed.");
    emitWSEvent(event);
    System.out.println("Returning processing order: " + processingOrder);
    return ResponseEntity.ok(processingOrder);
  }

  @PostMapping("/prompt")
  public ResponseEntity<String> placeOrder(@RequestBody(required = true) String prompt) throws Exception {
    String instanceId = startPizzaWorkflowPrompt(prompt);
    return ResponseEntity.ok(instanceId);
  }


  private String startPizzaWorkflowPrompt(String prompt) {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(PizzaOrderAgenticWorkflow.class, prompt);
    System.out.printf("scheduled new workflow instance of OrderProcessingWorkflow with instance ID: %s%n",
                       instanceId);
    return instanceId;
  }


  private String startPizzaWorkflow(OrderPayload order) {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(PizzaOrderWorkflow.class, order);
    System.out.println("Scheduled new PizzaOrderWorkflow instance with ID: " + instanceId);
    return instanceId;
  }

  @GetMapping("/order")
  public ResponseEntity<Orders> getOrders() {
    Orders orders = loadOrders();
    return ResponseEntity.ok(orders);
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
