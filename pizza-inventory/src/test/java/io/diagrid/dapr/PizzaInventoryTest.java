package io.diagrid.dapr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.hamcrest.Matchers.*;

import io.diagrid.dapr.PizzaInventory.InventoryRequest;
import io.diagrid.dapr.PizzaInventory.PizzaType;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.*;

@SpringBootTest(classes=PizzaInventoryAppTest.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class PizzaInventoryTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
    }
    
    @Test
    public void testInventoryRequest() throws Exception {
        

        given()
        .param("pizzaType",PizzaType.pepperoni)
        .get("/inventory")
        .then()
        .assertThat().body("pizzaType",equalTo(PizzaType.pepperoni.toString()))
        .assertThat().body("stockCount",equalTo(10))
        .statusCode(200)
        .log().body(true);

       with().body(new InventoryRequest(PizzaType.pepperoni, 1))
                                .contentType(ContentType.JSON)

        .when()
        .put("/inventory")
        .then()
        .assertThat().body("pizzaType",equalTo(PizzaType.pepperoni.toString()))
        .assertThat().body("stockCount",equalTo(9))
        .statusCode(200); 
    }

}
