# First Step - Prepare Distribution and Set Up for Custom Provider Authenticator
FROM registry.access.redhat.com/ubi9 AS ubi-micro-build

ENV KEYCLOAK_VERSION 999.0.0-SNAPSHOT
ARG KEYCLOAK_VERSION=21.1.1
ARG KEYCLOAK_DIST=https://github.com/keycloak/keycloak/releases/download/$KEYCLOAK_VERSION/keycloak-$KEYCLOAK_VERSION.tar.gz

# RUN yum provides javac # yields java-11-openjdk-devel as the only option? Unless I'm reading the output wrong.

RUN dnf install -y tar gzip

# RUN mvn -version # 3.6.3

ADD $KEYCLOAK_DIST /tmp/keycloak/

# The next step makes it uniform for local development and upstream built.
# If it is a local tar archive then it is unpacked, if from remote is just downloaded.
RUN (cd /tmp/keycloak && \
    tar -xvf /tmp/keycloak/keycloak-*.tar.gz && \
    rm /tmp/keycloak/keycloak-*.tar.gz) || true

RUN mv /tmp/keycloak/keycloak-* /opt/keycloak && mkdir -p /opt/keycloak/data
RUN chmod -R g+rwX /opt/keycloak

ADD ubi-null.sh /tmp/
RUN bash /tmp/ubi-null.sh java-17-openjdk-headless glibc-langpack-en

# Set Up For Custom Provider Authenticator
RUN dnf install -y java-11-openjdk-devel maven
ADD ./custom-provider-authenticator /tmp/custom-provider-authenticator
WORKDIR /tmp/custom-provider-authenticator
RUN mvn clean compile assembly:single
RUN mv ./target/example-custom-spi.jar /opt/keycloak/providers/example-custom-spi.jar
WORKDIR /

# Second Stage - Set Up For kc.sh
FROM registry.access.redhat.com/ubi9-micro AS keycloak-image
ENV LANG en_US.UTF-8

COPY --from=ubi-micro-build /tmp/null/rootfs/ /
COPY --from=ubi-micro-build --chown=1000:0 /opt/keycloak /opt/keycloak

RUN echo "keycloak:x:0:root" >> /etc/group && \
    echo "keycloak:x:1000:0:keycloak user:/opt/keycloak:/sbin/nologin" >> /etc/passwd

USER 1000

EXPOSE 8080
EXPOSE 8443

ENTRYPOINT [ "/opt/keycloak/bin/kc.sh" ]

# Third Stage - Configure Build Time Options
FROM keycloak-image as builder

# These are build time options
# https://www.keycloak.org/server/all-config
ENV KC_DB=postgres
ENV KC_HTTP_RELATIVE_PATH=/
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

WORKDIR /opt/keycloak
# for demonstration purposes only, please make sure to use proper certificates in production instead
RUN keytool -genkeypair -storepass password -storetype PKCS12 -keyalg RSA -keysize 2048 -dname "CN=server" -alias server -ext "SAN:c=DNS:localhost,IP:127.0.0.1" -keystore conf/server.keystore
RUN /opt/keycloak/bin/kc.sh build

# Fourth and Final Stage - An Image Which Runs Keycloak
FROM quay.io/keycloak/keycloak:latest
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# https://www.keycloak.org/server/all-config
ENV KC_DB_PASSWORD=password
ENV KC_DB_SCHEMA=public
ENV KC_DB_URL_HOST=postgres
ENV KC_DB_USERNAME=keycloak

# Hostname settings
ENV KC_HOSTNAME=localhost
ENV KC_HOSTNAME_PORT=8443
ENV KC_HOSTNAME_STRICT=false
ENV KC_HOSTNAME_STRICT_BACKCHANNEL=false

# HTTP/TLS settings
ENV KC_HTTP_ENABLED=false
ENV KC_HTTP_RELATIVE_PATH=/
ENV KC_HTTPS_PROTOCOLS=TLSv1.3,TLSv1.2

ENV KC_HEALTH_ENABLED=true

ENV KC_METRICS_ENABLED=true

# Logging Configuration
ENV KC_LOG=console
ENV KC_LOG_CONSOLE_COLOR=false
ENV KC_LOG_CONSOLE_OUTPUT=default
ENV KC_LOG_LEVEL=info

ENV KEYCLOAK_ADMIN=admin
ENV KEYCLOAK_ADMIN_PASSWORD=password
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start"]
