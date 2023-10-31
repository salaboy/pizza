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
  public ResponseEntity<KitchenResponse> prepareOrder(@RequestBody(required = true) Order order) throws InterruptedException{
        for(OrderItem orderItem : order.items){
          // Check inventory
          callInventoryService("inventory", new InventoryRequest(orderItem.type, orderItem.amount));
          //If there is stock prepare the pizza
          int pizzaPrepTime = RANDOM.nextInt(10 * MS_IN_SECOND);
          System.out.println("Preparing this pizza will take: " + pizzaPrepTime);
          Thread.sleep(pizzaPrepTime);
          

           // Emit Event
          Event event = new Event(EventType.ORDER_IN_PREPARATION, order);
  
          emitEvent(event);
        }
        return ResponseEntity.ok(new KitchenResponse("Order Accepted!", order.id));
  }
  
  private void callInventoryService(String serviceName, Object payload){

  }


  private void emitEvent(Event event) {
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      client.publishEvent(PUB_SUB_NAME, 
                          PUB_SUB_TOPIC, 
                          event, 
                          singletonMap(Metadata.TTL_IN_SECONDS, MESSAGE_TTL_IN_SECONDS))
                      .block();
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
    ORDER_COMPLETED("order-completed");

    private String type;

    EventType(String type) {
      this.type = type;
    }

    @JsonValue
    public String getType(){
      return type;
    }
  }

  public record Order(@JsonProperty String id,  @JsonProperty List<OrderItem> items, @JsonProperty Date orderDate) {}
  public record KitchenResponse(@JsonProperty String message, @JsonProperty String orderId){}
  public record OrderItem(@JsonProperty PizzaType type, @JsonProperty int amount) {}
  public enum PizzaType{pepperoni, margherita, hawaiian, vegetarian}
  public record InventoryRequest(PizzaType pizzaType, int amount){}
  
}
