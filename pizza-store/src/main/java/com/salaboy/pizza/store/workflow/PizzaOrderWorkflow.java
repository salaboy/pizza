package com.salaboy.pizza.store.workflow;

import com.salaboy.pizza.store.model.*;
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
      logger.info("Starting Workflow: {}", ctx.getName());
      logger.info("Instance ID: {}", instanceId);
      logger.info("Current Orchestration Time: {}", ctx.getCurrentInstant());

      OrderPayload orderPayload = ctx.getInput(OrderPayload.class);

      ctx.callActivity(ConfirmOrderPlaced.class.getName(), orderPayload).await();

      ctx.callActivity(StoreOrderActivity.class.getName(), orderPayload).await();

      ctx.callActivity(PlaceOrderToKitchen.class.getName(), orderPayload).await();

      OrderPayload orderFromTheKitchen = ctx.waitForExternalEvent("KitchenDone", Duration.ofMinutes(5), OrderPayload.class).await();

      ctx.callActivity(StoreOrderActivity.class.getName(), new OrderPayload(orderFromTheKitchen, Status.delivery)).await();

      ctx.callActivity(DeliverOrderToCustomer.class.getName(), orderPayload).await();

      ctx.waitForExternalEvent("PizzaDelivered", Duration.ofMinutes(10), OrderPayload.class).await();

      ctx.complete(orderPayload);

    };
  }
}
