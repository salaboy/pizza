package io.diagrid.dapr;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.State;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
@RestController
public class PizzaInventory {


  public static void main(String[] args) {
    SpringApplication.run(PizzaInventory.class, args);

  }


  private String STATE_STORE_NAME = "statestore";

  @GetMapping("/inventory")
  public ResponseEntity<InventoryResult> checkInventory(@RequestParam(required = true) PizzaType pizzaType){
    
    try (DaprClient client = (new DaprClientBuilder()).build()) {
        
        // Get inventory count based on pizza type
        State<Integer> stockCount = client.getState(STATE_STORE_NAME, pizzaType.toString(), Integer.class).block();
        return ResponseEntity.ok(new InventoryResult(pizzaType, stockCount.getValue()));
    }catch(Exception ex){
        ex.printStackTrace();
        return ResponseEntity.internalServerError().build();
    }

  }


  @PutMapping("/inventory")
  public ResponseEntity<InventoryResult> updateInventory(@RequestBody(required = true) InventoryRequest inventoryRequest){
    
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      
        //Get State from pizza type, decrement by inventoryRequest.stockCount
        State<Integer> stockCount = client.getState(STATE_STORE_NAME, inventoryRequest.pizzaType.toString(), Integer.class).block();
        // Save state
        int newStockCount = stockCount.getValue() - inventoryRequest.amount;
        client.saveState(STATE_STORE_NAME, inventoryRequest.pizzaType.toString(), newStockCount).block();

        return ResponseEntity.ok(new InventoryResult(inventoryRequest.pizzaType, newStockCount));
    }catch(Exception ex){
        ex.printStackTrace();
        return ResponseEntity.internalServerError().build();
    }

    
  }
  
  @PostConstruct
  private void initStock(){
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      client.saveState(STATE_STORE_NAME, PizzaType.hawaiian.toString(), 10).block();
      client.saveState(STATE_STORE_NAME, PizzaType.margherita.toString(), 10).block();
      client.saveState(STATE_STORE_NAME, PizzaType.pepperoni.toString(), 10).block();
      client.saveState(STATE_STORE_NAME, PizzaType.vegetarian.toString(), 10).block();
      }catch(Exception ex){
        ex.printStackTrace();
        
    }
  }

  
  public enum PizzaType{pepperoni, margherita, hawaiian, vegetarian}
  public record InventoryResult(PizzaType pizzaType, int stockCount){}
  public record InventoryRequest(PizzaType pizzaType, int amount){}
  
}
