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

mvn -Pjboss-release,nexus-staging -DretryFailedDeploymentCount=10 -DskipTests -DskipTestsuite -DskipExamples -DautoReleaseAfterClose=true -DskipLocalStaging=true deploy
#mvn -Pjboss-release,nexus-staging -DretryFailedDeploymentCount=10 -DskipTests -DskipTestsuite -DskipExamples -DautoReleaseAfterClose=true -DskipLocalStaging=true -DstagingProfileId=2161b7b8da0080 deploy
#mvn -Pjboss-release -DretryFailedDeploymentCount=10 -DskipTests -DskipTestsuite -DskipExamples deploy

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
