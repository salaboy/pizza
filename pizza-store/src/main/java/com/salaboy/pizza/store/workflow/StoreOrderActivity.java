package com.salaboy.pizza.store.workflow;

import com.salaboy.pizza.store.model.OrderPayload;
import com.salaboy.pizza.store.model.Orders;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.State;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class StoreOrderActivity implements WorkflowActivity {
  @Autowired
  private DaprClient daprClient;
  private String KEY = "orders";
  private final String STATE_STORE_NAME = "kvstore";

  public StoreOrderActivity(DaprClient daprClient) {
    this.daprClient = daprClient;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {

    OrderPayload orderPayload = ctx.getInput(OrderPayload.class);
    System.out.println(">> Store Order Activity ... " + orderPayload);

    String STATE_STORE_NAME = "kvstore";

    Orders orders = new Orders(new ArrayList<OrderPayload>());
    State<Orders> ordersState = daprClient.getState(STATE_STORE_NAME, KEY, null, Orders.class).block();
    if (ordersState.getValue() != null && ordersState.getValue().orders().isEmpty()) {
      orders.orders().addAll(ordersState.getValue().orders());
    }
    orders.orders().add(orderPayload);
    // Save state
    daprClient.saveState(STATE_STORE_NAME, KEY, orders).block();

    return "";
  }
}
