package com.salaboy.pizza.store;

import io.dapr.testcontainers.*;
import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.github.microcks.testcontainers.connection.KafkaConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class DaprTestContainersConfig {
    private static Network network = Network.newNetwork();
    private KafkaContainer kafkaContainer;
    private MicrocksContainersEnsemble ensemble;
    private DaprContainer daprContainer;
    private DaprContainer daprContainerKitchen;

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .withNetwork(network)
            .withNetworkAliases("kafka")
            .withListener(() -> "kafka:19092");
        return kafkaContainer;
    }

    @Bean
    MicrocksContainersEnsemble microcksEnsemble() {
        ensemble = new MicrocksContainersEnsemble(network, "quay.io/microcks/microcks-uber:1.11.0-native")
            .withAsyncFeature()
            .withAccessToHost(true)
            .withKafkaConnection(new KafkaConnection("kafka:19092"))
            .withMainArtifacts(
                  "store-openapi.yaml", "store-asyncapi.yaml",
                  "third-parties/kitchen-openapi.yaml", "third-parties/delivery-openapi.yaml",
                  "third-parties/kitchen-asyncapi.yaml", "third-parties/delivery-asyncapi.yaml")
            .withAsyncDependsOn(kafkaContainer);
        return ensemble;
    }

    @Bean
    @ServiceConnection
    DaprContainer daprContainer(KafkaContainer kafkaContainer, MicrocksContainersEnsemble ensemble) {
        daprContainer = new DaprContainer("daprio/daprd")
              .withAppName("pizza-store")
              .withAppPort(8080)
              .withNetwork(network)
              .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of()))
              .withComponent(new Component("pubsub", "pubsub.kafka", "v1",
                    Map.of(
                          "brokers", "kafka:19092",
                          "authType", "none"
                    )))
              .withSubscription(new Subscription(
                    "pizza-store-subscription",
                    "pubsub", "topic", "/events"))
              .withSubscription(new Subscription(
                    "pizza-kitchen-subscription", "pubsub",
                    ensemble.getAsyncMinionContainer().getKafkaMockTopic("Pizza Kitchen Events", "1.0.0", "RECEIVE receivePreparationEvents"),
                    "/events"))
              .withSubscription(new Subscription(
                    "pizza-delivery-subscription", "pubsub",
                    ensemble.getAsyncMinionContainer().getKafkaMockTopic("Pizza Delivery Events", "1.0.0", "RECEIVE receiveDeliveryEvents"),
                    "/events"))
              .withAppChannelAddress("host.testcontainers.internal")
              .withDaprLogLevel(DaprLogLevel.DEBUG)
              .dependsOn(kafkaContainer);
        org.testcontainers.Testcontainers.exposeHostPorts(8080);
        return daprContainer;
    }

    @Bean
    @DependsOn("microcksEnsemble")
    DaprContainer daprContainerKitchen(MicrocksContainersEnsemble ensemble) {
        AppHttpPipeline appHttpPipeline = new AppHttpPipeline(Collections
                .singletonList(new ListEntry("routes", "middleware.http.routeralias")));
        Map<String, String> routerMetadata = Collections.singletonMap("routes",
                "{ " +
                    "\"/prepare\": \"/rest/Pizza+Kitchen+API/1.0.0/prepare\""+
                "}");

      daprContainerKitchen = new DaprContainer("daprio/daprd")
                  .withAppName("kitchen-service")
                  .withNetwork(network)
                  .withComponent(new Component("routes", "middleware.http.routeralias", "v1", routerMetadata))
                  .withConfiguration(new Configuration("app-middleware", null, appHttpPipeline))
                  .withAppPort(8080)
                  .withAppChannelAddress("microcks")
                  .withDaprLogLevel(DaprLogLevel.DEBUG)
                  .dependsOn(ensemble);
      return daprContainerKitchen;
    }

    @Bean
    @DependsOn("microcksEnsemble")
    DaprContainer daprContainerDelivery(MicrocksContainersEnsemble ensemble) {
        AppHttpPipeline appHttpPipeline = new AppHttpPipeline(Collections
                .singletonList(new ListEntry("routes", "middleware.http.routeralias")));
        Map<String, String> routerMetadata = Collections.singletonMap("routes",
                "{ " +
                        "\"/deliver\": \"/rest/Pizza+Delivery+API/1.0.0/deliver\""+
                        "}");

        daprContainerKitchen = new DaprContainer("daprio/daprd")
                .withAppName("delivery-service")
                .withNetwork(network)
                .withComponent(new Component("routes", "middleware.http.routeralias", "v1", routerMetadata))
                .withConfiguration(new Configuration("app-middleware", null, appHttpPipeline))
                .withAppPort(8080)
                .withAppChannelAddress("microcks")
                .withDaprLogLevel(DaprLogLevel.DEBUG)
                .dependsOn(ensemble);
        return daprContainerKitchen;
    }

}
