#! /bin/bash
set -euxo pipefail

VERSION=$1
REPLACES_VERSION=$2
OPERATOR_DOCKER_IMAGE=$3

CREATED_AT=$(date "+%D %T")

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

echo "Creating OLM bundle for version $VERSION replacing version $REPLACES_VERSION"

rm -rf $SCRIPT_DIR/../olm/$VERSION
mkdir -p $SCRIPT_DIR/../olm/$VERSION

cp -r $SCRIPT_DIR/../olm-base/* $SCRIPT_DIR/../olm/$VERSION

# Inject RBAC rules
yq ea '.rules as $item ireduce ({}; .rules += $item)' $SCRIPT_DIR/../target/kubernetes/kubernetes.yml | \
  yq ea -i 'select(fileIndex==0).spec.install.spec.permissions[0] = select(fileIndex==1) | select(fileIndex==0)' $SCRIPT_DIR/../olm/$VERSION/manifests/clusterserviceversion.yaml - && \
yq ea -i '.spec.install.spec.permissions[0].serviceAccountName = "keycloak-operator"' $SCRIPT_DIR/../olm/$VERSION/manifests/clusterserviceversion.yaml && \
yq ea -i ".metadata.annotations.containerImage = \"$OPERATOR_DOCKER_IMAGE:$VERSION\"" $SCRIPT_DIR/../olm/$VERSION/manifests/clusterserviceversion.yaml && \
yq ea -i ".metadata.annotations.createdAt = \"$CREATED_AT\"" $SCRIPT_DIR/../olm/$VERSION/manifests/clusterserviceversion.yaml && \
yq ea -i ".metadata.name = \"keycloak-operator.v$VERSION\"" $SCRIPT_DIR/../olm/$VERSION/manifests/clusterserviceversion.yaml && \
yq ea -i ".spec.install.spec.deployments[0].spec.template.spec.containers[0].image = \"$OPERATOR_DOCKER_IMAGE:$VERSION\"" $SCRIPT_DIR/../olm/$VERSION/manifests/clusterserviceversion.yaml && \
yq ea 'select(.spec.template.spec.containers[0].env) | .spec.template.spec.containers[0].env[-1]' $SCRIPT_DIR/../target/kubernetes/kubernetes.yml | \
  yq ea -i 'select(fileIndex==0).spec.install.spec.deployments[0].spec.template.spec.containers[0].env += select(fileIndex==1) | select(fileIndex==0)' $SCRIPT_DIR/../olm/$VERSION/manifests/clusterserviceversion.yaml - && \
yq ea -i ".spec.version = \"$VERSION\"" $SCRIPT_DIR/../olm/$VERSION/manifests/clusterserviceversion.yaml

if [[ $REPLACES_VERSION = "NONE" ]]
then
  yq ea -i "del(.spec.replaces)" $SCRIPT_DIR/../olm/$VERSION/manifests/clusterserviceversion.yaml
else
  yq ea -i ".spec.replaces = \"keycloak-operator.v$REPLACES_VERSION\"" $SCRIPT_DIR/../olm/$VERSION/manifests/clusterserviceversion.yaml
fi

mv $SCRIPT_DIR/../olm/$VERSION/manifests/clusterserviceversion.yaml "$SCRIPT_DIR/../olm/$VERSION/manifests/keycloak-operator.v$VERSION.clusterserviceversion.yaml"

cp target/kubernetes/*.keycloak.org-v1.yml olm/$VERSION/manifests
