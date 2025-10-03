package com.salaboy.pizza.store.workflow;

import com.salaboy.pizza.store.model.Event;
import com.salaboy.pizza.store.model.EventType;
import com.salaboy.pizza.store.model.OrderPayload;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReportKitchenIssue implements WorkflowActivity {

	private final SimpMessagingTemplate simpMessagingTemplate;

	public ReportKitchenIssue(SimpMessagingTemplate simpMessagingTemplate) {
		this.simpMessagingTemplate = simpMessagingTemplate;
	}

  @Override
  public Object run(WorkflowActivityContext ctx) {
    var orderPayload = ctx.getInput(OrderPayload.class);

		// Emit Event
		Event event = new Event(EventType.KITCHEN_ISSUE, orderPayload, "store", "Unfortunately the Kitchen is having issue, your pizza cannot be cooked now.");
		emitWSEvent(event);
    return null;

  }

	private void emitWSEvent(Event event) {
		System.out.println("Emitting Event via WS: " + event.toString());
		simpMessagingTemplate.convertAndSend("/topic/events", event);
	}
}
