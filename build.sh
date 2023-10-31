sudo ./mvnw -pl quarkus/deployment,quarkus/dis,themes, -am -DskipTests clean install

docker build . -t emeritus-insights-keycloak