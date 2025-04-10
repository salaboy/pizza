package com.salaboy.pizza.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PizzaDeliveryAppTest {

    public static void main(String[] args) {
        SpringApplication.from(PizzaDelivery::main)
                .with(DaprTestContainersConfig.class)
                .run(args);
    }
}
