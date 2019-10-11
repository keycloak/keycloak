# Keycloak Quarkus Distribution

Keycloak on Quarkus is a work in progress.

## Building

    mvn -f ../pom.xml clean install -DskipTestsuite -DskipExamples -DskipTests -Pquarkus

## Running

    java -jar server/target/keycloak-runner.jar
    
## Running in dev mode

    cd server
    mvn compile quarkus:dev

