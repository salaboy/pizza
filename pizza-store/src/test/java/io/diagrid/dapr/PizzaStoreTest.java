package io.diagrid.dapr;

import io.dapr.springboot.DaprAutoConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.diagrid.dapr.model.Customer;
import io.diagrid.dapr.model.OrderPayload;
import io.diagrid.dapr.model.OrderItem;
import io.diagrid.dapr.model.PizzaType;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import static io.restassured.RestAssured.with;
import java.util.Arrays;


@SpringBootTest(classes= {PizzaStoreAppTest.class, AppTestcontainersConfig.class, DaprAutoConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class PizzaStoreTest {

    @LocalServerPort
    private int port;

    @BeforeAll
    public static void setup(){
        org.testcontainers.Testcontainers.exposeHostPorts(8080);
    }

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost:" + 8080;
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
