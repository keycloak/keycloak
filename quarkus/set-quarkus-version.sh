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

if [ "$1" == "-h" ]; then
  echo "Change the Quarkus version to a specific version."
  echo "Usage: set-quarkus-version <version>"
  exit
fi

DEFAULT_QUARKUS_VERSION="999-SNAPSHOT"
QUARKUS_VERSION=${1:-"$DEFAULT_QUARKUS_VERSION"}
QUARKUS_BRANCH="$QUARKUS_VERSION"

if [ "$QUARKUS_BRANCH" == "$DEFAULT_QUARKUS_VERSION" ]; then
    QUARKUS_BRANCH="main"
fi

QUARKUS_BOM_URL="https://raw.githubusercontent.com/quarkusio/quarkus/$QUARKUS_BRANCH/bom/application/pom.xml"

if ! $(curl --output /dev/null --silent --head --fail "$QUARKUS_BOM_URL"); then
    echo "Failed to resolve version from Quarkus BOM at '$QUARKUS_BOM_URL'"
    exit 1
fi

QUARKUS_BOM=$(curl -s "$QUARKUS_BOM_URL")

echo "Setting Quarkus version: $QUARKUS_VERSION"
$(mvn -f ../pom.xml versions:revert 1> /dev/null)
$(mvn versions:set-property -f ../pom.xml -Dproperty=quarkus.version -DnewVersion="$QUARKUS_VERSION" 1> /dev/null)

DEPENDENCIES_LIST="resteasy jackson-bom hibernate-orm mysql-jdbc postgresql-jdbc microprofile-metrics-api wildfly-common wildfly-elytron"

echo "Changing dependencies: $DEPENDENCIES_LIST"
$(mvn -f ./pom.xml versions:revert 1> /dev/null)

for dependency in $DEPENDENCIES_LIST; do
    VERSION=$(grep -oP "(?<=<$dependency.version>).*(?=</$dependency.version)" <<< "$QUARKUS_BOM")
    if [ "$VERSION" == "" ]; then
        echo "Failed to resolve version for dependency '$dependency'"
        exit 1
    fi
    echo "Setting $VERSION to $dependency"
    $(mvn versions:set-property -pl . -Dproperty="$dependency".version -DnewVersion="$VERSION" 1> /dev/null)
done

echo ""
echo "Done!"