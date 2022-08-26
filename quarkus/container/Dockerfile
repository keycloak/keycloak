FROM registry.access.redhat.com/ubi8-minimal AS build-env

ENV KEYCLOAK_VERSION 999-SNAPSHOT
ARG KEYCLOAK_DIST=https://github.com/keycloak/keycloak/releases/download/$KEYCLOAK_VERSION/keycloak-$KEYCLOAK_VERSION.tar.gz

RUN microdnf install -y tar gzip

ADD $KEYCLOAK_DIST /tmp/keycloak/

# The next step makes it uniform for local development and upstream built.
# If it is a local tar archive then it is unpacked, if from remote is just downloaded.
RUN (cd /tmp/keycloak && \
    tar -xvf /tmp/keycloak/keycloak-*.tar.gz && \
    rm /tmp/keycloak/keycloak-*.tar.gz) || true

RUN mv /tmp/keycloak/keycloak-* /opt/keycloak && mkdir -p /opt/keycloak/data

RUN chmod -R g+rwX /opt/keycloak

FROM registry.access.redhat.com/ubi8-minimal
ENV LANG en_US.UTF-8

COPY --from=build-env --chown=1000:0 /opt/keycloak /opt/keycloak

RUN microdnf update -y && \
    microdnf install -y --nodocs java-11-openjdk-headless glibc-langpack-en && microdnf clean all && rm -rf /var/cache/yum/* && \
    echo "keycloak:x:0:root" >> /etc/group && \
    echo "keycloak:x:1000:0:keycloak user:/opt/keycloak:/sbin/nologin" >> /etc/passwd

USER 1000

EXPOSE 8080
EXPOSE 8443

ENTRYPOINT [ "/opt/keycloak/bin/kc.sh" ]
