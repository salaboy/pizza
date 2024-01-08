package io.diagrid.dapr.model;

public class WorkflowPayload{
    private String workflowId;
    private OrderPayload order;

    public WorkflowPayload() {
    }

    public WorkflowPayload( OrderPayload order) {
        this.order = order;
    }
   
    public WorkflowPayload(String workflowId, OrderPayload order) {
        this.order = order;
        this.workflowId = workflowId;
    }



    public OrderPayload getOrder() {
        return order;
    }
    public void setOrder(OrderPayload order) {
        this.order = order;
    }



    public String getWorkflowId() {
        return workflowId;
    }



    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    

    
}
