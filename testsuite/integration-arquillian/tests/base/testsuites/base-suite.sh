#!/bin/bash -e

DIR=`readlink -f $0 | xargs dirname`
GROUP=$1
TESTSUITE_FILE="$DIR/base-suite"
TEST_DIR="$DIR/../src/test/java/org/keycloak/testsuite"

if [ "$GROUP" == "" ]; then
  echo "Usage: base-suite.sh <group>"
  exit
fi

PACKAGES=`cat $TESTSUITE_FILE | grep -v '^[[:space:]]*$' | grep -v '^[[:space:]]*#'`

# Check all packages in testsuite are included
for i in `ls -d $TEST_DIR/*/ | sed "s|$TEST_DIR||g" | sed "s|/||g"`; do
  if ( ! cat $TESTSUITE_FILE | grep "^$i," >/dev/null ); then
    echo "Test category '$i' not defined in base-suite"
    exit 1
  fi
done

SEP=""
GROUP_PACKAGES=""
for i in `echo $PACKAGES`; do
  PACKAGE=`echo $i | cut -d ',' -f 1`
  PACKAGE_GROUP=`echo $i | cut -d ',' -f 2`

  # Check package exists
  if [ ! -d "$TEST_DIR/$PACKAGE" ]; then
    echo "$PACKAGE not found in org/keycloak/testsuite"
    exit 1
  fi

  if [ "$GROUP" == "$PACKAGE_GROUP" ]; then
    GROUP_PACKAGES="$GROUP_PACKAGES$SEP$PACKAGE"
    SEP='|'
  fi
done

echo "%regex[org.keycloak.testsuite.($GROUP_PACKAGES).*]"