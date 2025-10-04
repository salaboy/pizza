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

      try {
        ctx.callActivity(StoreOrderActivity.class.getName(), orderPayload).await();
      } catch (TaskFailedException tfe){
        ctx.callActivity(ReportStoringIssue.class.getName(), orderPayload).await();
        ctx.complete(orderPayload);
      }

      boolean requiresCooking = false;
      if (!orderPayload.items().isEmpty()) {
        for (OrderItem oi : orderPayload.items()) {
          if (oi.category().equals("pizza")) {
            requiresCooking = true;
          }
        }
      }
      if (requiresCooking) {
        try {
          ctx.callActivity(PlaceOrderToKitchen.class.getName(), orderPayload).await();
        } catch (TaskFailedException tfe){
          ctx.callActivity(ReportKitchenIssue.class.getName(), orderPayload).await();
          ctx.complete(orderPayload);
        }

        ctx.waitForExternalEvent("KitchenDone", Duration.ofMinutes(5), OrderPayload.class).await();
      }
      ctx.callActivity(ReportOrderReadyForDelivery.class.getName(), orderPayload).await();
      try {
        ctx.callActivity(DeliverOrderToCustomer.class.getName(), orderPayload).await();
      } catch (TaskFailedException tfe){
        ctx.callActivity(ReportDeliveryIssue.class.getName(), orderPayload).await();
        ctx.complete(orderPayload);
      }

      ctx.waitForExternalEvent("PizzaDelivered", Duration.ofMinutes(10), OrderPayload.class).await();

      try {
        ctx.callActivity(StoreOrderActivity.class.getName(), new OrderPayload(orderPayload, Status.delivery)).await();
      } catch (TaskFailedException tfe){
        ctx.callActivity(ReportStoringIssue.class.getName(), orderPayload).await();
        ctx.complete(orderPayload);
      }

      ctx.complete(orderPayload);

    };
  }
}
