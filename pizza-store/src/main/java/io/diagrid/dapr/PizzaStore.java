package io.diagrid.dapr;
import java.util.UUID;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
 private String ORDER = "pizza";

  @PostMapping("/")
  public String newPizza(){

    try (DaprClient client = (new DaprClientBuilder()).build()) {
        
        // Save state
        client.saveState(STATE_STORE_NAME, ORDER, new Pizza("peperoni", "L")).block();
        return "Done!";
    }catch(Exception ex){
        ex.printStackTrace();
        return "Error!";
    }
    

  }

  @GetMapping("/")
  public Pizza getPizza(){

    try (DaprClient client = (new DaprClientBuilder()).build()) {
       

        // Get state
        State<Pizza> retrievedMessage = client.getState(STATE_STORE_NAME, ORDER, Pizza.class).block();

        return retrievedMessage.getValue();

    }catch(Exception ex){
        ex.printStackTrace();
    }
    return null;
  }

public record Pizza (String type, String size) {}

}
