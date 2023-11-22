package io.diagrid.dapr.model;

public record Event(EventType type, Order order, String service, String message) {
  }
