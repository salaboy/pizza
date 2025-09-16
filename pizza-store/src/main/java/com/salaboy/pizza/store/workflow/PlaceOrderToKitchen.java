package com.salaboy.pizza.store.workflow;

import com.salaboy.pizza.store.model.OrderPayload;
import com.salaboy.pizza.store.model.WorkflowPayload;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Component
public class PlaceOrderToKitchen implements WorkflowActivity {

  @Autowired
  private RestTemplate restTemplate;

  public PlaceOrderToKitchen(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    WorkflowPayload workflowPayload = ctx.getInput(WorkflowPayload.class);
    System.out.println("Placing Order to Kitchen Activity ... ");
    String daprHttp = System.getenv("DAPR_HTTP_ENDPOINT");
    if (daprHttp == null || daprHttp.isEmpty()) {
      daprHttp = "http://localhost:3500";
    }
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("dapr-app-id", "kitchen-service");
    headers.add("dapr-api-token", System.getenv("DAPR_API_TOKEN"));
    HttpEntity<OrderPayload> request = new HttpEntity<OrderPayload>(workflowPayload.getOrder(), headers);
    restTemplate.put(
            daprHttp + "/prepare", request);

    return "";
  }
}
