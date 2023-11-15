sudo ./mvnw -pl quarkus/deployment,quarkus/dist,themes, -am -DskipTests clean install

docker build . -t emeritus-insights-keycloak