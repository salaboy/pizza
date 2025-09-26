package com.salaboy.pizza.store;

import io.github.microcks.testcontainers.Assertions;
import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.github.microcks.testcontainers.model.RequestResponsePair;
import io.github.microcks.testcontainers.model.TestRequest;
import io.github.microcks.testcontainers.model.TestResult;
import io.github.microcks.testcontainers.model.TestRunnerType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.workflows.client.DaprWorkflowClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes=PizzaStoreAppTest.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = { "tests.mocks=true" })
@Import(DaprTestContainersConfig.class)
class PizzaStoreContractTest {

   @Autowired
   MicrocksContainersEnsemble microcksEnsemble;
   @Autowired
   DaprWorkflowClient daprWorkflowClient;

   @BeforeEach
   void setup() {
      try {
         daprWorkflowClient.terminateWorkflow("abc-def-ghi", "Test setup cleanup");
         daprWorkflowClient.terminateWorkflow("123-456-789", "Test setup cleanup");
         daprWorkflowClient.purgeInstance("abc-def-ghi");
         daprWorkflowClient.purgeInstance("123-456-789");
      } catch (Throwable t) {
         // Exception is ok, workflow may not exist.
      }
   }

   @Test
   void testPlaceOrderEndpointIsConformantToSpec() throws Exception {
      // Prepare a Microcks test.
      TestRequest openAPITest = new TestRequest.Builder()
            .serviceId("Pizza Store API:1.0.0")
            .runnerType(TestRunnerType.OPEN_API_SCHEMA.name())
            .testEndpoint("http://host.testcontainers.internal:8080")
            .timeout(Duration.ofSeconds(4))
            .build();

      TestResult testResult = microcksEnsemble.getMicrocksContainer().testEndpoint(openAPITest);

      Assertions.assertSuccess(testResult);
      // We tested 1 operation (POST /order).
      assertEquals(1, testResult.getTestCaseResults().size());
      // We tested with 2 samples (salaboy and lbroudoux).
      assertEquals(2, testResult.getTestCaseResults().get(0).getTestStepResults().size());
   }
}
