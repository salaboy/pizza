package io.diagrid.dapr;

import java.util.Collections;
import java.util.List;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.dockerfile.statement.Statement;
import org.testcontainers.junit.jupiter.Container;

import io.diagrid.dapr.DaprContainer.Component;
import io.diagrid.dapr.QuotedBoolean;

public interface DaprLocalWithWorkflows {

    // Network daprNetwork = getNetwork();

    // static Network getNetwork() {
    //     Network defaultDaprNetwork = new Network() {
    //         @Override
    //         public String getId() {
    //             return "dapr";
    //         }

    //         @Override
    //         public void close() {

    //         }

    //         @Override
    //         public Statement apply(Statement base, Description description) {
    //             return null;
    //         }
    //     };

    //     List<com.github.dockerjava.api.model.Network> networks = DockerClientFactory.instance().client().listNetworksCmd().withNameFilter("dapr").exec();
    //     if (networks.isEmpty()) {
    //         Network.builder()
    //                 .createNetworkCmdModifier(cmd -> cmd.withName("dapr"))
    //                 .build().getId();
    //         return defaultDaprNetwork;
    //     } else {
    //         return defaultDaprNetwork;
    //     }
    // }

    @Container
    DaprContainer dapr = new DaprContainer("daprio/daprd")
            .withAppName("local-dapr-app")
            .withComponent(new Component("kvstore", "state.in-memory", Collections.singletonMap("actorStateStore", new QuotedBoolean("true")) ))
            .withComponent(new Component("pubsub", "pubsub.in-memory", Collections.emptyMap()))
            .withAppPort(8080)
            .withAppChannelAddress("host.testcontainers.internal");

    // @Container
    // GenericContainer<?> daprPlacement = new GenericContainer<>("daprio/dapr")
    //         .withCommand("./placement", "-port", "50006")
    //         .withExposedPorts(50006) // for wait
    //         .withNetwork(daprNetwork)
    //         .withNetworkAliases("placement"); 

    @DynamicPropertySource
    static void daprProperties(DynamicPropertyRegistry registry) {
        Testcontainers.exposeHostPorts(8080);
        System.setProperty("dapr.grpc.port", Integer.toString(dapr.getGRPCPort()));
        System.setProperty("dapr.http.port", Integer.toString(dapr.getHTTPPort()));
    }
    
}
