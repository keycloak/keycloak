#!/bin/bash -e

DIR="$PWD"
source release-details

echo "Version: $VERSION"

echo "------------------------------------------------------------------------------------------------------------"
echo "Building:"
echo ""

mvn -Pjboss-release,distribution-downloads,nexus-staging -DskipTests -DskipTestsuite -DretryFailedDeploymentCount=10 -DautoReleaseAfterClose=true clean deploy


echo "------------------------------------------------------------------------------------------------------------"
echo "Upload to jboss.org:"
echo ""

rsync -rv --protocol=28 distribution/downloads/target/$VERSION keycloak@filemgmt.jboss.org:/downloads_htdocs/keycloak


echo "------------------------------------------------------------------------------------------------------------"
echo "NPM release:"
echo ""

TMP=`mktemp -d`
cd $TMP
unzip $DIR/distribution/adapters/js-adapter-npm-zip/target/keycloak-js-adapter-npm-dist-$VERSION.zip
cd keycloak-js-adapter-npm-dist-$VERSION

npm publish

cd $DIR
rm -rf $TMP


echo "------------------------------------------------------------------------------------------------------------"
echo "Done"
echo "------------------------------------------------------------------------------------------------------------"
