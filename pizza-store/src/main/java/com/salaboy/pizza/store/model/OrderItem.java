package com.salaboy.pizza.store.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderItem(@JsonProperty String category,
                        @JsonProperty String name,
                        @JsonProperty int amount) {
}