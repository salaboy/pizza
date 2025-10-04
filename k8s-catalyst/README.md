# Cloud-Native Pizza Store with Catalyst



## Installation

If you don't have a Kubernetes Cluster you can [install KinD](https://kind.sigs.k8s.io/docs/user/quick-start/) to create a local cluster to run the application. 

Once you have KinD installed you can run the following command to create a local Cluster: 

```
kind create cluster
```

## Diagrid's Catalyst

Create a new project with three App Ids

- **store**
- **kitchen**
- **delivery**

You will need to set up your Project URLs and API KEY to the following filles: 
- `pizza-store.yaml`
- `pizza-kitchen.yaml`
- `pizza-delivery.yaml`




## Installing the Application

To install the application you only need to run the following command: 

```
kubectl apply -f k8s-catalyst/
```

This install all the application services. To avoid dealing with Ingresses you can access the application by using `kubectl port-forward`, run to access the application on port `8080`. 
We also need to connect out three services to Catalyst, so Catalyst can connect back to our applications: 

For the Pizza Store Service run: 

```
diagrid dev run --app-id pizza-store --app-port 8080 -- kubectl port-forward svc/pizza-store 8080:80
```

For the Pizza Kitchen Service run: 
```
diagrid dev run --app-id kitchen-service --app-port 8081 -- kubectl port-forward svc/pizza-kitchen-service 8081:80
```

For the Pizza Delivery Service run: 

```
diagrid dev run --app-id delivery-service --app-port 8082 -- kubectl port-forward svc/pizza-delivery-service 8082:80
```

Then you can point your browser to [`http://localhost:8080`](http://localhost:8080) and you should see: 

![Pizza Store](imgs/pizza-store.png)




# Feedback / Comments / Contribute

Feel free to create issues or get in touch with us using Issues or via [Twitter @Salaboy](https://twitter.com/salaboy)
