package com.salaboy.pizza.store;

import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.github.microcks.testcontainers.model.TestRequest;
import io.github.microcks.testcontainers.model.TestResult;
import io.github.microcks.testcontainers.model.TestRunnerType;

import io.dapr.client.DaprClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest(classes=PizzaStoreAppTest.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = { "tests.mocks=true" })
@Import(DaprTestContainersConfig.class)
class PizzaStoreInteractionTest {

   @Autowired
   MicrocksContainersEnsemble microcksEnsemble;

   @Autowired
   PizzaStore pizzaStore;
   @Autowired
   DaprClient daprClient;

   @Test
   void testKitchenPrepareIsCalledAfterOrderIsPlaced() throws Exception {
      long kitchenInvocations = microcksEnsemble.getMicrocksContainer()
            .getServiceInvocationsCount("Pizza Kitchen API", "1.0.0");

      // Prepare a Microcks test.
      TestRequest openAPITest = new TestRequest.Builder()
            .serviceId("Pizza Store API:1.0.0")
            .runnerType(TestRunnerType.OPEN_API_SCHEMA.name())
            .testEndpoint("http://host.testcontainers.internal:8080")
            .timeout(Duration.ofSeconds(2))
            .build();

      TestResult testResult = microcksEnsemble.getMicrocksContainer().testEndpoint(openAPITest);

      // You may inspect complete response object with following:
      //ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
      //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(testResult));

      assertTrue(testResult.isSuccess());
      // We tested 1 operation (POST /order).
      assertEquals(1, testResult.getTestCaseResults().size());
      // We tested with 2 samples (salaboy and lbroudoux).
      assertEquals(2, testResult.getTestCaseResults().get(0).getTestStepResults().size());

      // Wait a second, ensuring the order threads are done.
      TimeUnit.SECONDS.sleep(1L);

      // Check that the kitchen service has been invoked twice (for salaboy and lbroudoux)
      long newKitchenInvocations = microcksEnsemble.getMicrocksContainer()
            .getServiceInvocationsCount("Pizza Kitchen API", "1.0.0");

      //TimeUnit.SECONDS.sleep(30L);
      assertTrue(newKitchenInvocations > kitchenInvocations);
   }

   @Test
   void testDeliveryDeliverIsCalledWhenPreparationEventReceived() throws Exception {
      // Empty the store and record number of mock invocations at the start.
      daprClient.deleteState("kvstore", "orders").block();
      long deliveryInvocations = microcksEnsemble.getMicrocksContainer()
            .getServiceInvocationsCount("Pizza Delivery API", "1.0.0");

      try {
         await().atMost(4, TimeUnit.SECONDS)
               .pollDelay(400, TimeUnit.MILLISECONDS)
               .pollInterval(400, TimeUnit.MILLISECONDS)
               .until(() -> {
                  PizzaStore.Orders orders = pizzaStore.loadOrders();
                  if (orders != null) {
                     for (PizzaStore.Order order : orders.orders()) {
                        if ("123-456-789".equals(order.id())) {
                           long newDeliveryInvocations = microcksEnsemble.getMicrocksContainer()
                                 .getServiceInvocationsCount("Pizza Delivery API", "1.0.0");

                           assertTrue(newDeliveryInvocations > deliveryInvocations);
                           return true;
                        }
                     }
                  }
                  return false;
               });
      } catch (ConditionTimeoutException timeoutException) {
         fail("The expected Order was not received/processed in expected delay");
      }
   }

   /*
   // Following test cannot work at the moment as Microcks only supports raw WebSocket and not STOMP over WS.
   @Test
   void testEventIsPublishedAfterOrderIsPlaced() throws Exception {
      // Prepare a Microcks test for event production.
      TestRequest wsTest = new TestRequest.Builder()
            .serviceId("Pizza Store Events:1.0.0")
            .filteredOperations(List.of("RECEIVE receiveOrderEvents"))
            .runnerType(TestRunnerType.ASYNC_API_SCHEMA.name())
            .testEndpoint("ws://host.testcontainers.internal:8080/ws")
            .timeout(Duration.ofSeconds(4))
            .build();


      // Prepare a Microcks test for event trigerring endpoint.
      TestRequest openAPITest = new TestRequest.Builder()
            .serviceId("Pizza Store API:1.0.0")
            .runnerType(TestRunnerType.OPEN_API_SCHEMA.name())
            .testEndpoint("http://host.testcontainers.internal:8080")
            .timeout(Duration.ofSeconds(2))
            .build();

      try {
         // Launch the Microcks test and wait a bit to be sure it actually connects to Kafka.
         CompletableFuture<TestResult> testResultFuture = microcksEnsemble.getMicrocksContainer().testEndpointAsync(wsTest);
         TimeUnit.MILLISECONDS.sleep(750L);

         // Invoke the application to emit an order event.
         TestResult testResult = microcksEnsemble.getMicrocksContainer().testEndpoint(openAPITest);

         // You may inspect complete response object with following:
         //ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
         //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(testResult));

         // Check this first interaction is ok.
         assertTrue(testResult.isSuccess());

         // Get the Microcks test result.
         TestResult wsResult = testResultFuture.get();

         // You may inspect complete response object with following:
         //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(kafkaResult));

         // Check success and that we read 2 valid message on the topic.
         // Because 2 orders are placed (per the OpenAPI samples) and 1 messages are sent for each order.
         assertTrue(wsResult.isSuccess());
         assertFalse(wsResult.getTestCaseResults().isEmpty());
         assertEquals(2, wsResult.getTestCaseResults().get(0).getTestStepResults().size());

         // Check the content of the emitted event, read from Kafka topic.
         List<UnidirectionalEvent> events = microcksEnsemble.getMicrocksContainer()
               .getEventMessagesForTestCase(wsResult, "RECEIVE receiveOrderEvents");

         assertEquals(2, events.size());

      } catch (Exception e) {
         fail("No exception should be thrown when testing WebSocket publication", e);
      }
   }
   */
}
