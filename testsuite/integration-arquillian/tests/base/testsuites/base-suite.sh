#!/bin/bash -e

GROUP="$1"
if [ "$GROUP" == "" ]; then
  echo 'Usage: base-suite.sh <group>'
  exit
fi

cd "`readlink -f "$0" | xargs dirname`"

TESTSUITE_FILE='base-suite'
TEST_DIR='../src/test/java/org/keycloak/testsuite'
BASE_PACKAGE='org.keycloak.testsuite'

PACKAGES=`cat $TESTSUITE_FILE | grep -v '^[[:space:]]*$' | grep -v '^[[:space:]]*#'`

# Check all packages in testsuite are included
for i in `ls -d $TEST_DIR/*/ | sed "s|$TEST_DIR||g" | sed "s|/||g"`; do
  if ( ! cat $TESTSUITE_FILE | grep "^$i," >/dev/null ); then
    echo "Package 'org.keycloak.testsuite.$i' not defined in base-suite"
    exit 1
  fi
done

SEP=""
TESTS=""
for i in `echo $PACKAGES`; do
  PACKAGE=`echo $i | cut -d ',' -f 1`
  PACKAGE_GROUP=`echo $i | cut -d ',' -f 2`

  # Check package exists
  if [ ! -d "$TEST_DIR/$PACKAGE" ]; then
    echo "Package 'org.keycloak.testsuite.$PACKAGE' not found"
    exit 1
  fi

  if [ "$GROUP" == "$PACKAGE_GROUP" ]; then
    TESTS="$TESTS$SEP$BASE_PACKAGE.$PACKAGE.**"
    SEP=','
  fi
done

echo "$TESTS"