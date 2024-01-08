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

import org.springframework.beans.factory.annotation.Value;

public class CompleteOrderActivity implements WorkflowActivity {

    // @Value("${STATE_STORE_NAME:kvstore}")
    // private String STATE_STORE_NAME = "kvstore";

    private String KEY = "orders";

    @Override
    public Object run(WorkflowActivityContext ctx) {
        WorkflowPayload workflowPayload = ctx.getInput(WorkflowPayload.class);
        System.out.println("Completing Order Activity ... ");
        String STATE_STORE_NAME = System.getenv("STATE_STORE_NAME");
      

        //@TODO UPDATE ORDER STATUS TO COMPLETED
        // try (DaprClient client = (new DaprClientBuilder()).build()) {
        //     Orders orders = new Orders(new ArrayList<OrderPayload>());
        //     State<Orders> ordersState = client.getState(STATE_STORE_NAME, KEY, null, Orders.class).block();
        //     if (ordersState.getValue() != null && ordersState.getValue().orders().isEmpty()) {
        //         orders.orders().addAll(ordersState.getValue().orders());
        //     }
        //     System.out.println("Order at first activity: " + workflowPayload.getOrder());
        //     orders.orders().add(workflowPayload.getOrder());
        //     // Save state
        //     client.saveState(STATE_STORE_NAME, KEY, orders).block();

        // } catch (Exception ex) {
        //     ex.printStackTrace();
        // }
        return "";
    }
}
