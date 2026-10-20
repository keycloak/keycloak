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

# Block the compromised eMudhra intermediate CAs (https://emudhra.we-have-a-problem.com/) for every TLS
# connection of the JVM, including those not covered by org.keycloak.truststore.DistrustedCertificates.
# Remove once the JDK ships them in its own blocked.certs.
RUN f="$(readlink -f /usr/lib/jvm/jre/lib/security/blocked.certs)" && \
    printf '%s\n' \
      42C4ACCD12C4F59597925E1A08A7A90102608543809308D50EC427E118BA1493 \
      45A883F349AEDFD5F3A649B5498FFB121712BA8B448F3F2A91DBC5A766FCDDBE \
      0164D7CEAE5BB909C272AF151FF44B1177AE9B47D9424B94F1D9558C30934D19 \
      62B7F9868263F72F6911FC78CEAF72C1979158B7368BF648683BB2AA39B02E72 \
      98B896625C0C230ADEEC3EB2CF6AAD5001ECF9156A68644E82374CE5833BC25B \
      5F1DED4DD590AF74631290CF073B122A2AAC6A1FA479778A0BD43579BD832699 \
      CB6E15165AC802E9B3F993F16C25D41F4E81C21EBDE71AE1B012AC00BF9C7C3F \
      7485B51C0ED192878FA1D2F948FB529AE1842CC5086C4B67F7BE5B2039DE9489 \
      E7568FE6E12331F4FD7DE61DA4C7559D0F8EB192A218D4741A3BA75184333838 \
      579480AEA166290BD3C643589E795FC0D0E54E86505D180F1DFF99EFD3199512 \
      432EC25F0D28DE9006D97BF752F17BBB2CD41F669933FCB072D9A1148D0261F3 \
      FD9D5C2DBBD4995AB69B8D50C439D9527B00529199C2FD01B2174EB58F07021F \
      9E39DE2881273ACC225A149C72BA3E0F51424963669AB633EE1DC7DF5DB067B4 \
      A496E23AC0CAB8155FC9B117B97EAE368EFCBB9041AEC8804D5A308D2D01C2D8 \
      4B2460A466F95FD40113BCDCCA2CEB29B3B093921DE45474DA7DF48874C630B1 \
      84112972B8E59D14560AE7E7119654474AAD119990CF3722BFC1E3B91615E1FD \
      >> "$f"

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
