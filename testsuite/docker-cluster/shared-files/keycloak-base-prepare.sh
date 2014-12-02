#!/bin/bash

# Copy MySQL driver
cd /tmp
mkdir -p mysql/main && mv /mysql-connector-java-5.1.32.jar mysql/main/
cp /keycloak-docker-cluster/shared-files/mysql-module.xml mysql/main/module.xml
mv mysql $JBOSS_MODULES_HOME/com/

sed -i -e "s/<extensions>/&\n <extension module=\"org.keycloak.keycloak-subsystem\"\/>/" $JBOSS_HOME/standalone/configuration/standalone-ha.xml
sed -i -e 's/<profile>/&\n <subsystem xmlns="urn:jboss:domain:keycloak:1.0"\/>/' $JBOSS_HOME/standalone/configuration/standalone-ha.xml && \
sed -i -e 's/<security-domains>/&\n <security-domain name="keycloak">\n  <authentication>\n   <login-module code="org.keycloak.adapters.jboss.KeycloakLoginModule" flag="required"\/>\n  <\/authentication>\n <\/security-domain>/' $JBOSS_HOME/standalone/configuration/standalone-ha.xml && \
sed -i -e 's/<drivers>/&\n <driver name="mysql" module="com.mysql">\n  <xa-datasource-class>com.mysql.jdbc.Driver<\/xa-datasource-class>\n  <driver-class>com.mysql.jdbc.Driver<\/driver-class>\n <\/driver>/' $JBOSS_HOME/standalone/configuration/standalone-ha.xml && \
sed -i -e 's/<\/periodic-rotating-file-handler>/&\n <logger category=\"org.keycloak\">\n  <level name=\"DEBUG\" \/> \n <\/logger>\n <logger category=\"org.jboss.resteasy.core.ResourceLocator\">\n  <level name=\"ERROR\" \/> \n <\/logger>/' $JBOSS_HOME/standalone/configuration/standalone-ha.xml

sed -i -e 's/<subsystem xmlns=\"urn:jboss:domain:infinispan:[0-9]\.[0-9]\">/&\n <cache-container name=\"keycloak\" jndi-name=\"infinispan\/Keycloak\" start=\"EAGER\"> \
\n  <transport lock-timeout=\"60000\"\/>\n  <distributed-cache name=\"sessions\" mode=\"SYNC\" owners=\"2\" segments=\"60\"\/> \
\n  <distributed-cache name=\"loginFailures\" mode=\"SYNC\" owners=\"2\" segments=\"60\"\/> \
\n  <invalidation-cache name=\"realms\" mode=\"SYNC\"\/>\n \
\n  <invalidation-cache name=\"users\"  mode=\"SYNC\"\/>\n <\/cache-container>/' $JBOSS_HOME/standalone/configuration/standalone-ha.xml

sed -i "s|<mod-cluster-config .*>|<mod-cluster-config advertise-socket=\"modcluster\" proxy-list=\"\$\{httpd.proxyList\}\" proxy-url=\"\/\" balancer=\"mycluster\" advertise=\"false\" connector=\"ajp\" sticky-session=\"true\">|" $JBOSS_HOME/standalone/configuration/standalone-ha.xml

sed -i "s|#JAVA_OPTS=\"\$JAVA_OPTS -agentlib:jdwp=transport=dt_socket|JAVA_OPTS=\"\$JAVA_OPTS -agentlib:jdwp=transport=dt_socket|" $JBOSS_HOME/bin/standalone.conf

cp /keycloak-docker-cluster/shared-files/mysql-keycloak-ds.xml $JBOSS_HOME/standalone/deployments/
