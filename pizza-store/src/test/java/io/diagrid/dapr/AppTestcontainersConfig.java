package io.diagrid.dapr;

import com.redis.testcontainers.RedisContainer;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.junit.runner.Description;

import java.util.List;
import java.util.Map;

import org.junit.runners.model.Statement;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.integrations.testcontainers.WireMockContainer;

@TestConfiguration(proxyBeanMethods = false)
public class AppTestcontainersConfig {

  @Bean
  RestTemplate restTemplate(){
    return new RestTemplate();
  }

//  @Bean
//  public RedisContainer redisContainer(Network daprNetwork){
//    return new RedisContainer(DockerImageName.parse("redis/redis-stack"))
//            .withExposedPorts(6379)
//            .withNetworkAliases("redis")
//            .withNetwork(daprNetwork);
//
//  }

  @Bean
  WireMockContainer wireMockContainer(DynamicPropertyRegistry properties) {

    var container = new WireMockContainer("wiremock/wiremock:3.1.0")
            .withMappingFromResource("kitchen", "kitchen-service-stubs.json");

    properties.add("dapr-http.base-url", container::getBaseUrl);
    return container;
  }

  @Bean
  public Network getNetwork() {
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

    List<com.github.dockerjava.api.model.Network> networks = DockerClientFactory.instance().client().listNetworksCmd().withNameFilter("dapr-network").exec();
    if (networks.isEmpty()) {
      Network.builder()
              .createNetworkCmdModifier(cmd -> cmd.withName("dapr-network"))
              .build().getId();
      return defaultDaprNetwork;
    } else {
      return defaultDaprNetwork;
    }
  }

  @Bean
  @ServiceConnection
  public DaprContainer daprContainer(Network daprNetwork){
    return new DaprContainer("daprio/daprd:1.14.4")
            .withAppName("pizza-store-app")
            .withNetwork(daprNetwork)
            .withComponent(new Component("statestore", "state.in-memory", "v1",
                    Map.of("actorStateStore", "true" )))
            .withDaprLogLevel(DaprLogLevel.DEBUG)
            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
            .withAppPort(8080)
            .withAppChannelAddress("host.testcontainers.internal");
            //.dependsOn(redis);
  }


}
