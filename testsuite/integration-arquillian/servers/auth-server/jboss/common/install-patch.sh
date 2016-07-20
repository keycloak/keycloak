#!/bin/bash
echo "JBOSS_HOME=$JBOSS_HOME"

if [ ! -d "$JBOSS_HOME/bin" ] ; then
    >&2 echo "JBOSS_HOME/bin doesn't exist"
    exit 1
fi

cd $JBOSS_HOME/bin

RESULT=0
./jboss-cli.sh --command="patch apply $PATCH_ZIP"
if [ $? -ne 0 ]; then RESULT=1; fi
   exit $RESULT
fi

exit 1
