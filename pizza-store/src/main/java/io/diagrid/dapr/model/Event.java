package io.diagrid.dapr.model;

public record Event(EventType type, OrderPayload order, String service, String message) {
  }
