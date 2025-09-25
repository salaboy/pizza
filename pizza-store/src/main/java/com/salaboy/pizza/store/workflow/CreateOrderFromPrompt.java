package com.salaboy.pizza.store.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.salaboy.pizza.store.model.OrderItem;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CreateOrderFromPrompt implements WorkflowActivity {

  private ChatClient chatClient;

	public CreateOrderFromPrompt(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
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

    return this.chatClient.prompt()
            .user(u -> u.text(DEFAULT_ORDER_PROMPT)
                    .param("prompt", orderPrompt))
            .call()
            .entity(OrderItem[].class);

  }
}
