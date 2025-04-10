package com.salaboy.pizza.store;

import io.dapr.testcontainers.*;
import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.github.microcks.testcontainers.connection.KafkaConnection;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class DaprTestContainersConfig {

    private KafkaContainer kafkaContainer;
    private MicrocksContainersEnsemble ensemble;
    private DaprContainer daprContainer;
    private DaprContainer daprContainerKitchen;


    @Bean
    public Network daprNetwork(Environment env) {
        boolean reuse = env.getProperty("reuse", Boolean.class, false);
        if (reuse) {
            Network defaultDaprNetwork = new Network() {
                @Override
                public String getId() {
                    return "dapr-network";
                }

                @Override
                public void close() {

                }

                @Override
                public Statement apply(Statement base, Description description) {
                    return null;
                }
            };

            List<com.github.dockerjava.api.model.Network> networks = DockerClientFactory.instance().client().listNetworksCmd()
                    .withNameFilter("dapr-network").exec();
            if (networks.isEmpty()) {
                Network.builder().createNetworkCmdModifier(cmd -> cmd.withName("dapr-network")).build().getId();
                return defaultDaprNetwork;
            } else {
                return defaultDaprNetwork;
            }
        } else {
            return Network.newNetwork();
        }
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer(Environment env, Network daprNetwork) {
        boolean reuse = env.getProperty("reuse", Boolean.class, false);
        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .withNetwork(daprNetwork)
            .withNetworkAliases("kafka")
            .withListener(() -> "kafka:19092").withReuse(reuse);
        return kafkaContainer;
    }

    @Bean
    @ConditionalOnProperty(prefix = "tests", name = "mocks", havingValue = "true")
    MicrocksContainersEnsemble microcksEnsemble(Network network) {
        ensemble = new MicrocksContainersEnsemble(network, "quay.io/microcks/microcks-uber:1.11.0-native")
            .withAsyncFeature()
            .withAccessToHost(true)
            .withKafkaConnection(new KafkaConnection("kafka:19092"))
            .withMainArtifacts(
                  "store-openapi.yaml", "store-asyncapi.yaml",
                  "third-parties/kitchen-openapi.yaml", "third-parties/delivery-openapi.yaml")
            .withAsyncDependsOn(kafkaContainer);
        // Async events can pollute the experience in spring-boot:test-run,
        // so we only add them if we are running in pure JUnit tests mode.
        boolean isSpringTestRunExecution =  Arrays.stream(Thread.currentThread().getStackTrace())
              .anyMatch(element -> element.getClassName().equals("com.salaboy.pizza.store.PizzaStoreAppTest"));
        if (!isSpringTestRunExecution) {
            ensemble.withMainArtifacts("third-parties/kitchen-asyncapi.yaml", "third-parties/delivery-asyncapi.yaml");
        }
        return ensemble;
    }

    @Bean
    @ServiceConnection
    DaprContainer daprContainer(KafkaContainer kafkaContainer, Environment env, Network daprNetwork, @Nullable MicrocksContainersEnsemble ensemble) {
        boolean reuse = env.getProperty("reuse", Boolean.class, false);
        daprContainer = new DaprContainer("daprio/daprd")
              .withAppName("pizza-store")
              .withAppPort(8080)
              .withNetwork(daprNetwork)
              .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of()))
              .withComponent(new Component("pubsub", "pubsub.kafka", "v1",
                    Map.of(
                          "brokers", "kafka:19092",
                          "authType", "none"
                    )))
              .withSubscription(new Subscription(
                    "pizza-store-subscription",
                    "pubsub", "topic", "/events"))
              .withAppChannelAddress("host.testcontainers.internal")
              .withDaprLogLevel(DaprLogLevel.DEBUG)
                .withReusablePlacement(reuse)
              .dependsOn(kafkaContainer);
        if (ensemble != null){
            daprContainer
                    .withSubscription(new Subscription(
                    "pizza-kitchen-subscription", "pubsub",
                    ensemble.getAsyncMinionContainer().getKafkaMockTopic("Pizza Kitchen Events", "1.0.0", "RECEIVE receivePreparationEvents"),
                    "/events"))
                    .withSubscription(new Subscription(
                            "pizza-delivery-subscription", "pubsub",
                            ensemble.getAsyncMinionContainer().getKafkaMockTopic("Pizza Delivery Events", "1.0.0", "RECEIVE receiveDeliveryEvents"),
                            "/events"));
        }
        org.testcontainers.Testcontainers.exposeHostPorts(8080);
        return daprContainer;
    }

    @Bean
    @DependsOn("microcksEnsemble")
    @ConditionalOnProperty(prefix = "tests", name = "mocks", havingValue = "true")
    DaprContainer daprContainerKitchen(MicrocksContainersEnsemble ensemble, Network daprNetwork) {
        AppHttpPipeline appHttpPipeline = new AppHttpPipeline(Collections
                .singletonList(new ListEntry("routes", "middleware.http.routeralias")));
        Map<String, String> routerMetadata = Collections.singletonMap("routes",
                "{ " +
                    "\"/prepare\": \"/rest/Pizza+Kitchen+API/1.0.0/prepare\""+
                "}");

      daprContainerKitchen = new DaprContainer("daprio/daprd")
                  .withAppName("kitchen-service")
                  .withNetwork(daprNetwork)
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
    @ConditionalOnProperty(prefix = "tests", name = "mocks", havingValue = "true")
    DaprContainer daprContainerDelivery(MicrocksContainersEnsemble ensemble, Network network) {
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
