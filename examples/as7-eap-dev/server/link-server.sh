#!/bin/bash

if [ -z "$JBOSS_HOME"]; then
    echo "Need toset JBOSS_HOME"
    exit 1
fi

KEYCLOAK_HOME=`pwd`/../../..
mkdir $JBOSS_HOME/standalone/deployments/auth-server.war
touch $JBOSS_HOME/standalone/deployments/auth-server.war.dodeploy

ln -s $KEYCLOAK_HOME/admin-ui-styles/src/main/resources/META-INF/resources/admin-ui $JBOSS_HOME/standalone/deployments/auth-server.war/admin-ui
ln -s $KEYCLOAK_HOME/admin-ui/src/main/resources/META-INF/resources/admin $JBOSS_HOME/standalone/deployments/auth-server.war/admin
ln -s `pwd`/target/auth-server/WEB-INF $JBOSS_HOME/standalone/deployments/auth-server.war/WEB-INF

