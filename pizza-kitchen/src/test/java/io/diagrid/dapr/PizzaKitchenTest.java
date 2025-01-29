package io.diagrid.dapr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.dapr.client.domain.CloudEvent;
import io.diagrid.dapr.PizzaKitchen.Event;
import io.diagrid.dapr.PizzaKitchen.EventType;
import io.diagrid.dapr.PizzaKitchen.Order;
import io.diagrid.dapr.PizzaKitchen.OrderItem;
import io.diagrid.dapr.PizzaKitchen.PizzaType;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.*;

@SpringBootTest(classes=PizzaKitchenAppTest.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(DaprTestContainersConfig.class)
class PizzaKitchenTest {

    @Autowired
    private SubscriptionsRestController subscriptionsRestController;

    
    @Test
    void testPrepareOrderRequest() throws Exception {
        // Get initial events size - it may not be empty if we're running in a test suite.
        List<CloudEvent<Event>> events = subscriptionsRestController.getAllEvents();
        int eventsSize = events.size();

        with().body(new Order(UUID.randomUUID().toString(), 
                                Arrays.asList(new OrderItem(PizzaType.pepperoni, 1)), 
                                new Date()))
                                .contentType(ContentType.JSON)
        .when()
        .request("PUT", "/prepare")
        .then().assertThat().statusCode(200);

        // Wait for the event to arrive
        Thread.sleep(20000);

        events = subscriptionsRestController.getAllEvents();
        assertEquals("Two published event are expected",2, events.size() - eventsSize);
        assertEquals("The content of the cloud event should be the in preparation event", EventType.ORDER_IN_PREPARATION, events.get(eventsSize).getData().type());
        assertEquals("The content of the cloud event should be the ready event", EventType.ORDER_READY, events.get(eventsSize + 1).getData().type());

    }

}
