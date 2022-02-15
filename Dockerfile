FROM maven:3.6.0-jdk-11-slim as builder

WORKDIR /app
COPY . .

RUN mvn -f /app/keycloak-theme/pom.xml install

FROM quay.io/keycloak/keycloak:17.0.0-legacy
COPY --from=builder /app/keycloak-theme/target/classes /opt/jboss/keycloak/themes/keycloak.v2

EXPOSE 8080
