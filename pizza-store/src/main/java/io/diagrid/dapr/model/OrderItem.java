package io.diagrid.dapr.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderItem(@JsonProperty PizzaType type, @JsonProperty int amount) {
  }
