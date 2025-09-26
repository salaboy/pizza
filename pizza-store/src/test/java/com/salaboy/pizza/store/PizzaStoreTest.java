package com.salaboy.pizza.store;

import com.salaboy.pizza.store.model.Customer;
import com.salaboy.pizza.store.model.OrderItem;
import com.salaboy.pizza.store.model.OrderPayload;
import com.salaboy.pizza.store.model.PizzaType;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import io.restassured.http.ContentType;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static io.restassured.RestAssured.with;

import java.util.Arrays;


@SpringBootTest(classes = PizzaStoreAppTest.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"tests.mocks=true"})
@Import(DaprTestContainersConfig.class)
class PizzaStoreTest {


  @BeforeAll
  public static void setup() {
    org.testcontainers.Testcontainers.exposeHostPorts(8080);
  }

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8080;
  }

  @Test
  void testPlaceOrder() throws Exception {

    with().body(new OrderPayload(new Customer("salaboy", "salaboy@mail.com"),
                    Arrays.asList(new OrderItem("pizza",PizzaType.pepperoni.name(), 1))))
            .contentType(ContentType.JSON)
            .when()
            .request("POST", "/order")
            .then().assertThat().statusCode(200);


  }

}
