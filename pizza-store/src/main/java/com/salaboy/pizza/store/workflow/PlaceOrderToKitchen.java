package com.salaboy.pizza.store.workflow;

import com.salaboy.pizza.store.model.OrderPayload;
import io.dapr.spring.boot.autoconfigure.client.DaprConnectionDetails;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Component
public class PlaceOrderToKitchen implements WorkflowActivity {


  private RestTemplate restTemplate;

  private DaprConnectionDetails daprConnectionDetails;

  public PlaceOrderToKitchen(RestTemplate restTemplate, DaprConnectionDetails daprConnectionDetails) {
    this.restTemplate = restTemplate;
    this.daprConnectionDetails = daprConnectionDetails;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    OrderPayload orderPayload = ctx.getInput(OrderPayload.class);
    System.out.println("Placing Order to Kitchen Activity ... ");

    String daprHttp = daprConnectionDetails.getHttpEndpoint() + ":" + daprConnectionDetails.getHttpPort();
    String daprAPIToken = daprConnectionDetails.getApiToken();

    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("dapr-app-id", "kitchen-service");
    headers.add("dapr-api-token", daprAPIToken);
    HttpEntity<OrderPayload> request = new HttpEntity<OrderPayload>(orderPayload, headers);

    System.out.println("Sending request via Dapr to URL: " + daprHttp + "/prepare");

    restTemplate.put(
            daprHttp + "/prepare", request);

    return "";
  }
}
