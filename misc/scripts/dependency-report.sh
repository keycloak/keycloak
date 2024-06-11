#!/bin/bash

cd $(dirname $0 | xargs readlink -f)/../../

DEP=$1
TMP=$(mktemp)

if [ "$DEP" == "" ]; then
    echo "Usage: dependency-report.sh [groupId]:[artifactId]:[type]:[version]"
    exit 1
fi

./mvnw -q dependency:tree -Dincludes=$DEP -DoutputFile=$TMP -DappendOutput=true -Poperator

echo ""
echo "==================================================================================================="
echo "Dependency tree for $DEP"
echo "---------------------------------------------------------------------------------------------------"
cat $TMP
rm $TMP
echo "==================================================================================================="
echo ""

