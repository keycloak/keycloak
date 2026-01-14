# Build only the server

```shell
    mvn -pl quarkus/deployment,quarkus/dist -am -DskipTests clean install
```

# Run Docker build

```shell
    cp ./quarkus/dist/target/keycloak-999.0.0-SNAPSHOT.tar.gz ./quarkus/container/keycloak.tar.gz

    docker build --build-arg KEYCLOAK_VERSION=999.0.0-SNAPSHOT --build-arg KEYCLOAK_DIST=keycloak.tar.gz \
        -t keycloak/keycloak:999.0.0-SNAPSHOT ./quarkus/container
    docker tag keycloak/keycloak:999.0.0-SNAPSHOT zeta/devops/keycloak:999.0.0-SNAPSHOT
```

# Test image

```shell
    docker run --rm --name keycloak -p 127.0.0.1:8080:8080 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin keycloak/keycloak:999.0.0-SNAPSHOT start-dev
```
