# Cloud-Native Pizza Delivery system

This repository contains a simple example for a Pizza delivery system using Kubernetes, Dapr and Testcontainers to enable developers with an awesome developer experience.

You can run this application on any Kubernetes cluster by following the step-by-step insturctions described in this short tutorial. 

## Installation

If you don't have a Kubernetes Cluster you can install KinD to create a local cluster to run the application. 

Once you have KinD installed you can run the following command to create a local Cluster: 

```
kind create cluster
```

Then we will install Dapr into our fresh new cluster by running the following command: 

```
helm repo add dapr https://dapr.github.io/helm-charts/
helm repo update
helm upgrade --install dapr dapr/dapr \
--version=1.12.0 \
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

## Installing the Application

To install the application you only need to run the following command: 

```
kubectl apply -f k8s/
```

