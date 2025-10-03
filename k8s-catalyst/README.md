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
kubectl apply -f k8s/
```

This install all the application services. To avoid dealing with Ingresses you can access the application by using `kubectl port-forward`, run to access the application on port `8080`: 

```
kubectl port-forward svc/pizza-store 8080:80
```

Then you can point your browser to [`http://localhost:8080`](http://localhost:8080) and you should see: 

![Pizza Store](imgs/pizza-store.png)




# Feedback / Comments / Contribute

Feel free to create issues or get in touch with us using Issues or via [Twitter @Salaboy](https://twitter.com/salaboy)
