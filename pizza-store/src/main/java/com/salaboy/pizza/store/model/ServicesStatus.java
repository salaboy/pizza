package com.salaboy.pizza.store.model;

public record ServicesStatus(boolean kvStoreOk, boolean pubsubOk, boolean kitchenOk, boolean deliveryOk) {
}
