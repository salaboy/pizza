package io.diagrid.dapr.workflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import io.dapr.workflows.runtime.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowActivityContext;
import io.diagrid.dapr.model.OrderPayload;
import io.diagrid.dapr.model.WorkflowPayload;

public class DeliverOrderToCustomer implements WorkflowActivity {

//   @Value("${dapr-http.base-url:http://localhost:3500}")
//   private String daprHttp;
//   @Value("${DAPR_API_TOKEN}")
//   private String API_TOKEN;
  private static RestTemplate restTemplate;


    @Override
    public Object run(WorkflowActivityContext ctx) {
        WorkflowPayload workflowPayload = ctx.getInput(WorkflowPayload.class);
        System.out.println("Delivering Pizza to Customer Activity ... ");

        String daprHttp = System.getenv("DAPR_HTTP_ENDPOINT");
        restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        headers.add("Content-Type", "application/json");
        headers.add("dapr-app-id", "delivery-service");
        headers.add("dapr-api-token", System.getenv("DAPR_API_TOKEN"));
        HttpEntity<OrderPayload> request = new HttpEntity<OrderPayload>(workflowPayload.getOrder(), headers);
        restTemplate.put(
            daprHttp + "/deliver", request);


        return "";   
    }
}
