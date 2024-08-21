FROM registry.access.redhat.com/ubi9 AS ubi-micro-build

ARG KEYCLOAK_VERSION 999.0.0-SNAPSHOT
ARG KEYCLOAK_DIST=https://github.com/vunetsystems/washington/releases/download/$KEYCLOAK_VERSION/keycloak-$KEYCLOAK_VERSION.tar.gz

RUN dnf install -y tar gzip zip

ADD $KEYCLOAK_DIST /tmp/keycloak/

# The next step makes it uniform for local development and upstream built.
# If it is a local tar archive then it is unpacked, if from remote is just downloaded.
RUN (cd /tmp/keycloak && \
    tar -xvf /tmp/keycloak/keycloak-*.tar.gz && \
    rm /tmp/keycloak/keycloak-*.tar.gz) || true

RUN mv /tmp/keycloak/keycloak-* /opt/keycloak && mkdir -p /opt/keycloak/data
RUN chmod -R g+rwX /opt/keycloak

ADD ubi-null.sh /tmp/
RUN bash /tmp/ubi-null.sh java-17-openjdk-headless glibc-langpack-en findutils

# vunet specific changes
WORKDIR /vunet
COPY vunet .
RUN bash ./build.sh

FROM registry.access.redhat.com/ubi9-micro
ENV LANG en_US.UTF-8

LABEL org.opencontainers.image.source https://github.com/vunetsystems/washington

COPY --from=ubi-micro-build /tmp/null/rootfs/ /
COPY --from=ubi-micro-build --chown=1000:0 /opt/keycloak /opt/keycloak

# vunet specific files
COPY --from=ubi-micro-build --chown=1000:0 /vunet/provisioning/realm.json /opt/keycloak/data/import/realm.json
COPY --from=ubi-micro-build --chown=1000:0 /vunet/dist/vsmaps-extensions.jar /opt/keycloak/providers/vsmaps-extensions.jar
COPY --from=ubi-micro-build --chown=1000:0 /vunet/theme/ /opt/keycloak/themes/

RUN echo "keycloak:x:0:root" >> /etc/group && \
    echo "keycloak:x:1000:0:keycloak user:/opt/keycloak:/sbin/nologin" >> /etc/passwd

USER 1000

EXPOSE 8080
EXPOSE 8443

ENV KC_DB postgres
ENV KC_HEALTH_ENABLED true
RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT [ "/opt/keycloak/bin/kc.sh" ]
