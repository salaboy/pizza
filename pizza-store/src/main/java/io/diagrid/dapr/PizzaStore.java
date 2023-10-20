package io.diagrid.dapr;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.State;

@SpringBootApplication
@RestController
public class PizzaStore {


  public static void main(String[] args) {
    SpringApplication.run(PizzaStore.class, args);

  }

 private String STATE_STORE_NAME = "statestore";
 private String ORDERS = "orders";

  @PostMapping("/order")
  public ResponseEntity<String> placeOrder(@RequestBody(required = true) Order order){
    
    try (DaprClient client = (new DaprClientBuilder()).build()) {
        
        // Save state
        client.saveState(STATE_STORE_NAME, ORDERS, order).block();
        return ResponseEntity.ok("Done!");
    }catch(Exception ex){
        ex.printStackTrace();
        return ResponseEntity.internalServerError().body("Error placing the order! Check the logs.");
    }

  }

  // @GetMapping("/")
  // public List<Order> getOrders(){

  //   try (DaprClient client = (new DaprClientBuilder()).build()) {
       

  //       // Get state
  //       State<Order> retrievedMessage = client.getState(STATE_STORE_NAME, ORDER, Order.class).block();

  //       return retrievedMessage.getValue();

  //   }catch(Exception ex){
  //       ex.printStackTrace();
  //   }
  //   return null;
  // }

  public record Customer(String name, String email){}
  public record OrderItem(PizzaType type, int amount){}
  public enum PizzaType{pepperoni, margherita, hawaiian, vegetarian}
  public enum Status{placed, instock, notinstock,inpreparation,completed}
  public record Order(String id, Customer customer, List<OrderItem> items, Date orderDate, Status status){
    public Order(Customer customer, List<OrderItem> items, Date orderDate, Status status) {
      this(UUID.randomUUID().toString(), customer, items, orderDate, status);
    }
  }

}


