package com.salaboy.pizza.store.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventType {

  ORDER_PROCESSED_BY_AI("order-processed-by-ai"),
  AI_IS_DOWN_OR_TOO_EXPENSIVE("ai-down"),
  ORDER_PLACED("order-placed"),
  STORING_ISSUE("storing-issue"),
  ITEMS_IN_STOCK("items-in-stock"),
  ITEMS_NOT_IN_STOCK("items-not-in-stock"),
  ORDER_IN_PREPARATION("order-in-preparation"),
  KITCHEN_ISSUE("kitchen-issue"),
  ORDER_READY("order-ready"),
  ORDER_OUT_FOR_DELIVERY("order-out-for-delivery"),
  ORDER_ON_ITS_WAY("order-on-its-way"),
  DELIVERY_ISSUE("delivery-issue"),
  ORDER_COMPLETED("order-completed"),
  PING("ping");


  private String type;

  EventType(String type) {
    this.type = type;
  }

  @JsonValue
  public String getType() {
    return type;
  }
}
