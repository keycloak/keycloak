
ARG DIST=amazonlinux

FROM amazoncorretto:17 AS base

ENV KEYCLOAK_VERSION 999.0.0-SNAPSHOT
ARG KEYCLOAK_DIST=https://github.com/keycloak/keycloak/releases/download/$KEYCLOAK_VERSION/keycloak-$KEYCLOAK_VERSION.tar.gz

ADD $KEYCLOAK_DIST /tmp/keycloak/

# The next step makes it uniform for local development and upstream built.
# If it is a local tar archive then it is unpacked, if from remote is just downloaded.
RUN set -eux; \
    yum  -y install tar zip gzip ; \
    cd /tmp/keycloak ; \
    tar -xvf keycloak-*.tar.gz ; \
    rm -f keycloak-*.tar.gz ; \
    mv keycloak-* /opt/keycloak ; \
    mkdir -p /opt/keycloak/data ; \
    chmod -R g+rwX /opt/keycloak

FROM amazoncorretto:17 AS dist-amazonlinux

# Install curl. May be useful in heatlcheck
RUN set -eux; \
    yum  -y install curl ; \
    yum clean all ; \
    rm -rf /var/cache/yum


FROM amazoncorretto:17-alpine AS  dist-alpine

# Install bash for kc.sh script and curl. Curl may be useful in heatlcheck
RUN set -eux ; \
    apk add --no-cache bash curl

FROM dist-${DIST}

ENV LANG en_US.UTF-8

COPY --from=base --chown=1000:0 /opt/keycloak /opt/keycloak

RUN echo "keycloak:x:0:root" >> /etc/group && \
    echo "keycloak:x:1000:0:keycloak user:/opt/keycloak:/sbin/nologin" >> /etc/passwd

USER 1000

EXPOSE 8080
EXPOSE 8443

ENTRYPOINT [ "/opt/keycloak/bin/kc.sh" ]
