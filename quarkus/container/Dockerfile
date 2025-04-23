FROM registry.access.redhat.com/ubi9/ubi AS ubi-full-build

ARG KEYCLOAK_VERSION=999.0.0-SNAPSHOT
ARG KEYCLOAK_DIST=https://github.com/vunetsystems/washington/releases/download/$KEYCLOAK_VERSION/keycloak-$KEYCLOAK_VERSION.tar.gz

# Install required OS tools and minimal fonts for Java rendering
RUN dnf install -y \
    tar gzip zip fontconfig \
    dejavu-sans-fonts \
    freetype google-droid-sans-fonts \
    java-21-openjdk-headless glibc-langpack-en findutils \
    && dnf clean all

ADD $KEYCLOAK_DIST /tmp/keycloak/

USER root

# Unpack Keycloak
RUN (cd /tmp/keycloak && \
      tar -xvf /tmp/keycloak/keycloak-*.tar.gz && \
      rm /tmp/keycloak/keycloak-*.tar.gz) || true

RUN mv /tmp/keycloak/keycloak-* /opt/keycloak && mkdir -p /opt/keycloak/data
RUN chmod -R g+rwX /opt/keycloak

# Vunet-specific build steps
WORKDIR /vunet
COPY vunet .
RUN bash ./build.sh

# ================== Final image ===================
FROM registry.access.redhat.com/ubi9/ubi

ENV LANG en_US.UTF-8

# Install runtime dependencies (minimal but enough for font rendering)
RUN dnf install -y \
    fontconfig \
    dejavu-sans-fonts \
    freetype google-droid-sans-fonts \
    java-21-openjdk-headless glibc-langpack-en findutils \
    && dnf clean all

LABEL org.opencontainers.image.source https://github.com/vunetsystems/washington

ENV KC_RUN_IN_CONTAINER true

# Copy built artifacts
COPY --from=ubi-full-build --chown=1000:0 /opt/keycloak /opt/keycloak

# Vunet-specific files
COPY --from=ubi-full-build --chown=1000:0 /vunet/provisioning/realm.json /opt/keycloak/data/import/realm.json
COPY --from=ubi-full-build --chown=1000:0 /vunet/dist/vsmaps-extensions.jar /opt/keycloak/providers/vsmaps-extensions.jar
COPY --from=ubi-full-build --chown=1000:0 /vunet/plugins/recaptcha-login.jar /opt/keycloak/providers/recaptcha-login.jar
COPY --from=ubi-full-build --chown=1000:0 /vunet/plugins/single-handshake-thirdparty-multifactor-authentication-1.0.0.jar /opt/keycloak/providers/single-handshake-thirdparty-multifactor-authentication-1.0.0.jar
COPY --from=ubi-full-build --chown=1000:0 /vunet/plugins/sms-based-thirdparty-multifactor-authentication-1.0.0.jar /opt/keycloak/providers/sms-based-thirdparty-multifactor-authentication-1.0.0.jar
COPY --from=ubi-full-build --chown=1000:0 /vunet/plugins/custom-username-password-form-1.0.0.jar /opt/keycloak/providers/custom-username-password-form-1.0.0.jar
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

# Common labels
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
      org.opencontainers.image.description=${KEYCLOAK_DESCRIPTION} \
      org.opencontainers.image.documentation=${KEYCLOAK_DOCS}

