FROM scratch

# Core bundle labels.
LABEL operators.operatorframework.io.bundle.mediatype.v1=registry+v1
LABEL operators.operatorframework.io.bundle.manifests.v1=manifests/
LABEL operators.operatorframework.io.bundle.metadata.v1=metadata/
LABEL operators.operatorframework.io.bundle.package.v1=keycloak-operator
LABEL operators.operatorframework.io.bundle.channels.v1=candidate
LABEL operators.operatorframework.io.bundle.channel.default.v1=candidate
LABEL com.redhat.openshift.versions=v4.6

# Copy files to locations specified by labels.
COPY manifests /manifests/
COPY metadata /metadata/
