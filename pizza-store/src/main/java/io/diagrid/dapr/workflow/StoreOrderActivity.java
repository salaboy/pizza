package io.diagrid.dapr.workflow;

import java.util.ArrayList;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.State;
import io.dapr.workflows.runtime.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowActivityContext;
import io.diagrid.dapr.model.Order;
import io.diagrid.dapr.model.Orders;

import org.springframework.beans.factory.annotation.Value;

public class StoreOrderActivity implements WorkflowActivity {

    @Value("${STATE_STORE_NAME:kvstore}")
    private String STATE_STORE_NAME;

    private String KEY = "orders";

    public StoreOrderActivity() {
    }

    @Override
    public Object run(WorkflowActivityContext ctx) {
        Order order = ctx.getInput(Order.class);
        try (DaprClient client = (new DaprClientBuilder()).build()) {
            Orders orders = new Orders(new ArrayList<Order>());
            State<Orders> ordersState = client.getState(STATE_STORE_NAME, KEY, null, Orders.class).block();
            if (ordersState.getValue() != null && ordersState.getValue().orders().isEmpty()) {
                orders.orders().addAll(ordersState.getValue().orders());
            }
            orders.orders().add(order);
            // Save state
            client.saveState(STATE_STORE_NAME, KEY, orders).block();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }
}
