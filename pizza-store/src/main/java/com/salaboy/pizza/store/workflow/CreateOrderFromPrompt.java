package com.salaboy.pizza.store.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.salaboy.pizza.store.model.Event;
import com.salaboy.pizza.store.model.EventType;
import com.salaboy.pizza.store.model.OrderItem;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class CreateOrderFromPrompt implements WorkflowActivity {

  private ChatClient chatClient;
	private final SimpMessagingTemplate simpMessagingTemplate;

	public CreateOrderFromPrompt(ChatClient.Builder chatClientBuilder, SimpMessagingTemplate simpMessagingTemplate) {
		this.chatClient = chatClientBuilder.build();
		this.simpMessagingTemplate = simpMessagingTemplate;
	}

  public static final String DEFAULT_ORDER_PROMPT = """
			Analyze this prompt and create a list of order items.    
			based on the customer prompt:
	
			Prompt: {prompt}

			Return your response in this JSON format, but with the correct items obtained from the prompt:
			[
				\\{
				"category": "pizza",
				"name": "pepperoni",
				"amount: 1
				\\},
				\\{
				"category": "drink",
				"name": "Beer",
				"amount: 1
				\\}
			]
			
			Failing to recognize the order items, return a single order item with the category  set to "unrecognizedbyai" and name set to the content of the prompt.
			
			""";

  @Override
  public Object run(WorkflowActivityContext ctx) {
    var orderPrompt = ctx.getInput(String.class);
		System.out.println("Prompt received: " + orderPrompt);
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(new Runnable() {
			@Override
			public void run() {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
				//Event event = new Event(EventType.ORDER_PLACED, orderPayload, "store", "We received the payment your order is confirmed.");
        //emitWSEvent(new Event("", ));
			}
		});

		return this.chatClient.prompt()
            .user(u -> u.text(DEFAULT_ORDER_PROMPT)
                    .param("prompt", orderPrompt))
            .call()
            .entity(OrderItem[].class);

  }

	private void emitWSEvent(Event event) {
		System.out.println("Emitting Event via WS: " + event.toString());
		simpMessagingTemplate.convertAndSend("/topic/events", event);
	}
}
