package io.diagrid.dapr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import io.restassured.RestAssured;

@SpringBootApplication
public class PizzaStoreAppTest {
    public static void main(String[] args) {
        SpringApplication.from(PizzaStore::main)
                .with(DaprTestConfiguration.class)
                .run(args);
    }

    @ImportTestcontainers(DaprLocal.class)
    static class DaprTestConfiguration {

        @Bean
        WireMockContainer wireMockContainer(DynamicPropertyRegistry properties) {

            var container = new WireMockContainer("wiremock/wiremock:3.1.0")
                    .withMappingFromResource("kitchen", "kitchen-service-stubs.json");

            properties.add("dapr-http.base-url", container::getBaseUrl);
            return container;
        }
    }

}
