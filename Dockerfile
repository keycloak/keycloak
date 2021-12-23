FROM maven:3.6.0-jdk-11-slim as builder

WORKDIR /app
COPY . .

RUN mvn -f /app/keycloak-theme/pom.xml install

FROM quay.io/keycloak/keycloak:16.1.0
COPY --from=builder /app/keycloak-theme/target/classes /opt/jboss/keycloak/themes/keycloak.v2

EXPOSE 8080
