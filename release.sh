#!/bin/bash -e

DIR="$PWD"
VERSION=`./get-version.sh`
echo "Version: $VERSION"

echo "------------------------------------------------------------------------------------------------------------"
echo "Building:"
echo ""

mvn -Pjboss-release -DskipTests clean install


echo "------------------------------------------------------------------------------------------------------------"
echo "Deploying:"
echo ""

mvn -Pjboss-release,nexus-staging -DretryFailedDeploymentCount=10 -DskipTests deploy

mvn nexus-staging:release


echo "------------------------------------------------------------------------------------------------------------"
echo "Upload to jboss.org:"
echo ""

rsync -rv --protocol=28 distribution/downloads/target/$VERSION keycloak@filemgmt.jboss.org:/downloads_htdocs/keycloak


echo "------------------------------------------------------------------------------------------------------------"
echo "NPM release:"
echo ""

TMP=`mktemp -d`
cd $TMP
unzip $DIR/keycloak/distribution/adapters/js-adapter-npm-zip/target/keycloak-js-adapter-npm-dist-$VERSION.zip
cd keycloak-js-adapter-npm-dist-$VERSION

npm publish

cd $DIR
rm -rf $TMP


echo "------------------------------------------------------------------------------------------------------------"
echo "Done"
echo "------------------------------------------------------------------------------------------------------------"
