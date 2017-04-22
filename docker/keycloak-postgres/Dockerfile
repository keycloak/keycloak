FROM almighty/keycloak:latest

ADD changeDatabase.xsl /opt/jboss/keycloak/

RUN java -jar /usr/share/java/saxon.jar -s:/opt/jboss/keycloak/standalone/configuration/standalone.xml -xsl:/opt/jboss/keycloak/changeDatabase.xsl -o:/opt/jboss/keycloak/standalone/configuration/standalone.xml; java -jar /usr/share/java/saxon.jar -s:/opt/jboss/keycloak/standalone/configuration/standalone-ha.xml -xsl:/opt/jboss/keycloak/changeDatabase.xsl -o:/opt/jboss/keycloak/standalone/configuration/standalone-ha.xml; rm /opt/jboss/keycloak/changeDatabase.xsl

RUN mkdir -p /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main; cd /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main; curl -O http://central.maven.org/maven2/org/postgresql/postgresql/9.3-1102-jdbc3/postgresql-9.3-1102-jdbc3.jar

ADD module.xml /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main/
