package com.salaboy.pizza.store;


import com.salaboy.pizza.store.model.*;
import com.salaboy.pizza.store.workflow.PizzaOrderAgenticWorkflow;
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


import io.dapr.client.DaprClient;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.State;
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

  @Value("${PUB_SUB_NAME:pubsub}")
  private String PUB_SUB_NAME;

  @Value("${PUB_SUB_TOPIC_PING:topic-ping}")
  private String PUB_SUB_TOPIC_PING;

  @Value("${PUBLIC_IP:localhost:8080}")
  private String publicIp;

  @GetMapping("/server-info")
  public Info getInfo() {
    return new Info(publicIp);
  }

  private RestTemplate restTemplate;

  public record Info(String publicIp) {
  }

  private String KEY = "orders";

  private final SimpMessagingTemplate simpMessagingTemplate;

  public static void main(String[] args) {
    SpringApplication.run(PizzaStore.class, args);
  }

  public PizzaStore(SimpMessagingTemplate simpMessagingTemplate, RestTemplate restTemplate) {
    this.simpMessagingTemplate = simpMessagingTemplate;
    this.restTemplate = restTemplate;
  }

  @PostMapping(path = "/events", consumes = "application/cloudevents+json")
  public void receiveEvents(@RequestBody CloudEvent<Event> event) {
    if(event.getData().type().equals(EventType.PING)) {
      return;
    }
    emitWSEvent(event.getData());
    System.out.println("Received CloudEvent via Subscription: " + event.getData());
    Event pizzaEvent = event.getData();

    try {
      if (pizzaEvent.type().equals(EventType.ORDER_READY)) {
        daprWorkflowClient.raiseEvent(pizzaEvent.order().id(), "KitchenDone", pizzaEvent.order());
      } else if (pizzaEvent.type().equals(EventType.ORDER_COMPLETED)) {
        daprWorkflowClient.raiseEvent(pizzaEvent.order().id(), "PizzaDelivered", pizzaEvent.order());
      }
    } catch (Exception e) {
      System.err.println("Exception raised while processing event: " + e.getMessage());
    }
  }

  private void emitWSEvent(Event event) {
    System.out.println("Emitting Event via WS: " + event.toString());
    simpMessagingTemplate.convertAndSend("/topic/events", event);
  }


  @PostMapping("/order")
  public ResponseEntity<OrderPayload> placeOrder(@RequestBody(required = true) OrderPayload order) throws Exception {

    startPizzaWorkflow(order);

    return ResponseEntity.ok(order);
  }

  private String startPizzaWorkflow(OrderPayload order) {
    if (order.prompt() == null || order.prompt().isEmpty()) {
      System.out.println("Scheduled new PizzaOrderWorkflow instance with ID: " + order.id());
      daprWorkflowClient.scheduleNewWorkflow(PizzaOrderWorkflow.class, order, order.id());
    } else {
      System.out.println("Scheduled new PizzaOrderAgenticWorkflow instance with ID: " + order.id());
      daprWorkflowClient.scheduleNewWorkflow(PizzaOrderAgenticWorkflow.class, order, order.id());
    }
    return order.id();
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


  private boolean checkKVstoreStatus() {
    try {
      daprClient.saveState(STATE_STORE_NAME, "ping", "done").block();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }


  private boolean checkPubSubStatus() {
    try {
      daprClient.publishEvent(PUB_SUB_NAME, PUB_SUB_TOPIC_PING, new Event(EventType.PING, null, "store", "ping")).block();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }


  @GetMapping("/status")
  public ServicesStatus kitchenPing() {

    boolean kvStoreStatus = checkKVstoreStatus();
    boolean pubsubStatus = checkPubSubStatus();
    boolean kitchenStatus = checkKitchenStatus();
    boolean deliveryStatus = checkDeliveryStatus();

    return new ServicesStatus(kvStoreStatus, pubsubStatus, kitchenStatus, deliveryStatus);

  }

  private boolean checkKitchenStatus() {
    String daprHttp = daprConnectionDetails.getHttpEndpoint();
    String daprAPIToken = daprConnectionDetails.getApiToken();

    HttpHeaders headers = new HttpHeaders();

    headers.add("Content-Type", "application/json");
    headers.add("dapr-app-id", "kitchen-service");
    headers.add("dapr-api-token", daprAPIToken);
    HttpEntity<Void> request = new HttpEntity<>(headers);

    // System.out.println("Sending request via Dapr to URL: " + daprHttp + "/actuator/health");

    try {
      ResponseEntity<String> response = restTemplate.exchange(
              daprHttp + "/actuator/health", HttpMethod.GET, request, String.class);
      return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
      return false;
    }
  }


  private boolean checkDeliveryStatus() {
    String daprHttp = daprConnectionDetails.getHttpEndpoint();
    String daprAPIToken = daprConnectionDetails.getApiToken();

    HttpHeaders headers = new HttpHeaders();

    headers.add("Content-Type", "application/json");
    headers.add("dapr-app-id", "delivery-service");
    headers.add("dapr-api-token", daprAPIToken);
    HttpEntity<Void> request = new HttpEntity<>(headers);

    // System.out.println("Sending request via Dapr to URL: " + daprHttp + "/actuator/health");

    try {
      ResponseEntity<String> response = restTemplate.exchange(
              daprHttp + "/actuator/health", HttpMethod.GET, request, String.class);
      return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
      return false;
    }
  }


}
