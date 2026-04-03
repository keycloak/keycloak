FROM registry.access.redhat.com/ubi10/ubi AS ubi-full-build

ARG KEYCLOAK_VERSION=999.0.0-SNAPSHOT

RUN dnf install -y \
    tar gzip zip fontconfig \
    dejavu-sans-fonts \
    freetype google-droid-sans-fonts \
    java-21-openjdk-headless glibc-langpack-en findutils \
    && dnf clean all

ADD keycloak-*.tar.gz /tmp/keycloak/

USER root

RUN mv /tmp/keycloak/keycloak-* /opt/keycloak && mkdir -p /opt/keycloak/data
RUN chmod -R g+rwX /opt/keycloak

WORKDIR /vunet
COPY vunet .
RUN bash ./build.sh

ARG MARAUDER_VERSION
ARG MARAUDER_TARBALL

COPY marauder-plugins.tar.gz /tmp/marauder-plugins.tar.gz

RUN set -euo pipefail && \
    if [ -z "$MARAUDER_VERSION" ] || [ "$MARAUDER_VERSION" == "null" ]; then \
      echo "ERROR: MARAUDER_VERSION build-arg is missing or null"; \
      exit 1; \
    fi && \
    if [ -z "$MARAUDER_TARBALL" ] || [ "$MARAUDER_TARBALL" == "null" ]; then \
      echo "ERROR: MARAUDER_TARBALL build-arg is missing or null"; \
      exit 1; \
    fi && \
    mkdir -p /vunet/plugins && \
    if [ ! -f /tmp/marauder-plugins.tar.gz ]; then \
      echo "ERROR: /tmp/marauder-plugins.tar.gz is missing from the Docker context"; \
      exit 1; \
    fi && \
    echo "Extracting bundled Marauder asset: ${MARAUDER_TARBALL}" && \
    tar -xzf /tmp/marauder-plugins.tar.gz -C /vunet/plugins && \
    rm /tmp/marauder-plugins.tar.gz && \
    echo "=== Extracted Marauder plugins ===" && \
    ls -lh /vunet/plugins/

FROM registry.access.redhat.com/ubi10/ubi

ENV LANG en_US.UTF-8

RUN dnf install -y \
    fontconfig \
    dejavu-sans-fonts \
    freetype google-droid-sans-fonts \
    java-21-openjdk-headless glibc-langpack-en findutils \
    && dnf clean all

LABEL org.opencontainers.image.source https://github.com/vunetsystems/washington

ENV KC_RUN_IN_CONTAINER true

COPY --from=ubi-full-build --chown=1000:0 /opt/keycloak /opt/keycloak
COPY --from=ubi-full-build --chown=1000:0 /vunet/provisioning/realm.json /opt/keycloak/data/import/realm.json
COPY --from=ubi-full-build --chown=1000:0 /vunet/dist/vsmaps-extensions.jar /opt/keycloak/providers/vsmaps-extensions.jar
COPY --from=ubi-full-build --chown=1000:0 /vunet/plugins/ /opt/keycloak/providers/
COPY --from=ubi-full-build --chown=1000:0 /vunet/theme/ /opt/keycloak/themes/

RUN echo "keycloak:x:0:root" >> /etc/group && \
    echo "keycloak:x:1000:0:keycloak user:/opt/keycloak:/sbin/nologin" >> /etc/passwd

USER 1000

EXPOSE 8080
EXPOSE 8443
EXPOSE 9000

ENV KC_DB postgres
ENV KC_HEALTH_ENABLED true
RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT [ "/opt/keycloak/bin/kc.sh" ]

ARG KEYCLOAK_VERSION
ARG MARAUDER_VERSION
ARG KEYCLOAK_URL="https://www.keycloak.org/"
ARG KEYCLOAK_TAGS="keycloak security identity"
ARG KEYCLOAK_MAINTAINER=${KEYCLOAK_URL}
ARG KEYCLOAK_VENDOR=${KEYCLOAK_MAINTAINER}

LABEL maintainer=${KEYCLOAK_MAINTAINER} \
      vendor=${KEYCLOAK_VENDOR} \
      version=${KEYCLOAK_VERSION} \
      url=${KEYCLOAK_URL} \
      io.openshift.tags=${KEYCLOAK_TAGS} \
      vunetsystems.marauder.version=${MARAUDER_VERSION} \
      release="" \
      vcs-ref="" \
      com.redhat.build-host="" \
      com.redhat.component="" \
      com.redhat.license_terms=""

ARG KEYCLOAK_SERVER_DISPLAY_NAME="Keycloak Server"
ARG KEYCLOAK_SERVER_IMAGE_NAME="keycloak"
ARG KEYCLOAK_SERVER_DESCRIPTION="${KEYCLOAK_SERVER_DISPLAY_NAME} Image"

LABEL name=${KEYCLOAK_SERVER_IMAGE_NAME} \
      description=${KEYCLOAK_SERVER_DESCRIPTION} \
      summary=${KEYCLOAK_SERVER_DESCRIPTION} \
      io.k8s.display-name=${KEYCLOAK_SERVER_DISPLAY_NAME} \
      io.k8s.description=${KEYCLOAK_SERVER_DESCRIPTION}

ARG KEYCLOAK_SOURCE="https://github.com/keycloak/keycloak"
ARG KEYCLOAK_DOCS=${KEYCLOAK_URL}documentation

LABEL org.opencontainers.image.title=${KEYCLOAK_SERVER_DISPLAY_NAME} \
      org.opencontainers.image.url=${KEYCLOAK_URL} \
      org.opencontainers.image.source=${KEYCLOAK_SOURCE} \
      org.opencontainers.image.description=${KEYCLOAK_SERVER_DESCRIPTION} \
      org.opencontainers.image.documentation=${KEYCLOAK_DOCS}
