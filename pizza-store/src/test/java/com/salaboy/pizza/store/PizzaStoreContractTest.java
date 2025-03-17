package com.salaboy.pizza.store;

import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.github.microcks.testcontainers.model.TestRequest;
import io.github.microcks.testcontainers.model.TestResult;
import io.github.microcks.testcontainers.model.TestRunnerType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes=PizzaStoreAppTest.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(DaprTestContainersConfig.class)
class PizzaStoreContractTest {

   @Autowired
   MicrocksContainersEnsemble microcksEnsemble;

   @Test
   void testPlaceOrderEndpointIsConformantToSpec() throws Exception {
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
   }
}
