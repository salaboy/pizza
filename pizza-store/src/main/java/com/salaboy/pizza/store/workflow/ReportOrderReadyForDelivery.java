package com.salaboy.pizza.store.workflow;

import com.salaboy.pizza.store.model.Event;
import com.salaboy.pizza.store.model.EventType;
import com.salaboy.pizza.store.model.OrderPayload;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReportOrderReadyForDelivery implements WorkflowActivity {

	private final SimpMessagingTemplate simpMessagingTemplate;

	public ReportOrderReadyForDelivery(SimpMessagingTemplate simpMessagingTemplate) {
		this.simpMessagingTemplate = simpMessagingTemplate;
	}

  @Override
  public Object run(WorkflowActivityContext ctx) {
    var orderPayload = ctx.getInput(OrderPayload.class);
		// Emit Event
		Event wsevent = new Event(EventType.ORDER_OUT_FOR_DELIVERY, orderPayload, "store", "Delivery in progress.");
		emitWSEvent(wsevent);
    return null;

  }

	private void emitWSEvent(Event event) {
		System.out.println("Emitting Event via WS: " + event.toString());
		simpMessagingTemplate.convertAndSend("/topic/events", event);
	}
}
