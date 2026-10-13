#!/bin/bash -e

GROUP="$1"
if [ "$GROUP" == "" ]; then
  echo 'Usage: suite.sh <group>'
  exit
fi

function printTest() {
    INPUT="$(echo $1 | sed 's|../src/test/java/org/keycloak/testsuite/||' | sed 's|.java||')"

    TEST="${INPUT##*/}"
    PKG="${INPUT%/*}"

    echo "$PKG,$TEST"
}

cd "$(readlink -f "$0" | xargs dirname)"

TEST_DIR="../src/test/java/"
SUITE_FILE="$GROUP-suite"

if [ ! -f "$SUITE_FILE" ]; then
  echo "$SUITE_FILE not found"
  exit 1
fi

for i in `cat "$SUITE_FILE" | grep -v '^[[:space:]]*$' | grep -v '^[[:space:]]*#'`; do
  if [[ "$i" != *'.'* ]]; then
    for i in $(find "$TEST_DIR" -name "$i.java"); do
        printTest $i
    done
  else
    TMP="$(echo $i | sed 's|[.]|/|g')"
    TMP="$TEST_DIR$TMP"
    
    delimiter="/"

    PKG="${TMP%$delimiter*}"
    EXP="${TMP##*$delimiter}"
    EXP=$(echo "$EXP" | sed 's/**/*/')
    
    for i in $(find $PKG -name "$EXP.java"); do
        printTest "$i"
    done 
  fi
done | sort

