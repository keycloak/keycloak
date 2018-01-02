#!/bin/bash -e

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

GUIDE_DIR=$1

function printLine
{
    echo "************************************************************************************************"
}

function getTitle
{
    GUIDE_DIR=`readlink -f $1`

    TITLE_KEY=`cat $GUIDE_DIR/master-docinfo.xml | grep '<title>' | cut -d '{' -f 2 | cut -d '}' -f 1`
    TITLE=`cat $GUIDE_DIR/topics/templates/document-attributes-product.adoc | grep $TITLE_KEY | sed "s/:$TITLE_KEY: //"`

    echo $TITLE    
}

function buildGuide
{
    GUIDE_DIR=`readlink -f $1`
    TITLE=`getTitle $GUIDE_DIR`

    printLine
    echo ""
    echo "Building: $TITLE"
    echo ""

    cd $GUIDE_DIR

    echo "Deleting $GUIDE_DIR/build"
    rm -rf build
    echo ""

    echo "Running asciidoctor..."
    echo ""
    asciidoctor -t -dbook -a toc -o target/html/index.html master.adoc
    echo ""

    echo "Running ccutil..."
    echo ""
    ccutil compile --lang en_US --format html-single --main-file master.adoc
    cd ..

    echo ""
    echo "Done"
    echo ""
}

function clean
{
    GUIDE_DIR=`readlink -f $1`
    cd $GUIDE_DIR
    echo "Deleting $GUIDE_DIR/build"
    rm -rf $GUIDE_DIR/build
    cd ..
}

if [ "$TOOL" = "clean" ]; then
    if [ "$GUIDE_DIR" = "" ]; then
        for i in `find -maxdepth 2 -name master.adoc | xargs dirname | sort`; do
            getTitle $i
        done
    else 
        getTitle $GUIDE_DIR
    fi
else
    if [ "$GUIDE_DIR" = "" ]; then
        for i in `find -maxdepth 2 -name master.adoc | xargs dirname | sort`; do
            buildGuide $i
        done

        printLine

        echo ""
        for i in `find -maxdepth 2 -name master.adoc | xargs dirname | sort`; do
            TITLE=`getTitle $i`
            GUIDE_DIR=`readlink -f $i`
            echo "$TITLE"
            echo " - AsciiDoctor:  file://$GUIDE_DIR/target/html/index.html"
            echo " - ccutil:       file://$GUIDE_DIR/build/tmp/en-US/html-single/index.html"
            echo ""
        done

        printLine
    else 
        buildGuide $GUIDE_DIR

        printLine
        TITLE=`getTitle $GUIDE_DIR`
        echo ""
        echo "$TITLE"
        echo " - AsciiDoctor:  file://$GUIDE_DIR/target/html/index.html"
        echo " - ccutil:       file://$GUIDE_DIR/build/tmp/en-US/html-single/index.html"
        echo ""
        printLine
    fi
fi
