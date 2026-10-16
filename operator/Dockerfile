FROM registry.access.redhat.com/hi/openjdk:25-builder AS build
USER root

ADD target/ubi-null.sh /tmp/
RUN bash /tmp/ubi-null.sh java-25-openjdk-headless glibc-langpack-en

FROM scratch
ENV LANG=en_US.UTF-8

COPY --from=build /tmp/null/rootfs/ /
COPY --from=build /usr/lib/jvm/java-25-openjdk/bin/jcmd /usr/lib/jvm/java-25-openjdk/bin/jcmd
COPY --from=build /usr/lib/jvm/java-25-openjdk/bin/jfr /usr/lib/jvm/java-25-openjdk/bin/jfr
COPY --from=build /usr/lib/jvm/java-25-openjdk/bin/jps /usr/lib/jvm/java-25-openjdk/bin/jps

ADD --chown=1000:0 target/quarkus-app/ /opt/keycloak

RUN ln -s /usr/lib/jvm/java-25-openjdk/bin/jcmd /usr/bin/jcmd && \
    ln -s /usr/lib/jvm/java-25-openjdk/bin/jfr /usr/bin/jfr && \
    ln -s /usr/lib/jvm/java-25-openjdk/bin/jps /usr/bin/jps && \
    chmod -R g+rwX /opt/keycloak && \
    echo "keycloak:x:0:root" >> /etc/group && \
    echo "keycloak:x:1000:0:keycloak user:/opt/keycloak:/sbin/nologin" >> /etc/passwd

USER 1000

WORKDIR /opt/keycloak

ENTRYPOINT [ "java", "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", "-jar", "quarkus-run.jar" ]

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
      io.openshift.tags=${KEYCLOAK_TAGS}

# operator specific
ARG KEYCLOAK_OPERATOR_DISPLAY_NAME="Keycloak Operator"
ARG KEYCLOAK_OPERATOR_IMAGE_NAME="keycloak-operator"
ARG KEYCLOAK_OPERATOR_DESCRIPTION="${KEYCLOAK_OPERATOR_DISPLAY_NAME} Image"

LABEL name=${KEYCLOAK_OPERATOR_IMAGE_NAME} \
      description=${KEYCLOAK_OPERATOR_DESCRIPTION} \
      summary=${KEYCLOAK_OPERATOR_DESCRIPTION} \
      io.k8s.display-name=${KEYCLOAK_OPERATOR_DISPLAY_NAME} \
      io.k8s.description=${KEYCLOAK_OPERATOR_DESCRIPTION}

# oci
ARG KEYCLOAK_SOURCE="https://github.com/keycloak/keycloak"
ARG KEYCLOAK_DOCS=${KEYCLOAK_URL}documentation

LABEL org.opencontainers.image.title=${KEYCLOAK_OPERATOR_DISPLAY_NAME} \
      org.opencontainers.image.url=${KEYCLOAK_URL} \
      org.opencontainers.image.source=${KEYCLOAK_SOURCE} \
      org.opencontainers.image.description=${KEYCLOAK_OPERATOR_DESCRIPTION} \
      org.opencontainers.image.documentation=${KEYCLOAK_DOCS}

# Base OS provenance from the Hummingbird builder image (hi/openjdk:25-builder).
# Update these when switching to a new base image version.
# To check current values: podman inspect registry.access.redhat.com/hi/openjdk:25-builder --format '{{json .Config.Labels}}'
LABEL cpe="cpe:/a:redhat:hummingbird:1" \
      io.hummingbird-project.repository="openjdk" \
      io.hummingbird-project.major-version="25" \
      io.hummingbird-project.variant.base="scratch+ubi-null"
