#!/bin/bash
echo "FUSE_INSTALLER=$JBOSS_HOME/$FUSE_INSTALLER_NAME"
if [ ! -f "$JBOSS_HOME/$FUSE_INSTALLER_NAME" ] ; then
    >&2 echo "JBOSS_HOME/$FUSE_INSTALLER_NAME doesn't exist"
    exit 1
fi

cd $JBOSS_HOME
java -jar $FUSE_INSTALLER_NAME
rm $FUSE_INSTALLER_NAME

mv standalone/deployments/hawtio*.war standalone/deployments/hawtio.war

exit 0