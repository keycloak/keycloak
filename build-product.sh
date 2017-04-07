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

    cd $GUIDE
    rm -rf build
    rm -rf target

    python ../gitlab-conversion.py

    if [ "$TOOL" = "asciidoctor" ]; then
        asciidoctor -dbook -a toc -o target/master.html target/master.adoc
        echo ""
        echo "Built file://$CURRENT_DIRECTORY/$GUIDE/target/master.html"
    fi

    if [ "$TOOL" = "ccutil" ]; then
        ccutil compile --lang en_US --format html-single --main-file target/master.adoc
        echo ""
        echo "Built file://$CURRENT_DIRECTORY/$GUIDE/build/tmp/en-US/html-single/index.html"
    fi
   
    cd ..
}

if [ "$GUIDE" = "" ]; then
    for i in authorization_services getting_started securing_apps server_admin server_development server_installation; do
        buildGuide $i
    done
else
    buildGuide $GUIDE
fi

echo "***********************************************************************************************************"
