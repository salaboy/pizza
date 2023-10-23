package io.diagrid.dapr;

import static org.mockito.ArgumentMatchers.eq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.QueryStateItem;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.State;
import io.dapr.client.domain.query.Query;
import io.dapr.client.domain.query.Sorting;
import io.dapr.client.domain.query.filters.EqFilter;
import io.dapr.client.domain.query.filters.Filter;

@SpringBootApplication
@RestController
public class PizzaStore {

  public static void main(String[] args) {
    SpringApplication.run(PizzaStore.class, args);

  }

  private String STATE_STORE_NAME = "statestore";
  private String KEY = "orders";

  @PostMapping("/order")
  public ResponseEntity<String> placeOrder(@RequestBody(required = true) Order order) {

    // Store Order and Emit Event NewOrder
    store(order);

    // Process Order, sent to kitcken
    callService("kitchen", order);

    return ResponseEntity.ok("Done");
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
    placed, instock, notinstock, inpreparation, completed
  }

  private record Orders(@JsonProperty List<Order> orders){}

  public record Order(@JsonProperty String id, @JsonProperty Customer customer, @JsonProperty List<OrderItem> items,
      @JsonProperty Date orderDate, @JsonProperty Status status) {
    public Order(String id, Customer customer, List<OrderItem> items, Date orderDate, Status status) {
      this.id = UUID.randomUUID().toString();
      this.customer = customer;
      this.items = items;
      this.orderDate = orderDate;
      this.status = status;
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

      // Save state
      client.saveState(STATE_STORE_NAME, KEY, orders).block();
    } catch (Exception ex) {
      ex.printStackTrace();
    }

  }

  private void callService(String service, Object payload) {

  }

  private Orders loadOrders() {
    try (DaprClient client = (new DaprClientBuilder()).build()) {
       State<Orders> ordersState = client.getState(STATE_STORE_NAME, KEY, null, Orders.class).block();
      return ordersState.getValue();

    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
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
  }

}

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
