package io.diagrid.dapr;
import java.util.List;
import java.util.UUID;
import java.util.Random;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.State;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
@RestController
public class PizzaKitchen {

  private static final int MS_IN_SECOND = 1000;
  private static final Random RANDOM = new Random();

  public static void main(String[] args) {
    SpringApplication.run(PizzaKitchen.class, args);

  }

   

  @PutMapping("/prepare")
  public ResponseEntity<KitchenResponse> prepareOrder(@RequestBody(required = true) Order order) throws InterruptedException{
        for(OrderItem orderItem : order.items){
          callService("inventory", new InventoryRequest(orderItem.type, orderItem.amount));
          int pizzaPrepTime = RANDOM.nextInt(10000);
          System.out.println("Preparing this pizza will take: " + pizzaPrepTime);
          Thread.sleep(pizzaPrepTime);
        }
        return ResponseEntity.ok(new KitchenResponse("Order Accepted!", order.id));
  }
  
  private void callService(String serviceName, Object payload){
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      client.invokeMethod(serviceName, serviceName, client, null, null, null).block();
    }catch(Exception ex){
        ex.printStackTrace();
    }  

  }

  public record Order(@JsonProperty String id,  @JsonProperty List<OrderItem> items, @JsonProperty Date orderDate) {}
  public record KitchenResponse(@JsonProperty String message, @JsonProperty String orderId){}
  public record OrderItem(@JsonProperty PizzaType type, @JsonProperty int amount) {}
  public enum PizzaType{pepperoni, margherita, hawaiian, vegetarian}
  public record InventoryRequest(PizzaType pizzaType, int amount){}
  
}
