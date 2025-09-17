package com.salaboy.pizza.store.workflow;

import com.salaboy.pizza.store.model.OrderPayload;
import com.salaboy.pizza.store.model.WorkflowPayload;
import io.dapr.spring.boot.autoconfigure.client.DaprConnectionDetails;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DeliverOrderToCustomer implements WorkflowActivity {

  private RestTemplate restTemplate;

  private DaprConnectionDetails daprConnectionDetails;

  public DeliverOrderToCustomer(RestTemplate restTemplate, DaprConnectionDetails daprConnectionDetails) {
    this.restTemplate = restTemplate;
    this.daprConnectionDetails = daprConnectionDetails;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    WorkflowPayload workflowPayload = ctx.getInput(WorkflowPayload.class);
    System.out.println("Delivering Pizza to Customer Activity ... ");

    String daprHttp = daprConnectionDetails.getHttpEndpoint();
    String daprAPIToken = daprConnectionDetails.getApiToken();

    HttpHeaders headers = new HttpHeaders();

    headers.add("Content-Type", "application/json");
    headers.add("dapr-app-id", "delivery-service");
    headers.add("dapr-api-token", daprAPIToken);
    HttpEntity<OrderPayload> request = new HttpEntity<OrderPayload>(workflowPayload.getOrder(), headers);
    restTemplate.put(
            daprHttp + "/deliver", request);

    return "";
  }
}
