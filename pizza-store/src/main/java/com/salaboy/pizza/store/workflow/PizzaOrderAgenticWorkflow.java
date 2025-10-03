package com.salaboy.pizza.store.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.salaboy.pizza.store.model.Customer;
import com.salaboy.pizza.store.model.OrderItem;
import com.salaboy.pizza.store.model.OrderPayload;
import com.salaboy.pizza.store.model.Status;
import io.dapr.durabletask.TaskFailedException;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
public class PizzaOrderAgenticWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      Logger logger = ctx.getLogger();
      String instanceId = ctx.getInstanceId();
      logger.info("Starting Workflow: {}", ctx.getName());
      logger.info("Instance ID: {}", instanceId);
      logger.info("Current Orchestration Time: {}", ctx.getCurrentInstant());

      OrderPayload orderPayloadWithPrompt = ctx.getInput(OrderPayload.class);


      OrderItem[] orderItems = null;
      try {
        orderItems = ctx.callActivity(CreateOrderFromPrompt.class.getName(),
                orderPayloadWithPrompt,
                OrderItem[].class).await();

      } catch (TaskFailedException tfe) {
        ctx.callActivity(ReportOrderProcessingIssue.class.getName(), orderPayloadWithPrompt).await();
        ctx.complete(orderPayloadWithPrompt);
      }

      OrderPayload orderPayloadWithItems = new OrderPayload(orderPayloadWithPrompt,
              Arrays.stream(orderItems).toList());


      ctx.callActivity(ConfirmOrderPlaced.class.getName(), orderPayloadWithItems).await();

      try {
        ctx.callActivity(StoreOrderActivity.class.getName(), orderPayloadWithItems).await();
      } catch (TaskFailedException tfe){
        ctx.callActivity(ReportStoringIssue.class.getName(), orderPayloadWithPrompt).await();
        ctx.complete(orderPayloadWithItems);
      }

      boolean requiresCooking = false;
      if (!orderPayloadWithItems.items().isEmpty()) {
        for (OrderItem oi : orderPayloadWithItems.items()) {
          if (oi.category().equals("pizza")) {
            requiresCooking = true;
          }
        }
      }
      if (requiresCooking) {
        try {
          ctx.callActivity(PlaceOrderToKitchen.class.getName(), orderPayloadWithItems).await();
        } catch (TaskFailedException tfe){
          ctx.callActivity(ReportKitchenIssue.class.getName(), orderPayloadWithPrompt).await();
          ctx.complete(orderPayloadWithItems);
        }

        ctx.waitForExternalEvent("KitchenDone", Duration.ofMinutes(5), OrderPayload.class).await();
      }
      ctx.callActivity(ReportOrderReadyForDelivery.class.getName(), orderPayloadWithPrompt).await();
      try {
        ctx.callActivity(DeliverOrderToCustomer.class.getName(), orderPayloadWithItems).await();
      } catch (TaskFailedException tfe){
        ctx.callActivity(ReportDeliveryIssue.class.getName(), orderPayloadWithPrompt).await();
        ctx.complete(orderPayloadWithItems);
      }

      ctx.waitForExternalEvent("PizzaDelivered", Duration.ofMinutes(10), OrderPayload.class).await();

      ctx.complete(orderPayloadWithItems);

    };
  }
}
