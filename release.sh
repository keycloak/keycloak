#!/bin/bash -e

DIR="$PWD"
source release-details

echo "Version: $VERSION"

echo "------------------------------------------------------------------------------------------------------------"
echo "Building:"
echo ""

mvn -Pjboss-release,distribution-downloads -DskipTests -DskipTestsuite clean install


echo "------------------------------------------------------------------------------------------------------------"
echo "Deploying:"
echo ""

mvn -Pjboss-release,nexus-staging -DretryFailedDeploymentCount=10 -DskipTests -DskipTestsuite -DskipExamples -DautoReleaseAfterClose=true deploy


echo "------------------------------------------------------------------------------------------------------------"
echo "Upload to jboss.org:"
echo ""

rsync -rv --protocol=28 distribution/downloads/target/$VERSION keycloak@filemgmt.jboss.org:/downloads_htdocs/keycloak


echo "------------------------------------------------------------------------------------------------------------"
echo "NPM release:"
echo ""

cd $DIR/adapters/oidc/js
mvn clean install -DskipTests
npm publish packages/keycloak-js
npm publish packages/keycloak-authz
cd $DIR


echo "------------------------------------------------------------------------------------------------------------"
echo "Done"
echo "------------------------------------------------------------------------------------------------------------"
