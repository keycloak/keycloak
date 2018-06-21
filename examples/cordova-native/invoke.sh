#!/bin/bash -e

export URL=$1

URL=`echo $URL | sed 's|&|\\\&|g'`


echo $URL

adb shell am start -W -a android.intent.action.VIEW -d $URL org.keycloak.examples.cordova
