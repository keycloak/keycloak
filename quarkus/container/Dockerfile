FROM registry.access.redhat.com/ubi9 AS ubi-micro-build

ARG KEYCLOAK_VERSION=999.0.0-SNAPSHOT
ARG KEYCLOAK_DIST=https://github.com/keycloak/keycloak/releases/download/$KEYCLOAK_VERSION/keycloak-$KEYCLOAK_VERSION.tar.gz

RUN dnf install -y tar gzip

ADD $KEYCLOAK_DIST /tmp/keycloak/

# The next step makes it uniform for local development and upstream built.
# If it is a local tar archive then it is unpacked, if from remote is just downloaded.
RUN (cd /tmp/keycloak && \
    tar -xvf /tmp/keycloak/keycloak-*.tar.gz && \
    rm /tmp/keycloak/keycloak-*.tar.gz) || true

RUN mv /tmp/keycloak/keycloak-* /opt/keycloak && mkdir -p /opt/keycloak/data
RUN chmod -R g+rwX /opt/keycloak

ADD ubi-null.sh /tmp/
RUN bash /tmp/ubi-null.sh java-21-openjdk-headless glibc-langpack-en findutils

FROM registry.access.redhat.com/ubi9-micro
ENV LANG=en_US.UTF-8

# Flag for determining app is running in container
ENV KC_RUN_IN_CONTAINER=true

COPY --from=ubi-micro-build /tmp/null/rootfs/ /
COPY --from=ubi-micro-build --chown=1000:0 /opt/keycloak /opt/keycloak

RUN echo "keycloak:x:0:root" >> /etc/group && \
    echo "keycloak:x:1000:0:keycloak user:/opt/keycloak:/sbin/nologin" >> /etc/passwd

USER 1000

EXPOSE 8080
EXPOSE 8443
EXPOSE 9000

ENTRYPOINT [ "/opt/keycloak/bin/kc.sh" ]

# common labels
ARG KEYCLOAK_VERSION
ARG KEYCLOAK_URL="https://www.keycloak.org/"
ARG KEYCLOAK_TAGS="keycloak security identity"
ARG KEYCLOAK_MAINTAINER=${KEYCLOAK_URL}
ARG KEYCLOAK_VENDOR=${KEYCLOAK_MAINTAINER}

LABEL maintainer=${KEYCLOAK_MAINTAINER} \
      vendor=${KEYCLOAK_VENDOR} \
      version=${KEYCLOAK_VERSION} \
      url=${KEYCLOAK_URL} \
      io.openshift.tags=${KEYCLOAK_TAGS} \
      release="" \
      vcs-ref="" \
      com.redhat.build-host="" \
      com.redhat.component="" \
      com.redhat.license_terms=""

# server specific
ARG KEYCLOAK_SERVER_DISPLAY_NAME="Keycloak Server"
ARG KEYCLOAK_SERVER_IMAGE_NAME="keycloak"
ARG KEYCLOAK_SERVER_DESCRIPTION="${KEYCLOAK_SERVER_DISPLAY_NAME} Image"

LABEL name=${KEYCLOAK_SERVER_IMAGE_NAME} \
      description=${KEYCLOAK_SERVER_DESCRIPTION} \
      summary=${KEYCLOAK_SERVER_DESCRIPTION} \
      io.k8s.display-name=${KEYCLOAK_SERVER_DISPLAY_NAME} \
      io.k8s.description=${KEYCLOAK_SERVER_DESCRIPTION}

# oci
ARG KEYCLOAK_SOURCE="https://github.com/keycloak/keycloak"
ARG KEYCLOAK_DOCS=${KEYCLOAK_URL}documentation

LABEL org.opencontainers.image.title=${KEYCLOAK_SERVER_DISPLAY_NAME} \
      org.opencontainers.image.url=${KEYCLOAK_URL} \
      org.opencontainers.image.source=${KEYCLOAK_SOURCE} \
      org.opencontainers.image.description=${KEYCLOAK_SERVER_DESCRIPTION} \
      org.opencontainers.image.documentation=${KEYCLOAK_DOCS}
