#!/bin/bash -e

export VULN_SCRIPT_HOME=$(dirname $0 | xargs readlink -f)

if [ ! -f $HOME/target/vuln-to-issues.jar ]; then
  echo "vuln-to-issues.jar not found, building"
  CURRENT_DIR=$PWD
  cd $VULN_SCRIPT_HOME/../../
  ./mvnw package -q -f misc/vuln-to-issues
  cd $CURRENT_DIR
fi

java -jar $VULN_SCRIPT_HOME/target/vuln-to-issues.jar $@