#!/bin/bash

DIR=`dirname $0 | xargs readlink -f`
cd $DIR/../../

find . -name licenses.xml -not -path '*/target/*' -exec distribution/licenses-common/download-license-files.sh {} \;
