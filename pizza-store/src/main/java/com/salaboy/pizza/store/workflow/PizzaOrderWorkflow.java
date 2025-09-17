package com.salaboy.pizza.store.workflow;

import com.salaboy.pizza.store.model.OrderPayload;
import com.salaboy.pizza.store.model.WorkflowPayload;
import io.dapr.durabletask.TaskFailedException;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PizzaOrderWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      Logger logger = ctx.getLogger();
      String instanceId = ctx.getInstanceId();
      logger.info("Starting Workflow: " + ctx.getName());
      logger.info("Instance ID: " + instanceId);
      logger.info("Current Orchestration Time: " + ctx.getCurrentInstant());

      WorkflowPayload workflowPayload = ctx.getInput(WorkflowPayload.class);
      workflowPayload.setWorkflowId(instanceId);
      workflowPayload.setOrder(new OrderPayload(workflowPayload.getOrder(), workflowPayload.getWorkflowId()));

      ctx.callActivity(StoreOrderActivity.class.getName(), workflowPayload).await();

      ctx.callActivity(PlaceOrderToKitchen.class.getName(), workflowPayload).await();

      ctx.waitForExternalEvent("KitchenDone", Duration.ofMinutes(5), OrderPayload.class).await();

      ctx.callActivity(DeliverOrderToCustomer.class.getName(), workflowPayload).await();

      ctx.waitForExternalEvent("PizzaDelivered", Duration.ofMinutes(10), OrderPayload.class).await();

      //ctx.callActivity(CompleteOrderActivity.class.getName(), workflowPayload).await();

      ctx.complete(workflowPayload.getOrder());

    };
  }
}
