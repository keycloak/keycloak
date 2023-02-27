#!/bin/bash -e

GROUP="$1"

if [ "$GROUP" == "" ]; then
  echo 'Usage: base-suite-profiles.sh <group>'
  exit
fi

cd "`readlink -f "$0" | xargs dirname`"

GROUP_PROFILES_FILE='base-suite-group-profiles'
PROFILES=`cat $GROUP_PROFILES_FILE | grep -v '^[[:space:]]*$' | grep -v '^[[:space:]]*#'`

SEP=""
PROFILE_PROP="-P"
APPLY_PROFILES=""

for i in `echo $PROFILES`; do
  PROFILE_GROUP=`echo $i | cut -d ';' -f 1`
  PROFILE=`echo $i | cut -d ';' -f 2`

  if [ "$GROUP" == "$PROFILE_GROUP" ]; then
      APPLY_PROFILES="$PROFILE_PROP$APPLY_PROFILES$SEP$PROFILE"
      SEP=','
      PROFILE_PROP=''
  fi
done

echo "$APPLY_PROFILES"



