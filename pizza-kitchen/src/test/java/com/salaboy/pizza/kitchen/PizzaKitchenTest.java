package com.salaboy.pizza.kitchen;

import io.dapr.testcontainers.DaprContainer;
import io.restassured.RestAssured;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.dapr.client.domain.CloudEvent;
import com.salaboy.pizza.kitchen.PizzaKitchen.Event;
import com.salaboy.pizza.kitchen.PizzaKitchen.EventType;
import com.salaboy.pizza.kitchen.PizzaKitchen.Order;
import com.salaboy.pizza.kitchen.PizzaKitchen.OrderItem;
import com.salaboy.pizza.kitchen.PizzaKitchen.PizzaType;
import io.restassured.http.ContentType;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

import static io.restassured.RestAssured.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest(classes = PizzaKitchenAppTest.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"tests.mocks=true"})
@Import(DaprTestContainersConfig.class)
class PizzaKitchenTest {

  @Autowired
  private SubscriptionsRestController subscriptionsRestController;

  @Autowired
  private DaprContainer daprContainer;

  private static final String SUBSCRIPTION_MESSAGE_PATTERN = ".*consumer/broker/1 added subscription to topic.*";

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8081;
    // Ensure the subscriptions are registered
    Wait.forLogMessage(SUBSCRIPTION_MESSAGE_PATTERN, 1).waitUntilReady(daprContainer);

  }

  @Test
  void testPrepareOrderRequest() throws Exception {

    with().body(new Order(UUID.randomUUID().toString(),
                    Arrays.asList(new OrderItem(PizzaType.pepperoni, 1)),
                    new Date()))
            .contentType(ContentType.JSON)
            .when()
            .request("PUT", "/prepare")
            .then().assertThat().statusCode(200);

    try {
      await().atMost(10, TimeUnit.SECONDS)
              .pollDelay(500, TimeUnit.MILLISECONDS)
              .pollInterval(500, TimeUnit.MILLISECONDS)
              .until(() -> {
                List<CloudEvent<PizzaKitchen.Event>> events = subscriptionsRestController.getAllEvents();
                System.out.println("Delivery Events so far: " + events.size());
                if (events.size() == 2) {;
                  assertEquals("The content of the cloud event should be the in preparation event", EventType.ORDER_IN_PREPARATION, events.get(0).getData().type());
                  assertEquals("The content of the cloud event should be the ready event", EventType.ORDER_READY, events.get(1).getData().type());

                  return true;

                }
                return false;
              });
    } catch (ConditionTimeoutException timeoutException) {

      fail("The expected Order was not received/processed in expected delay");
    }


  }

}
