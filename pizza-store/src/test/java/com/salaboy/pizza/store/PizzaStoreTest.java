package com.salaboy.pizza.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.salaboy.pizza.store.PizzaStore.Customer;
import com.salaboy.pizza.store.PizzaStore.Order;
import com.salaboy.pizza.store.PizzaStore.OrderItem;
import com.salaboy.pizza.store.PizzaStore.PizzaType;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static io.restassured.RestAssured.with;
import java.util.Arrays;


@SpringBootTest(classes=PizzaStoreAppTest.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = { "tests.mocks=true" })
@Import(DaprTestContainersConfig.class)
class PizzaStoreTest {

    
    @Test
    void testPlaceOrder() throws Exception {
        
       with().body(new Order(new Customer("salaboy", "salaboy@mail.com"), 
                                Arrays.asList(new OrderItem(PizzaType.pepperoni, 1))))
                                .contentType(ContentType.JSON)
        .when()
        .request("POST", "/order")
        .then().assertThat().statusCode(200);


    }

}
