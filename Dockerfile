# Use a base image with Java 20 and other dependencies
#FROM openjdk:20-jdk-slim
FROM alpine:3.14

# Set the Keycloak version and other environment variables as needed
ENV KC_HOSTNAME_STRICT=false
ENV KC_HOSTNAME_STRICT_HTTPS=false
ENV KC_HTTP_ENABLED=true
ENV DB_USERNAME=$DB_USERNAME
ENV DB_PASSWORD=$DB_PASSWORD
ENV DB_HOST=$DB_HOST
ENV DB_NAME=$DB_NAME

# Create a directory for Keycloak
WORKDIR /app

# Set the working directory
COPY * /app/

# Expose ports (adjust as needed)
EXPOSE 8080

# Command to start Keycloak
# CMD ["java", "-jar", "/app/quarkus/server/target/lib/quarkus-run.jar", "start", "--db", "mysql", "--db-username", "$DB_USERNAME", "--db-password", "$DB_PASSWORD", "--http-enabled", "true", "--hostname-strict", "false", "--db-url-host", "$DB_HOST", "--proxy", "edge", "--db-url-database", "$DB_NAME"]