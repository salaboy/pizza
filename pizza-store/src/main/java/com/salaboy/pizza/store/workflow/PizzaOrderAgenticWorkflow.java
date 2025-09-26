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

      OrderItem[] orderItems = ctx.callActivity(CreateOrderFromPrompt.class.getName(),
                orderPayloadWithPrompt,
                OrderItem[].class).await();

      OrderPayload orderPayloadWithItems = new OrderPayload(orderPayloadWithPrompt,
              Arrays.stream(orderItems).toList());

      ctx.callActivity(ConfirmOrderPlaced.class.getName(), orderPayloadWithItems).await();

      ctx.callActivity(StoreOrderActivity.class.getName(), orderPayloadWithItems).await();

      boolean requiresCooking = false;
      if(!orderPayloadWithItems.items().isEmpty()){
        for(OrderItem oi : orderPayloadWithItems.items()){
          if(oi.category().equals("pizza")){
            requiresCooking = true;
          }
        }
      }
      if(requiresCooking){
        ctx.callActivity(PlaceOrderToKitchen.class.getName(), orderPayloadWithItems).await();
        ctx.waitForExternalEvent("KitchenDone", Duration.ofMinutes(5), OrderPayload.class).await();
      }

      ctx.callActivity(DeliverOrderToCustomer.class.getName(), orderPayloadWithItems).await();

      ctx.waitForExternalEvent("PizzaDelivered", Duration.ofMinutes(10), OrderPayload.class).await();

      ctx.complete(orderPayloadWithItems);

    };
  }
}
