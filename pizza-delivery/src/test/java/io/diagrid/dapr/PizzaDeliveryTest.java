package io.diagrid.dapr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.dapr.client.domain.CloudEvent;
import io.diagrid.dapr.PizzaDelivery.Event;
import io.diagrid.dapr.PizzaDelivery.EventType;
import io.diagrid.dapr.PizzaDelivery.Order;
import io.diagrid.dapr.PizzaDelivery.OrderItem;
import io.diagrid.dapr.PizzaDelivery.PizzaType;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.*;

@SpringBootTest(classes=PizzaDeliveryAppTest.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
public class PizzaDeliveryTest {

    @Autowired
    private SubscriptionsRestController subscriptionsRestController;

    @Test
    public void testDelivery() throws Exception {
        with().body(new Order(UUID.randomUUID().toString(), 
                                Arrays.asList(new OrderItem(PizzaType.pepperoni, 1)), 
                                new Date()))
                                .contentType(ContentType.JSON)
        .when()
        .request("PUT", "/deliver")
        .then().assertThat().statusCode(200);

        // Wait for the event to arrive
        Thread.sleep(10000);

        List<CloudEvent<Event>> events = subscriptionsRestController.getAllEvents();
        assertEquals("Four published event are expected",4, events.size());
        assertEquals("The content of the cloud event should be the order-out-on-its-way event", EventType.ORDER_ON_ITS_WAY, events.get(0).getData().type());
        assertEquals("The content of the cloud event should be the order-out-on-its-way event", EventType.ORDER_ON_ITS_WAY, events.get(1).getData().type());
        assertEquals("The content of the cloud event should be the order-out-on-its-way event", EventType.ORDER_ON_ITS_WAY, events.get(2).getData().type());
        assertEquals("The content of the cloud event should be the order-completed event", EventType.ORDER_COMPLETED, events.get(3).getData().type());

      
    }

}
