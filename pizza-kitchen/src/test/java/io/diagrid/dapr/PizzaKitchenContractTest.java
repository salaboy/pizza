package io.diagrid.dapr;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.domain.CloudEvent;
import io.diagrid.dapr.PizzaKitchen.Event;
import io.diagrid.dapr.PizzaKitchen.EventType;
import io.diagrid.dapr.PizzaKitchen.Order;
import io.diagrid.dapr.PizzaKitchen.OrderItem;
import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.github.microcks.testcontainers.model.EventMessage;
import io.github.microcks.testcontainers.model.TestRequest;
import io.github.microcks.testcontainers.model.TestResult;
import io.github.microcks.testcontainers.model.TestRunnerType;
import io.github.microcks.testcontainers.model.UnidirectionalEvent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes=PizzaKitchenAppTest.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(DaprTestContainersConfig.class)
class PizzaKitchenContractTest {

    @Autowired
    MicrocksContainersEnsemble microcksEnsemble;

    @Autowired
    PizzaKitchen service;

    @Test
    void testEventIsPublishedOnKafkaAndIsConformantToSpec() {
        // Prepare a Microcks test.
        TestRequest kafkaTest = new TestRequest.Builder()
            .serviceId("Pizza Kitchen Events:1.0.0")
            .filteredOperations(List.of("RECEIVE receivePreparationEvents"))
            .runnerType(TestRunnerType.ASYNC_API_SCHEMA.name())
            .testEndpoint("kafka://kafka:19092/topic")
            .timeout(Duration.ofSeconds(3))
            .build();

        // Prepare an application Event.
        Event event = new Event(EventType.ORDER_IN_PREPARATION,
             new Order("123-456-789",
                  List.of(new OrderItem(PizzaKitchen.PizzaType.pepperoni, 1)),
                  new Date()),
             "kitchen",
             "The order is now in the kitchen.");

        try {
            // Launch the Microcks test and wait a bit to be sure it actually connects to Kafka.
            CompletableFuture<TestResult> testResultFuture = microcksEnsemble.getMicrocksContainer().testEndpointAsync(kafkaTest);
            TimeUnit.MILLISECONDS.sleep(750L);

            // Invoke the application to emit an order event.
            service.emitEvent(event);

            // Get the Microcks test result.
            TestResult testResult = testResultFuture.get();

            //System.err.println(microcksEnsemble.getAsyncMinionContainer().getLogs());
            //ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
            //System.out.println("testResult: " + mapper.writeValueAsString(testResult));

            // Check success and that we read 1 valid message on the topic.
            assertTrue(testResult.isSuccess());
            assertFalse(testResult.getTestCaseResults().isEmpty());
            assertEquals(1, testResult.getTestCaseResults().get(0).getTestStepResults().size());

            // Check the content of the emitted event, read from Kafka topic.
            List<UnidirectionalEvent> events = microcksEnsemble.getMicrocksContainer()
                  .getEventMessagesForTestCase(testResult, "RECEIVE receivePreparationEvents");

            assertEquals(1, events.size());

            EventMessage message = events.get(0).getEventMessage();
            Map<String, Object> messageMap = new ObjectMapper().readValue(message.getContent(), new TypeReference<>() {});

            // Properties from the cloud event message should match the application config and the event.
            assertEquals("1.0", messageMap.get("specversion"));
            assertEquals("local-dapr-app", messageMap.get("source"));
            assertEquals("com.dapr.event.sent", messageMap.get("type"));

            Map<String, Object> eventMap = (Map<String, Object>) messageMap.get("data");
            assertEquals("kitchen", eventMap.get("service"));
            assertEquals("The order is now in the kitchen.", eventMap.get("message"));

            // You can also try to deserialize the message content to a CloudEvent object.
            // We have to ignore the failure on unknown expiration time property.
            CloudEvent<Event> cloudEvent = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                  .readValue(message.getContent(), new TypeReference<CloudEvent<Event>>() {});
            assertEquals("1.0", cloudEvent.getSpecversion());
            assertEquals("local-dapr-app", cloudEvent.getSource());
            assertEquals("com.dapr.event.sent", cloudEvent.getType());
            assertEquals("kitchen", cloudEvent.getData().service());
            assertEquals("The order is now in the kitchen.", cloudEvent.getData().message());
        } catch (Exception e) {
            fail("No exception should be thrown when testing Kafka publication", e);
        }
    }

    @Test
    void testDeliverEndpointIsConformantToSpec() throws Exception {
        // Prepare a Microcks test.
        TestRequest openAPITest = new TestRequest.Builder()
              .serviceId("Pizza Kitchen API:1.0.0")
              .runnerType(TestRunnerType.OPEN_API_SCHEMA.name())
              .testEndpoint("http://host.testcontainers.internal:8080")
              .timeout(Duration.ofSeconds(2))
              .build();

        TestResult testResult = microcksEnsemble.getMicrocksContainer().testEndpoint(openAPITest);

        // You may inspect complete response object with following:
        //ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(testResult));

        assertTrue(testResult.isSuccess());
        // We tested 1 operation (PUT /prepare).
        assertEquals(1, testResult.getTestCaseResults().size());
        // We tested with 2 samples (salaboy and lbroudoux).
        assertEquals(2, testResult.getTestCaseResults().get(0).getTestStepResults().size());

        // We should wait here to avoid in-flight messages to be seen by other tests.
        // 20 seconcs because of PizzaKitchen implemenetation (5000 + 15000).
        TimeUnit.SECONDS.sleep(20L);
    }

    @Test
    void testCompleteFlowAndBusinessLogicIsConformantToSpecs() throws Exception {
        // Prepare a Microcks test for event production.
        TestRequest kafkaTest = new TestRequest.Builder()
              .serviceId("Pizza Kitchen Events:1.0.0")
              .filteredOperations(List.of("RECEIVE receivePreparationEvents"))
              .runnerType(TestRunnerType.ASYNC_API_SCHEMA.name())
              .testEndpoint("kafka://kafka:19092/topic")
              .timeout(Duration.ofSeconds(22))
              .build();

        // Prepare a Microcks test for event trigerring endpoint.
        TestRequest openAPITest = new TestRequest.Builder()
               .serviceId("Pizza Kitchen API:1.0.0")
               .runnerType(TestRunnerType.OPEN_API_SCHEMA.name())
               .testEndpoint("http://host.testcontainers.internal:8080")
               .timeout(Duration.ofSeconds(2))
               .build();

        try {
            // Launch the Microcks test and wait a bit to be sure it actually connects to Kafka.
            CompletableFuture<TestResult> testResultFuture = microcksEnsemble.getMicrocksContainer().testEndpointAsync(kafkaTest);
            TimeUnit.MILLISECONDS.sleep(750L);

            // Invoke the application to emit an order event.
            TestResult openAPIResult = microcksEnsemble.getMicrocksContainer().testEndpoint(openAPITest);

            // You may inspect complete response object with following:
            //ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
            //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(openAPIResult));

            // Check this first interaction is ok.
            assertTrue(openAPIResult.isSuccess());

            // Get the Microcks test result.
            TestResult kafkaResult = testResultFuture.get();

            // You may inspect complete response object with following:
            //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(kafkaResult));

            // Check success and that we read 8 valid message on the topic.
            // 4 because 2 orders are placed (per the OpenAPI samples) and 2 messages are sent for each order.
            assertTrue(kafkaResult.isSuccess());
            assertFalse(kafkaResult.getTestCaseResults().isEmpty());
            assertEquals(4, kafkaResult.getTestCaseResults().get(0).getTestStepResults().size());

            // Check the content of the emitted event, read from Kafka topic.
            List<UnidirectionalEvent> events = microcksEnsemble.getMicrocksContainer()
                  .getEventMessagesForTestCase(kafkaResult, "RECEIVE receivePreparationEvents");

            assertEquals(4, events.size());
        } catch (Exception e) {
            fail("No exception should be thrown when testing Kafka publication", e);
        }
    }
}
