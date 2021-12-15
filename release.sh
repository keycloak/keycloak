#!/bin/bash -e

VERSION=`./get-version.sh`
echo "Version: $VERSION"

echo "------------------------------------------------------------------------------------------------------------"
echo "Building:"
echo ""

mvn --file=./keycloak-theme/pom.xml -Pnexus-staging -DretryFailedDeploymentCount=10 -DautoReleaseAfterClose=true clean deploy

echo "------------------------------------------------------------------------------------------------------------"
echo "Create tag:"
echo ""

git tag $VERSION
git push origin $VERSION

echo "------------------------------------------------------------------------------------------------------------"
echo "Upload to GitHub releases:"
echo ""

hub release create -m "$VERSION" $VERSION
