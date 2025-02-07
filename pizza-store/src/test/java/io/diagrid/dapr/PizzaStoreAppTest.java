package io.diagrid.dapr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PizzaStoreAppTest {

    public static void main(String[] args) {
        SpringApplication.from(PizzaStore::main)
                .with(DaprTestContainersConfig.class)
                .run(args);
    }
}
