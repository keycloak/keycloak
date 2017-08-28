#!/bin/bash

TOOL="asciidoctor"

while getopts "h?auc" opt; do
    case "$opt" in
    h|\?)
        echo "Usage: build-guide.sh [OPTION] [GUIDE]"
        echo ""
        echo " -a   use asciidoctor (default)"
        echo " -u   use ccutil"
        echo " -c   delete built guides"
        echo ""
        echo "If guide is not specified all guides are built. GUIDE should be the directory"
        echo "name of the specific guide to build."
        exit 0
        ;;
    a)  TOOL="asciidoctor"
        ;;
    u)  TOOL="ccutil"
        ;;
    c)  TOOL="clean"
        ;;
    esac
done

shift $((OPTIND-1))
[ "$1" = "--" ] && shift

GUIDE=$1

function buildGuide
{
    GUIDE=$1
    CURRENT_DIRECTORY=$(pwd)

    echo "***********************************************************************************************************"
    echo "$TOOL: $GUIDE"
    echo ""

    cp /home/st/tmp/keycloak-documentation/$GUIDE/pom.xml $GUIDE/
}

if [ "$GUIDE" = "" ]; then
    for i in authorization_services getting_started securing_apps server_admin server_development server_installation; do
        buildGuide $i
    done
else
    buildGuide $GUIDE
fi

echo "***********************************************************************************************************"
