# Cloud-Native Pizza Delivery system

This repository contains a simple example for a Pizza Store application using Kubernetes, [Dapr](https://dapr.io) and [Testcontainers](https://testcontainers.com) to enable developers with an awesome developer experience.

![](imgs/testcontainers-dapr.png)

You can run this application on any Kubernetes cluster by following the step-by-step insturctions described in this document. 

## Installation

If you don't have a Kubernetes Cluster you can [install KinD](https://kind.sigs.k8s.io/docs/user/quick-start/) to create a local cluster to run the application. 

Once you have KinD installed you can run the following command to create a local Cluster: 

```
kind create cluster
```

Then we will install [Dapr](https://dapr.io) into our fresh new cluster by running the following command: 

```
helm repo add dapr https://dapr.github.io/helm-charts/
helm repo update
helm upgrade --install dapr dapr/dapr \
--version=1.12.3 \
--namespace dapr-system \
--create-namespace \
--wait
```

## Installing infrastructure for the application

We will be using Kafka for sending messages between services: 

```
helm install kafka oci://registry-1.docker.io/bitnamicharts/kafka --version 22.1.5 --set "provisioning.topics[0].name=events-topic" --set "provisioning.topics[0].partitions=1" --set "persistence.size=1Gi" 
```

We will be using PostgreSQL as our persistent store, but before installing the PostgreSQL Chart run:

```
kubectl apply -f k8s/pizza-init-sql-cm.yaml
```

Then: 

```
helm install postgresql oci://registry-1.docker.io/bitnamicharts/postgresql --version 12.5.7 --set "image.debug=true" --set "primary.initdb.user=postgres" --set "primary.initdb.password=postgres" --set "primary.initdb.scriptsConfigMap=pizza-init-sql" --set "global.postgresql.auth.postgresPassword=postgres" --set "primary.persistence.size=1Gi"
```

## Installing Observability

Based on [official docs with Jaeger](https://docs.dapr.io/operations/observability/tracing/otel-collector/open-telemetry-collector-jaeger/)

Install Cert manager: 

```
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.3/cert-manager.yaml
```

Install Jaeger Operator (https://www.jaegertracing.io/docs/1.49/operator/): 

```
kubectl create namespace observability
kubectl create -f https://github.com/jaegertracing/jaeger-operator/releases/download/v1.49.0/jaeger-operator.yaml -n observability 

```

Then create a Jaeger Collector by applying the following resources:
```
kubectl apply -f observability/jaeger.yaml
```
And then the OpenTelemetry collector for Jaeger: 
```
kubectl apply -f observability/open-telemetry-collector-jaeger.yaml
```

Let's connect this with Dapr by applying the following Configuration (named `tracing`) resources:

```
kubectl apply -f observability/collector-config-otel.yaml
```

The last step, is to check that your Dapr applications add the annotations Dapr annotation: 

```
  dapr.io/config: "tracing"
```

To check the traces you can access the Jaeger UI by using port-forwarding: 

```
kubectl port-forward svc/simplest-query 16686
```

Access Jaeger by pointing your browser to [http://localhost:16686](http://localhost:16686)

When using interacting with the application you should be able to see the traces of each service and the System Architecture: 

![jaeger](imgs/jaeger.png)

## Installing Tracetest

```
kubectl create namespace tracetest
kubectl apply -f tracetest/tracetest.yaml -n tracetest
```

Get the application URL:

```
kubectl port-forward svc/tracetest 11633 -n tracetest
```

## Installing the Application

To install the application you only need to run the following command: 

```
kubectl apply -f k8s/
```

This install all the application services. To avoid dealing with Ingresses you can access the application by using `kubectl port-forward`, run to access the application on port `8080`: 

```
kubectl port-forward svc/pizza-store 8080:80
```

Then you can point your browser to [`http://localhost:8080`](http://localhost:8080) and you should see: 

![Pizza Store](imgs/pizza-store.png)

## Testing the Application

To test the application you will use Tracetest either from the UI or CLI.

Endpoint:

```
POST pizza-store.default.svc.cluster.local:80/order
```

Body:

```json
{
    "customer": {
      "name": "salaboy",
      "email": "salaboy@mail.com"
    },
    "items": [
      {
      "type":"pepperoni",
      "amount": 1
      }
    ]
}
```

## Building from source / changing the services

The application services are written using Java + Spring Boot. These services use the Dapr Java SDK to interact with the Dapr [PubSub](https://docs.dapr.io/getting-started/quickstarts/pubsub-quickstart/) and [Statestore](https://docs.dapr.io/getting-started/quickstarts/statemanagement-quickstart/) APIs. 

To run the services locally you can use the [Testcontainer](https://testcontainaers.com) integration already included in the projects. 

For example you can start a local version of the `pizza-store` service by running the following command inside the `pizza-store/` directory (this requires having Java and [Maven](https://maven.apache.org/) installed locally):

```
mvn spring-boot:test-run
```

This, not only start the `pizza-store` service, but it also uses the [Testcontainers + Dapr Spring Boot](https://central.sonatype.com/artifact/io.diagrid.dapr/dapr-spring-boot-starter) integration to configure and wire up a Dapr configuration for local development. In other words, you can now use Dapr outside of Kubernetes, for writing your service tests without the need to know how Dapr is configured. 


Once the service is up, you can place orders and simulate other events coming from the Kitchen and Delivery services by sending HTTP requests to the `/events` endpoint. 

Using [`httpie`](https://httpie.io/) this look like this: 

```
http :8080/events Content-Type:application/cloudevents+json < pizza-store/event-in-prep.json
```

In the Application you should see the event recieved that the order moving forward. 


# Resources and references

- [Platform engineering on Kubernetes Book](http://mng.bz/jjKP?ref=salaboy.com)
- [Testcontainers for Go Developers](https://www.atomicjar.com/2023/08/local-development-of-go-applications-with-testcontainers/)
- [Cloud native local development with Dapr and Testcontainers](https://www.diagrid.io/blog/cloud-native-local-development)

# Feedback / Comments / Contribute

Feel free to create issues or get in touch with us using Issues or via [Twitter @Salaboy](https://twitter.com/salaboy)