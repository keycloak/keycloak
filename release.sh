#!/bin/bash -e

DIR="$PWD"
source release-details

echo "Version: $VERSION"

echo "------------------------------------------------------------------------------------------------------------"
echo "Building:"
echo ""

mvn -Pjboss-release,distribution-downloads,nexus-staging -DskipTests -DskipTestsuite -DretryFailedDeploymentCount=10 -DautoReleaseAfterClose=true clean deploy


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

cd $DIR/adapters/oidc/js
mvn clean install -DskipTests

cd $DIR/adapters/oidc/js/packages/keycloak-js
npm version $VERSION --git-tag-version=false
npm publish

cd $DIR/adapters/oidc/js/packages/keycloak-authz
npm install keycloak-js@$VERSION
npm version $VERSION --git-tag-version=false
npm publish

cd $DIR


echo "------------------------------------------------------------------------------------------------------------"
echo "Done"
echo "------------------------------------------------------------------------------------------------------------"
