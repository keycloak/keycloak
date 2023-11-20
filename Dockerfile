# Use a base image with Java 20 and other dependencies
FROM openjdk:20-jdk-slim

# Set the Keycloak version and other environment variables as needed
ENV KC_HOSTNAME_STRICT=$$KC_HOSTNAME_STRICT
# ENV HOSTNAME=$HOSTNAME
ENV KC_HOSTNAME_STRICT_HTTPS=$$KC_HOSTNAME_STRICT_HTTPS
ENV KC_HTTP_ENABLED=$$KC_HTTP_ENABLED
ENV DB_USERNAME=$$DB_USERNAME
ENV DB_PASSWORD=$$DB_PASSWORD
ENV DB_HOST=$$DB_HOST
ENV DB_NAME=$$DB_NAME
ENV KEYCLOAK_ADMIN=$$KEYCLOAK_ADMIN
ENV KEYCLOAK_ADMIN_PASSWORD=$$KEYCLOAK_ADMIN_PASSWORD

# Create a directory for Keycloak
WORKDIR /app

# Set the working directory
COPY / /app/

# Expose ports (adjust as needed)
EXPOSE 8080

# Installing relevant packages [ curl ping ]
#RUN apt install -y iputils-ping curl

# Downloading Cloud SQL Proxy
#RUN curl -o cloud-sql-proxy https://storage.googleapis.com/cloud-sql-connectors/cloud-sql-proxy/v2.7.2/cloud-sql-proxy.linux.amd64
#RUN chmod +x cloud-sql-proxy
#RUN mv cloud-sql-proxy /usr/local/bin

# Running SQL Proxy Local Tunnel
#RUN nohup cloud-sql-proxy --private-ip disco-ascent-395314:asia-southeast1:emeritus-insights-nprod-mysql &

# Command to start Keycloak
CMD ["java", "-jar", "/app/quarkus/server/target/lib/quarkus-run.jar", "start", "--db", "mysql", "--db-username", "$DB_USERNAME", "--db-password", "$DB_PASSWORD", "--db-url-host", "10.30.16.34", "--proxy", "edge", "--db-url-database", "$DB_NAME"]
#CMD ["java", "-jar", "/app/quarkus/server/target/lib/quarkus-run.jar", "start", "--db", "mysql", "--db-username", "$DB_USERNAME", "--db-password", "$DB_PASSWORD", "--http-enabled", "true", "--hostname-strict", "false", "--db-url-host", "127.0.0.1", "--proxy", "edge", "--db-url-database", "$DB_NAME"]