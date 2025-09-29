package com.salaboy.pizza.store.workflow;

import com.salaboy.pizza.store.model.Event;
import com.salaboy.pizza.store.model.EventType;
import com.salaboy.pizza.store.model.OrderPayload;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReportOrderProcessingIssue implements WorkflowActivity {

	private final SimpMessagingTemplate simpMessagingTemplate;

	public ReportOrderProcessingIssue(SimpMessagingTemplate simpMessagingTemplate) {
		this.simpMessagingTemplate = simpMessagingTemplate;
	}

  @Override
  public Object run(WorkflowActivityContext ctx) {
    var orderPayload = ctx.getInput(OrderPayload.class);

		// Emit Event
		Event event = new Event(EventType.AI_IS_DOWN_OR_TOO_EXPENSIVE, orderPayload, "store", "The AI couldn't handle your order, please try again later.");
		emitWSEvent(event);
    return null;

  }

	private void emitWSEvent(Event event) {
		System.out.println("Emitting Event via WS: " + event.toString());
		simpMessagingTemplate.convertAndSend("/topic/events", event);
	}
}
