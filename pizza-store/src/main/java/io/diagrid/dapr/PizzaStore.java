package io.diagrid.dapr;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.State;

@SpringBootApplication
@RestController
public class PizzaStore {

  @Value( "${dapr-http.base-url:http://localhost:3500}" )
  private String daprHttp;

  public static void main(String[] args) {
    SpringApplication.run(PizzaStore.class, args);

  }

  private String STATE_STORE_NAME = "statestore";
  private String KEY = "orders";
  private static RestTemplate restTemplate;

  @PostMapping("/order")
  public ResponseEntity<Order> placeOrder(@RequestBody(required = true) Order order) {

    // Store Order and Emit Event NewOrder
    store(order);

    // Process Order, sent to kitcken
    Order updatedOrder = callKitchenService(order);

    store(updatedOrder);

    return ResponseEntity.ok(updatedOrder);
  }

  @GetMapping("/order")
  public ResponseEntity<Orders> getOrders() {

    Orders orders = loadOrders();

    return ResponseEntity.ok(orders);
  }

  public record Customer(@JsonProperty String name, @JsonProperty String email) {
  }

  public record OrderItem(@JsonProperty PizzaType type, @JsonProperty int amount) {
  }

  public enum PizzaType {
    pepperoni, margherita, hawaiian, vegetarian
  }

  public enum Status {
    created, placed, notplaced, instock, notinstock, inpreparation, completed, failed
  }


  public record KitchenResponse(@JsonProperty String message, @JsonProperty String orderId){}

  private record Orders(@JsonProperty List<Order> orders){}

  public record Order(@JsonProperty String id, @JsonProperty Customer customer, @JsonProperty List<OrderItem> items,
      @JsonProperty Date orderDate, @JsonProperty Status status) {
 
    public Order(String id, Customer customer, List<OrderItem> items, Date orderDate, Status status) {
      this.id = id;
      this.customer = customer;
      this.items = items;
      this.orderDate = orderDate;
      this.status = status;
    }

    public Order(Customer customer, List<OrderItem> items, Date orderDate, Status status) {
      this(UUID.randomUUID().toString(), customer, items, orderDate, status);
    }

    public Order(Customer customer, List<OrderItem> items) {
      this(UUID.randomUUID().toString(), customer, items, new Date(), Status.created);
    }

    public Order(Order order){
      this(order.id, order.customer, order.items, order.orderDate, order.status);
    }
  }

  private void store(Order order) {
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      Orders orders = new Orders(new ArrayList<Order>());
      State<Orders> ordersState = client.getState(STATE_STORE_NAME, KEY, null, Orders.class).block();
      if(ordersState.getValue() != null && ordersState.getValue().orders.isEmpty()){
          orders.orders.addAll(ordersState.getValue().orders);
      }
      orders.orders.add(order);
      // Emit Event
      //client.publishEvent(STATE_STORE_NAME, KEY, ordersState, null);
      // Save state
      client.saveState(STATE_STORE_NAME, KEY, orders).block();

    } catch (Exception ex) {
      ex.printStackTrace();
    }

  }


  private Order callKitchenService(Order order) {


    restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("dapr-app-id", "kitchen-service");
    HttpEntity<Order> request = new HttpEntity<Order>(order, headers);
    KitchenResponse kitchenResponse = restTemplate.postForObject(
					daprHttp + "/kitchen/", request, KitchenResponse.class);
    if(kitchenResponse.message.equals("placed")){
      return new Order(order.id, order.customer, order.items, order.orderDate, Status.placed);
    }
		return new Order(order.id, order.customer, order.items, order.orderDate, Status.notplaced);
  }

  private Orders loadOrders() {
    try (DaprClient client = (new DaprClientBuilder()).build()) {
       State<Orders> ordersState = client.getState(STATE_STORE_NAME, KEY, null, Orders.class).block();
      return ordersState.getValue();

    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
   
  }

}



















 // DaprClientBuilder builder = new DaprClientBuilder();
    // try (DaprPreviewClient previewClient = builder.buildPreviewClient()) {

    // Query query = new Query()
    // .setFilter(new EmptyFilter())
    // .setSort(Arrays.asList(new Sorting("orderDate", Sorting.Order.DESC)));

    // QueryStateRequest queryStateRequest = new QueryStateRequest(STATE_STORE_NAME)
    // .setQuery(query);

    // QueryStateResponse<Order> result =
    // previewClient.queryState(queryStateRequest, Order.class).block();
    // List<Order> orders = new ArrayList<>(result.getResults().size());
    // for (QueryStateItem<Order> item : result.getResults()) {
    // System.out.println("Key: " + item.getKey());
    // System.out.println("Data: " + item.getValue());
    // orders.add(item.getValue());
    // }

    // return orders;

    // } catch (Exception ex) {
    // ex.printStackTrace();
    // }
    // return null;





// class EmptyFilter extends Filter<Void> {
// public EmptyFilter() {
// super("EMPTY");
// }

// @Override
// public String getRepresentation() {
// return "EMPTY";
// }

// @Override
// public Boolean isValid() {
// return true;
// }
// }
