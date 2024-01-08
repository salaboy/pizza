package io.diagrid.dapr.model;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderPayload(@JsonProperty String id, @JsonProperty Customer customer, @JsonProperty List<OrderItem> items,
      @JsonProperty Date orderDate, @JsonProperty Status status, @JsonProperty String workflowId) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrderPayload(String id, Customer customer, List<OrderItem> items, Date orderDate, Status status, String workflowId) {
      if (id == null) {
        this.id = UUID.randomUUID().toString();
      } else {
        this.id = id;
      }
      this.customer = customer;
      this.items = items;
      this.workflowId = workflowId;
      if (orderDate == null) {
        this.orderDate = new Date();
      } else {
        this.orderDate = orderDate;
      }
      if (status == null) {
        this.status = Status.created;
      } else {
        this.status = status;
      }
    }

    public OrderPayload(Customer customer, List<OrderItem> items, Date orderDate, Status status, String workflowId) {
      this(UUID.randomUUID().toString(), customer, items, orderDate, status, workflowId);
    }

    public OrderPayload(Customer customer, List<OrderItem> items, String workflowId) {
      this(UUID.randomUUID().toString(), customer, items, new Date(), Status.created, workflowId);
    }

    public OrderPayload(OrderPayload order, String workflowId) {
      this(order.id, order.customer, order.items, order.orderDate, order.status, workflowId);
    }
    
    public OrderPayload(OrderPayload order) {
      this(order.id, order.customer, order.items, order.orderDate, order.status, order.workflowId);
    }
  }

