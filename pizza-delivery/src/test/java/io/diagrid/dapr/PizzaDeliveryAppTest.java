package io.diagrid.dapr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

@SpringBootApplication
public class PizzaDeliveryAppTest {
    public static void main(String[] args) {
        SpringApplication.from(PizzaDelivery::main)
                .with(DaprTestConfiguration.class)
                .run(args);
    }

    
    @ImportTestcontainers(DaprLocal.class)
    static class DaprTestConfiguration {
       
    }

}
