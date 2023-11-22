# Use a base image with Java 20 and other dependencies
FROM openjdk:20-jdk-slim

# Set the Keycloak version and other environment variables as needed
ENV KC_HOSTNAME_STRICT=$KC_HOSTNAME_STRICT
ENV HOSTNAME=$HOSTNAME
ENV KC_HOSTNAME_STRICT_HTTPS=$KC_HOSTNAME_STRICT_HTTPS
ENV KC_HTTP_ENABLED=$KC_HTTP_ENABLED
ENV DB_USERNAME=$DB_USERNAME
ENV DB_PASSWORD=$DB_PASSWORD
ENV DB_HOST=$DB_HOST
ENV DB_NAME=$DB_NAME
ENV KEYCLOAK_ADMIN=$KEYCLOAK_ADMIN
ENV KEYCLOAK_ADMIN_PASSWORD=$KEYCLOAK_ADMIN_PASSWORD

# Create a directory for Keycloak
WORKDIR /app

# Set the working directory
COPY / /app/

# Expose ports (adjust as needed)
EXPOSE 8080

# Command to start Keycloak
#CMD ["java", "-jar", "/app/quarkus/server/target/lib/quarkus-run.jar", "start", "--db", "mysql", "--db-username", "$DB_USERNAME", "--db-password", "$DB_PASSWORD", "--db-url-host", "$DB_HOST", "--proxy", "edge", "--db-url-database", "$DB_NAME"]
CMD ["java", "-jar", "/app/quarkus/server/target/lib/quarkus-run.jar", "start", "--db", "mysql", "--db-username", "$DB_USERNAME", "--db-password", "$DB_PASSWORD", "--hostname", "$HOSTNAME", "--db-url-host", "$DB_HOST", "--proxy", "edge", "--db-url-database", "$DB_NAME"]
