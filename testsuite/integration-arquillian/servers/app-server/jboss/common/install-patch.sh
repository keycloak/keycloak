#!/bin/bash
echo "JBOSS_HOME=$JBOSS_HOME"

if [ ! -d "$JBOSS_HOME/bin" ] ; then
    >&2 echo "JBOSS_HOME/bin doesn't exist"
    exit 1
fi

cd $JBOSS_HOME/bin

RESULT=0
patches=$(echo $APP_PATCH_ZIPS | tr "," "\n")
for patch in $patches
do
    if [[ $patch == http* ]];
    then
       wget -O ./patch.zip $patch >/dev/null 2>&1
       patch=./patch.zip
    fi
    ./jboss-cli.sh --command="patch apply $patch"
    if [ $? -ne 0 ]; then exit 1; fi
done
exit 0
