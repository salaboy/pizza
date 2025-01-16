package io.diagrid.dapr.workflow;

import java.util.ArrayList;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.State;
import io.dapr.workflows.runtime.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowActivityContext;
import io.diagrid.dapr.model.OrderPayload;
import io.diagrid.dapr.model.Orders;
import io.diagrid.dapr.model.WorkflowPayload;
import org.springframework.beans.factory.annotation.Autowired;

public class StoreOrderActivity implements WorkflowActivity {


    @Autowired
    private DaprClient daprClient;
    private String KEY = "orders";

    public StoreOrderActivity(DaprClient daprClient) {
        this.daprClient = daprClient;
    }

    @Override
    public Object run(WorkflowActivityContext ctx) {
        WorkflowPayload workflowPayload = ctx.getInput(WorkflowPayload.class);
        System.out.println("Store Order Activity ... ");
        String STATE_STORE_NAME = System.getenv("STATE_STORE_NAME");

        Orders orders = new Orders(new ArrayList<OrderPayload>());
        State<Orders> ordersState = daprClient.getState(STATE_STORE_NAME, KEY, null, Orders.class).block();
        if (ordersState.getValue() != null && ordersState.getValue().orders().isEmpty()) {
            orders.orders().addAll(ordersState.getValue().orders());
        }
        System.out.println("Order at first activity: " + workflowPayload.getOrder());
        orders.orders().add(workflowPayload.getOrder());
        // Save state
        daprClient.saveState(STATE_STORE_NAME, KEY, orders).block();

        return "";
    }
}
