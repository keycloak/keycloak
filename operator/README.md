# Keycloak on Quarkus

The module holds the codebase to build the Keycloak Operator on top of [Quarkus](https://quarkus.io/).
Using the [Quarkus Operator SDK](https://github.com/quarkiverse/quarkus-operator-sdk).

## Activating the Module

When build from the project root directory, this module is only enabled if the installed JDK is 11 or newer. 

## Building

Ensure you have JDK 11 (or newer) installed.

Build the Docker image with:

```bash
mvn clean package -Doperator -Dquarkus.container-image.build=true
```

## Contributing

### Quick start on Minikube

Enable the Minikube Docker daemon:

```bash
eval $(minikube -p minikube docker-env)
```

Compile the project and generate the Docker image with JIB:

```bash
mvn clean package -Doperator -Dquarkus.container-image.build=true -Dquarkus.kubernetes.deployment-target=minikube
```

Install the CRD definition and the operator in the cluster in the `keycloak` namespace:

```bash
kubectl apply -k target
```

to install in the `default` namespace:

```bash
kubectl apply -k overlays/default-namespace
```

Remove the created resources with:

```bash
kubectl delete -k <previously-used-folder>
```
