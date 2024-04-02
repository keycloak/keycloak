FROM registry.access.redhat.com/ubi9 AS ubi-micro-build

ENV KEYCLOAK_OPERATOR_DISPLAY_NAME "Keycloak Operator"
ENV KEYCLOAK_OPERATOR_IMAGE_NAME "keycloak-operator"
ENV KEYCLOAK_MAINTAINER "https://www.keycloak.org/"

ADD target/ubi-null.sh /tmp/
RUN bash /tmp/ubi-null.sh java-17-openjdk-headless glibc-langpack-en

FROM registry.access.redhat.com/ubi9-micro
ENV LANG en_US.UTF-8

COPY --from=ubi-micro-build /tmp/null/rootfs/ /

ADD --chown=1000:0 target/quarkus-app/ /opt/keycloak

RUN chmod -R g+rwX /opt/keycloak && \
    echo "keycloak:x:0:root" >> /etc/group && \
    echo "keycloak:x:1000:0:keycloak user:/opt/keycloak:/sbin/nologin" >> /etc/passwd

USER 1000

WORKDIR /opt/keycloak

ENTRYPOINT [ "java", "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", "-jar", "quarkus-run.jar" ]

LABEL name="$KEYCLOAK_OPERATOR_IMAGE_NAME" \
      maintainer="$KEYCLOAK_MAINTAINER" \
      io.k8s.display-name="$KEYCLOAK_OPERATOR_DISPLAY_NAME" \
      description="$KEYCLOAK_OPERATOR_DISPLAY_NAME Image" \
      io.k8s.description="$KEYCLOAK_OPERATOR_DISPLAY_NAME Image"
