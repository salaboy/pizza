package io.diagrid.dapr;

import java.util.List;
import java.util.Random;
import java.util.Date;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import static java.util.Collections.singletonMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.Metadata;

@SpringBootApplication
@RestController
public class PizzaKitchen {

  private static final String MESSAGE_TTL_IN_SECONDS = "1000";
  private String PUB_SUB_NAME = "pubsub";
  private String PUB_SUB_TOPIC = "topic";
  private static final int MS_IN_SECOND = 1000;
  private static final Random RANDOM = new Random();

  public static void main(String[] args) {
    SpringApplication.run(PizzaKitchen.class, args);
  }

  @PutMapping("/prepare")
  public ResponseEntity prepareOrder(@RequestBody(required = true) Order order) throws InterruptedException {
    new Thread(new Runnable() {
      @Override
      public void run() {
           // Emit Event
            Event event = new Event(EventType.ORDER_IN_PREPARATION, order);
            emitEvent(event);
            for (OrderItem orderItem : order.items) {
              // Check inventory
              callInventoryService("inventory", new InventoryRequest(orderItem.type, orderItem.amount));
              // If there is stock prepare the pizza
              int pizzaPrepTime = RANDOM.nextInt(15 * MS_IN_SECOND);
              System.out.println("Preparing this " + orderItem.type + " pizza will take: " + pizzaPrepTime);
              try {
                Thread.sleep(pizzaPrepTime);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
            event = new Event(EventType.ORDER_READY, order);
            emitEvent(event);
      }
    }).start();
    
    return ResponseEntity.ok().build();
  }

  private void callInventoryService(String serviceName, Object payload) {
    // @TODO:
  }

  private void emitEvent(Event event) {
    System.out.println("> Emitting Kitchen Event: "+ event.toString());
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      client.publishEvent(PUB_SUB_NAME,
          PUB_SUB_TOPIC,
          event,
          singletonMap(Metadata.TTL_IN_SECONDS, MESSAGE_TTL_IN_SECONDS)).block();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
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

  public record Order(@JsonProperty String id, @JsonProperty List<OrderItem> items, @JsonProperty Date orderDate) {
  }


  public record KitchenResponse(@JsonProperty String message, @JsonProperty String orderId) {
  }

  public record OrderItem(@JsonProperty PizzaType type, @JsonProperty int amount) {
  }

  public enum PizzaType {
    pepperoni, margherita, hawaiian, vegetarian
  }

  public record InventoryRequest(PizzaType pizzaType, int amount) {
  }

}
