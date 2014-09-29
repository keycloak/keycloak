FROM jboss/wildfly

USER root
RUN yum install -y unzip && yum install -y wget && yum install -y mc && yum -y install nc
RUN yum clean all

RUN cd /tmp
RUN wget http://search.maven.org/remotecontent?filepath=mysql/mysql-connector-java/5.1.32/mysql-connector-java-5.1.32.jar
RUN mv *.jar mysql-connector-java-5.1.32.jar

RUN mkdir -p mysql/main && mv mysql-connector-java-5.1.32.jar mysql/main/ 
ADD mysql-module.xml mysql/main/module.xml
RUN mv mysql /opt/wildfly/modules/system/layers/base/com/

RUN sed -i -e 's/<extensions>/&\n <extension module="org.keycloak.keycloak-wildfly-subsystem"\/>/' /opt/wildfly/standalone/configuration/standalone-ha.xml && \
sed -i -e 's/<profile>/&\n <subsystem xmlns="urn:jboss:domain:keycloak:1.0"\/>/' /opt/wildfly/standalone/configuration/standalone-ha.xml && \
sed -i -e 's/<security-domains>/&\n <security-domain name="keycloak">\n  <authentication>\n   <login-module code="org.keycloak.adapters.jboss.KeycloakLoginModule" flag="required"\/>\n  <\/authentication>\n <\/security-domain>/' /opt/wildfly/standalone/configuration/standalone-ha.xml && \
sed -i -e 's/<drivers>/&\n <driver name="mysql" module="com.mysql">\n  <xa-datasource-class>com.mysql.jdbc.Driver<\/xa-datasource-class>\n <\/driver>/' /opt/wildfly/standalone/configuration/standalone-ha.xml && \
sed -i -e 's/<\/periodic-rotating-file-handler>/&\n <logger category=\"org.keycloak\">\n  <level name=\"DEBUG\" \/> \n <\/logger>/' /opt/wildfly/standalone/configuration/standalone-ha.xml

RUN sed -i -e 's/<subsystem xmlns=\"urn:jboss:domain:infinispan:2\.0\">/&\n <cache-container name=\"keycloak\" jndi-name=\"infinispan\/Keycloak\" start=\"EAGER\"> \
\n  <transport lock-timeout=\"60000\"\/>\n  <distributed-cache name=\"sessions\" mode=\"SYNC\" owners=\"2\" segments=\"60\"\/> \
\n  <invalidation-cache name=\"realms\" mode=\"SYNC\"\/>\n <\/cache-container>/' /opt/wildfly/standalone/configuration/standalone-ha.xml

RUN sed -i "s|<mod-cluster-config .*>|<mod-cluster-config advertise-socket=\"modcluster\" proxy-list=\"\$\{httpd.proxyList\}\" proxy-url=\"\/\" balancer=\"mycluster\" advertise=\"false\" connector=\"ajp\" sticky-session=\"true\">|" /opt/wildfly/standalone/configuration/standalone-ha.xml

RUN sed -i "s|#JAVA_OPTS=\"\$JAVA_OPTS -agentlib:jdwp=transport=dt_socket|JAVA_OPTS=\"\$JAVA_OPTS -agentlib:jdwp=transport=dt_socket|" /opt/wildfly/bin/standalone.conf

ADD mysql-keycloak-ds.xml /opt/wildfly/standalone/deployments/
ADD keycloak-run-node.sh /keycloak-run-node.sh
RUN chmod u+x /keycloak-run-node.sh

EXPOSE 8787

CMD [ "/keycloak-run-node.sh" ]
