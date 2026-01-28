#
# Copyright 2022 Red Hat, Inc. and/or its affiliates
# and other contributors as indicated by the @author tags.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

function revertChanges() {
    echo "Reverting version changes."
    git checkout ../.
}

if [ "$1" == "-h" ]; then
  echo "Change the Quarkus version to a specific version."
  echo "Usage: set-quarkus-version <version>"
  exit
fi

if [ "$1" == "--revert" ]; then
  revertChanges
  exit
fi

SCRIPT_DIR=$(dirname "$0")
DEFAULT_QUARKUS_VERSION="999-SNAPSHOT"
QUARKUS_VERSION=${1:-"$DEFAULT_QUARKUS_VERSION"}
QUARKUS_BRANCH="$QUARKUS_VERSION"

EXCLUDED_DEPENDENCIES=(
    "infinispan"
    "jakarta.mail"
    "webauthn4j" # https://github.com/keycloak/keycloak/issues/36385
)

if [ "$QUARKUS_BRANCH" == "$DEFAULT_QUARKUS_VERSION" ]; then
    QUARKUS_BRANCH="main"
fi

QUARKUS_BOM_URL="https://raw.githubusercontent.com/quarkusio/quarkus/$QUARKUS_BRANCH/bom/application/pom.xml"
QUARKUS_PARENT_POM_URL="https://raw.githubusercontent.com/quarkusio/quarkus/$QUARKUS_BRANCH/independent-projects/parent/pom.xml"

VERSIONS_FROM_QUARKUS_PARENT=(
    "version.surefire.plugin"
)

if ! $(curl --output /dev/null --silent --head --fail "$QUARKUS_BOM_URL"); then
    echo "Failed to resolve version from Quarkus BOM at '$QUARKUS_BOM_URL'"
    exit 1
fi

if ! $(curl --output /dev/null --silent --head --fail "$QUARKUS_PARENT_POM_URL"); then
    echo "Failed to resolve version from Quarkus Parent pom.xml at '$QUARKUS_PARENT_POM_URL'"
    exit 1
fi

QUARKUS_BOM=$(curl -f -s "$QUARKUS_BOM_URL")
QUARKUS_PARENT_POM=$(curl -f -s "$QUARKUS_PARENT_POM_URL")

echo "Setting Quarkus version: $QUARKUS_VERSION"

$SCRIPT_DIR/../mvnw -B versions:set-property -f $SCRIPT_DIR/../pom.xml -Dproperty=quarkus.version -DnewVersion="$QUARKUS_VERSION" 1> /dev/null
$SCRIPT_DIR/../mvnw -B versions:set-property -f $SCRIPT_DIR/../pom.xml -Dproperty=quarkus.build.version -DnewVersion="$QUARKUS_VERSION" 1> /dev/null

for dependency in "${VERSIONS_FROM_QUARKUS_PARENT[@]}"; do
    VERSION=$(grep -oP "(?<=<$dependency>)[^<]*(?=</$dependency>)" <<< "$QUARKUS_PARENT_POM")
    echo "Setting $dependency to $VERSION based on the Quarkus Parent pom.xml"
    $SCRIPT_DIR/../mvnw versions:set-property -f $SCRIPT_DIR/../pom.xml -Dproperty="$dependency" -DnewVersion="$VERSION" 1> /dev/null
done

DEPENDENCIES_LIST=$(grep -oP '(?<=\</).*(?=\.version\>)' "$SCRIPT_DIR/../pom.xml")

echo "Changing dependencies: $DEPENDENCIES_LIST"
$SCRIPT_DIR/../mvnw -f $SCRIPT_DIR/pom.xml versions:revert 1> /dev/null

for dependency in $DEPENDENCIES_LIST; do
    for excluded in "${EXCLUDED_DEPENDENCIES[@]}"; do
        if [[ $dependency =~ $excluded ]]; then
            echo "Skipping $dependency because it is listed as an excluded dependency"
            continue 2
        fi
    done
    VERSION=$(grep -oP "(?<=<$dependency.version>).*(?=</$dependency.version)" <<< "$QUARKUS_BOM")
    if [ "$VERSION" == "" ]; then
        echo "Failed to resolve version for dependency '$dependency'"
        continue
    fi
    echo "Setting $dependency to $VERSION"
        $SCRIPT_DIR/../mvnw versions:set-property -f $SCRIPT_DIR/../pom.xml -Dproperty="$dependency".version -DnewVersion="$VERSION" 1> /dev/null
        $SCRIPT_DIR/../mvnw versions:set-property -f $SCRIPT_DIR/pom.xml -Dproperty="$dependency".version -DnewVersion="$VERSION" 1> /dev/null
done

echo ""
echo "Done!"