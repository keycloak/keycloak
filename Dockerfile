FROM maven:3.6.0-jdk-11-slim as builder

WORKDIR /app
COPY . .

RUN mvn -f /app/keycloak-theme/pom.xml install

FROM quay.io/keycloak/keycloak:17.0.0
COPY --from=builder /app/keycloak-theme/target/keycloak-admin-ui*.jar /opt/jboss/keycloak/lib/lib/main/

EXPOSE 8080
