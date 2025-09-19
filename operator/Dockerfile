FROM registry.access.redhat.com/ubi9 AS ubi-micro-build

ADD target/ubi-null.sh /tmp/
RUN bash /tmp/ubi-null.sh java-21-openjdk-headless glibc-langpack-en

FROM registry.access.redhat.com/ubi9-micro
ENV LANG=en_US.UTF-8

COPY --from=ubi-micro-build /tmp/null/rootfs/ /

ADD --chown=1000:0 target/quarkus-app/ /opt/keycloak

RUN chmod -R g+rwX /opt/keycloak && \
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
      io.openshift.tags=${KEYCLOAK_TAGS} \
      release="" \
      vcs-ref="" \
      com.redhat.build-host="" \
      com.redhat.component="" \
      com.redhat.license_terms=""

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
