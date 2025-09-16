package com.salaboy.pizza.store.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KitchenResponse(@JsonProperty String message, @JsonProperty String orderId) {
}
