#!/bin/bash -e

VERSION=$1
SHORT_VERSION=`echo $VERSION | awk -F '.' '{ print $1"."$2 }'`
NPM_VERSION=`echo $VERSION | sed 's/.Final//' | sed 's/.CR/-cr./' | sed 's/.Beta/-beta./' | sed 's/.Alpha/-alpha./'`

sed -i 's/:project_version: .*/:project_version: '$VERSION'/' topics/templates/document-attributes-community.adoc
sed -i 's/:project_versionMvn: .*/:project_versionMvn: '$VERSION'/' topics/templates/document-attributes-community.adoc
sed -i 's/:project_versionNpm: .*/:project_versionNpm: '$NPM_VERSION'/' topics/templates/document-attributes-community.adoc
sed -i 's/:project_versionDoc: .*/:project_versionDoc: '$SHORT_VERSION'/' topics/templates/document-attributes-community.adoc
