package com.salaboy.pizza.store.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public record OrderPayload(@JsonProperty String id,
                           @JsonProperty Customer customer,
                           @JsonProperty List<OrderItem> items,
                           @JsonProperty String prompt,
                           @JsonProperty Date orderDate,
                           @JsonProperty Status status) {

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public OrderPayload(String id, Customer customer, List<OrderItem> items, String prompt, Date orderDate, Status status) {
    if (id == null || id.isEmpty()) {
      this.id = UUID.randomUUID().toString();
    } else {
      this.id = id;
    }
    this.customer = customer;
    this.items = items;
    this.prompt = prompt;
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

  public OrderPayload(String id, Customer customer, List<OrderItem> orderItems, Date orderDate, Status status) {
    this(id, customer, orderItems, "", orderDate, status);
  }

  public OrderPayload(Customer customer, List<OrderItem> items, Date orderDate, Status status) {
    this(UUID.randomUUID().toString(), customer, items, "", orderDate, status);
  }

  public OrderPayload(Customer customer, List<OrderItem> items) {
    this(UUID.randomUUID().toString(), customer, items, "", new Date(), Status.created);
  }

  public OrderPayload(Customer customer, String prompt, String workflowId) {
    this(UUID.randomUUID().toString(), customer, null, prompt, new Date(), Status.created);
  }


  public OrderPayload(OrderPayload order, List<OrderItem> orderItems) {
    this(order.id, order.customer, orderItems, order.prompt, order.orderDate, order.status);
  }

  public OrderPayload(OrderPayload order) {
    this(order.id, order.customer, order.items, order.prompt, order.orderDate, order.status);
  }

  public OrderPayload(OrderPayload order, Status status) {
    this(order.id, order.customer, order.items, order.prompt, order.orderDate, status);
  }
}
