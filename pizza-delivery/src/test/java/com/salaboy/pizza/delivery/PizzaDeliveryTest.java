package com.salaboy.pizza.delivery;

import io.dapr.testcontainers.DaprContainer;
import io.restassured.RestAssured;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.dapr.client.domain.CloudEvent;
import io.restassured.http.ContentType;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

import static io.restassured.RestAssured.*;

@SpringBootTest(classes = PizzaDeliveryAppTest.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"tests.mocks=true"})
@Import(DaprTestContainersConfig.class)
class PizzaDeliveryTest {

  @Autowired
  private SubscriptionsRestController subscriptionsRestController;

  @Autowired
  private DaprContainer daprContainer;

  private static final String SUBSCRIPTION_MESSAGE_PATTERN = ".*consumer/broker/1 added subscription to topic.*";

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8082;
    // Ensure the subscriptions are registered
    Wait.forLogMessage(SUBSCRIPTION_MESSAGE_PATTERN, 1).waitUntilReady(daprContainer);

  }

  @Test
  void testDelivery() throws Exception {
    with().body(new PizzaDelivery.Order(UUID.randomUUID().toString(),
                    Arrays.asList(new PizzaDelivery.OrderItem(PizzaDelivery.PizzaType.pepperoni, 1)),
                    new Date()))
            .contentType(ContentType.JSON)
            .when()
            .request("PUT", "/deliver")
            .then().assertThat().statusCode(200);


    try {
      await().atMost(20, TimeUnit.SECONDS)
              .pollDelay(500, TimeUnit.MILLISECONDS)
              .pollInterval(500, TimeUnit.MILLISECONDS)
              .until(() -> {
                List<CloudEvent<PizzaDelivery.Event>> events = subscriptionsRestController.getAllEvents();
                System.out.println("Delivery Events so far: " + events.size());
                if (events.size() == 4) {
                  Assert.assertEquals("The content of the cloud event should be the order-out-on-its-way event", PizzaDelivery.EventType.ORDER_ON_ITS_WAY, events.get(0).getData().type());
                  Assert.assertEquals("The content of the cloud event should be the order-out-on-its-way event", PizzaDelivery.EventType.ORDER_ON_ITS_WAY, events.get(1).getData().type());
                  Assert.assertEquals("The content of the cloud event should be the order-out-on-its-way event", PizzaDelivery.EventType.ORDER_ON_ITS_WAY, events.get(2).getData().type());
                  Assert.assertEquals("The content of the cloud event should be the order-completed event", PizzaDelivery.EventType.ORDER_COMPLETED, events.get(3).getData().type());
                  return true;

                }
                return false;
              });
    } catch (ConditionTimeoutException timeoutException) {

      fail("The expected Order was not received/processed in expected delay");
    }

  }

}
