#!/bin/bash

# Copy Overlay
cp -r /keycloak-docker-cluster/server-overlay/* $JBOSS_HOME/

# Deploy and configure examples
/keycloak-docker-cluster/shared-files/deploy-examples.sh

# Copy MySQL driver
cd /tmp
mkdir -p mysql/main && mv /mysql-connector-java-5.1.32.jar mysql/main/
cp /keycloak-docker-cluster/shared-files/mysql-module.xml mysql/main/module.xml
mv mysql $JBOSS_MODULES_HOME/com/

# Transform standalone-keycloak-ha.xml
java -jar /usr/share/java/saxon.jar -s:$JBOSS_HOME/standalone/configuration/standalone-keycloak-ha.xml -xsl:/keycloak-docker-cluster/shared-files/standaloneXmlChanges.xsl -o:$JBOSS_HOME/standalone/configuration/standalone-keycloak-ha.xml

sed -i "s|#JAVA_OPTS=\"\$JAVA_OPTS -agentlib:jdwp=transport=dt_socket|JAVA_OPTS=\"\$JAVA_OPTS -agentlib:jdwp=transport=dt_socket|" $JBOSS_HOME/bin/standalone.conf

cp /keycloak-docker-cluster/shared-files/mysql-keycloak-ds.xml $JBOSS_HOME/standalone/deployments/

# Enable Infinispan provider
#sed -i "s|\"provider\".*: \"mem\"|\"provider\": \"infinispan\"|" $JBOSS_HOME/standalone/configuration/keycloak-server.json
#sed -i -e "s/\"connectionsJpa\"/\n \"connectionsInfinispan\": \{\n  \"default\" : \{\n   \"cacheContainer\" : \"java:jboss\/infinispan\/Keycloak\"\n  \}\n \},\n     &/" $JBOSS_HOME/standalone/configuration/keycloak-server.json
