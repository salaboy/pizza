package com.salaboy.pizza.store.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Customer(@JsonProperty String name, @JsonProperty String email) {
}
