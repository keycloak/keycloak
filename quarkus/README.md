# Keycloak Quarkus Distribution

Keycloak on Quarkus is a work in progress.

## Building and running

    mvn package
    java -jar server/target/keycloak-runner.jar
    
## Running in dev mode

    cd server
    mvn compile quarkus:dev