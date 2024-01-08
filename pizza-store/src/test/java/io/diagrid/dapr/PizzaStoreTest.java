package io.diagrid.dapr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.diagrid.dapr.model.Customer;
import io.diagrid.dapr.model.OrderPayload;
import io.diagrid.dapr.model.OrderItem;
import io.diagrid.dapr.model.PizzaType;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import static io.restassured.RestAssured.with;
import java.util.Arrays;


@SpringBootTest(classes=PizzaStoreAppTest.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
public class PizzaStoreTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
    }
    
    @Test
    public void testPlaceOrder() throws Exception {
        
       with().body(new OrderPayload(new Customer("salaboy", "salaboy@mail.com"), 
                                Arrays.asList(new OrderItem(PizzaType.pepperoni, 1)),""))
                                .contentType(ContentType.JSON)
        .when()
        .request("POST", "/order")
        .then().assertThat().statusCode(200);
        

        
    }

}
