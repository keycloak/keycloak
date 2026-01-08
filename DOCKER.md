# Build only the server

    mvn -pl quarkus/deployment,quarkus/dist -am -DskipTests clean install

# Run Docker build

    cp ./quarkus/dist/target/keycloak-999.0.0-SNAPSHOT.tar.gz ./quarkus/container/keycloak.tar.gz

    docker build --build-arg KEYCLOAK_VERSION=999.0.0-SNAPSHOT --build-arg KEYCLOAK_DIST=keycloak.tar.gz \
        -t keycloak/keycloak:999.0.0-SNAPSHOT ./quarkus/container

# Test image

    docker run --rm --name keycloak -p 127.0.0.1:8080:8080 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin keycloak/keycloak:999.0.0-SNAPSHOT start-dev