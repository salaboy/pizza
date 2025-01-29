package io.diagrid.dapr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PizzaKitchenAppTest {

    public static void main(String[] args) {
        SpringApplication.from(PizzaKitchen::main)
                .with(DaprTestContainersConfig.class)
                .run(args);
    }
}
