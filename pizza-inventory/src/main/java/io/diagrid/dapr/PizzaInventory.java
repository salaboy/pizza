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
public class PizzaInventory {


  public static void main(String[] args) {
    SpringApplication.run(PizzaInventory.class, args);

  }

  private String STATE_STORE_NAME = "statestore";

  @GetMapping("/inventory")
  public ResponseEntity<InventoryResult> checkInventory(@QueryParam(required = true) PizzaType pizzaType){
    
    try (DaprClient client = (new DaprClientBuilder()).build()) {
        
        // Get inventory count based on pizza type
        client.getState(STATE_STORE_NAME, order.id, order).block();

        return ResponseEntity.ok(new InventoryResult(pizzaType, 0));
    }catch(Exception ex){
        ex.printStackTrace();
        return ResponseEntity.internalServerError().body("Error placing the order! Check the logs.");
    }

  }


  @PutMapping("/inventory")
  public ResponseEntity<String> updateInventory(@RequestBody(required = true) InventoryRequest inventoryRequest){
    
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      
      //Get State from pizza type, decrement by inventoryRequest.stockCount

        // Save state
        client.saveState(STATE_STORE_NAME, order.id, order).block();

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

  
  public enum PizzaType{pepperoni, margherita, hawaiian, vegetarian}
  public record InventoryResult(PizzaType pizzaType, int stockCount){}
  public record InventoryRequest(PizzaType pizzaType, int stockCount){}

}


