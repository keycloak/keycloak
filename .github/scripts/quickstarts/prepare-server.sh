#!/bin/bash -e

VERSION=$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec -f keycloak)

unzip ~/.m2/repository/org/keycloak/keycloak-server-dist/${VERSION}/keycloak-server-dist-${VERSION}.zip
mv keycloak-${VERSION} keycloak-dist

keycloak-dist/bin/add-user-keycloak.sh -u admin -p admin

# update QS version to match KC version
mvn versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false -DgroupId=org.keycloak* -DartifactId=* -Pbump-version -B