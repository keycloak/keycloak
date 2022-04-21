#!/bin/bash -e

DIR="$PWD"
source release-details

echo "Version: $VERSION"

echo "------------------------------------------------------------------------------------------------------------"
echo "Building:"
echo ""

mvn -Pgpg,jboss-release,distribution-downloads,nexus-staging -DskipTests -DskipTestsuite -DretryFailedDeploymentCount=10 -DautoReleaseAfterClose=true clean deploy


echo "------------------------------------------------------------------------------------------------------------"
echo "Create tag:"
echo ""

git tag $VERSION
git push origin $VERSION


echo "------------------------------------------------------------------------------------------------------------"
echo "Upload to GitHub releases:"
echo ""

hub release create -m "$VERSION" $VERSION
cd distribution/downloads/target/$VERSION

for i in *; do
  echo "Uploading $i"
  hub release edit -a $i -m "" $VERSION
done


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
