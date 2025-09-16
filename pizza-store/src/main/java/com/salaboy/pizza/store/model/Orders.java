package com.salaboy.pizza.store.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Orders(@JsonProperty List<OrderPayload> orders) {
}
