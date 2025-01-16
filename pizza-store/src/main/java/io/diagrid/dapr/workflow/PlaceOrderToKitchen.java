package io.diagrid.dapr.workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import io.dapr.workflows.runtime.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowActivityContext;
import io.diagrid.dapr.model.OrderPayload;
import io.diagrid.dapr.model.WorkflowPayload;

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
        if(daprHttp == null || daprHttp.isEmpty()){
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
