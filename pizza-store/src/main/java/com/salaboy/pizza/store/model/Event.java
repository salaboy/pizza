package com.salaboy.pizza.store.model;

public record Event(EventType type, OrderPayload order,
                    String service, String message) {
}
