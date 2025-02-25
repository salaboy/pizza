package io.diagrid.dapr;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.HttpEndpoint;
import io.dapr.testcontainers.Subscription;
import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.github.microcks.testcontainers.connection.KafkaConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class DaprTestContainersConfig {
    private static Network network = Network.newNetwork();
    private KafkaContainer kafkaContainer;
    private MicrocksContainersEnsemble ensemble;
    private DaprContainer daprContainer;

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
    MicrocksContainersEnsemble microcksEnsemble(DynamicPropertyRegistry registry) {
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
    DaprContainer daprContainer(DynamicPropertyRegistry registry, KafkaContainer kafkaContainer, MicrocksContainersEnsemble ensemble) {
        daprContainer = new DaprContainer("daprio/daprd")
              .withAppName("local-dapr-app")
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
              .withHttpEndpoint(new HttpEndpoint("kitchen-service",
                    "http://microcks:8080/rest/Pizza+Kitchen+API/1.0.0"))
              .withHttpEndpoint(new HttpEndpoint("delivery-service",
                    "http://microcks:8080/rest/Pizza+Delivery+API/1.0.0"))
              .withAppChannelAddress("host.testcontainers.internal")
              .withDaprLogLevel(DaprLogLevel.DEBUG)
              .dependsOn(kafkaContainer);

        org.testcontainers.Testcontainers.exposeHostPorts(8080);

        registry.add("DAPR_HTTP_ENDPOINT", daprContainer::getHttpEndpoint);

        return daprContainer;
    }
}
