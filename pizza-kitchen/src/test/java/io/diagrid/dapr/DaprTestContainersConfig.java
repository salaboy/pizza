package io.diagrid.dapr;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
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
    private DaprContainer daprContainer;

    @Bean
    @ServiceConnection
    DaprContainer daprContainer(DynamicPropertyRegistry registry, KafkaContainer kafkaContainer) {
        daprContainer = new DaprContainer("daprio/daprd")
            .withAppName("local-dapr-app")
            .withAppPort(8080)
            .withNetwork(network)
            .withComponent(new Component("pubsub", "pubsub.kafka", "v1",
                  Map.of(
                        "brokers", "kafka:19092",
                        "authType", "none"
                  )))
            .withSubscription(new Subscription(
                  "pizza-store-subscription",
                  "pubsub", "topic", "/events"))
            .withAppChannelAddress("host.testcontainers.internal")
            .dependsOn(kafkaContainer);

        org.testcontainers.Testcontainers.exposeHostPorts(8080);
        return daprContainer;
    }

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
        MicrocksContainersEnsemble ensemble = new MicrocksContainersEnsemble(network, "quay.io/microcks/microcks-uber:1.11.0-native")
            .withAsyncFeature()
            .withAccessToHost(true)
            .withKafkaConnection(new KafkaConnection("kafka:19092"))
            .withMainArtifacts("kitchen-openapi.yaml", "kitchen-asyncapi.yaml")
            .withAsyncDependsOn(kafkaContainer);
        return ensemble;
    }
}
