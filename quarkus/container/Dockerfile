# Use Red Hat's UBI (Universal Base Image) as a build stage named 'ubi-micro-build'
FROM registry.access.redhat.com/ubi9 AS ubi-micro-build

# Set environment variable for Keycloak version and download link
ENV KEYCLOAK_VERSION 999.0.0-SNAPSHOT
ARG KEYCLOAK_DIST=https://github.com/keycloak/keycloak/releases/download/$KEYCLOAK_VERSION/keycloak-$KEYCLOAK_VERSION.tar.gz

# Install necessary packages, add and unpack Keycloak, then clean up in one layer
RUN dnf install -y tar gzip && \
    ADD $KEYCLOAK_DIST /tmp/keycloak/ && \
    (cd /tmp/keycloak && tar -xvf /tmp/keycloak/keycloak-*.tar.gz && rm /tmp/keycloak/keycloak-*.tar.gz) || true && \
    mv /tmp/keycloak/keycloak-* /opt/keycloak && \
    mkdir -p /opt/keycloak/data && \
    chmod -R g+rwX /opt/keycloak && \
    dnf clean all

# Add the UBI null script and execute it in one layer
ADD ubi-null.sh /tmp/
RUN bash /tmp/ubi-null.sh java-17-openjdk-headless glibc-langpack-en findutils

# Use Red Hat's UBI Micro as the final base image
FROM registry.access.redhat.com/ubi9-micro
ENV LANG en_US.UTF-8

COPY --from=ubi-micro-build /tmp/null/rootfs/ /
COPY --from=ubi-micro-build --chown=1000:0 /opt/keycloak /opt/keycloak

# Add keycloak user and group in one layer
RUN echo "keycloak:x:0:root" >> /etc/group && \
    echo "keycloak:x:1000:0:keycloak user:/opt/keycloak:/sbin/nologin" >> /etc/passwd

USER 1000
EXPOSE 8080 8443
ENTRYPOINT [ "/opt/keycloak/bin/kc.sh" ]