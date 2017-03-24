FROM jboss/base-jdk:8
MAINTAINER Hector Fernandez <hfernand@redhat.com>

# Enables signals getting passed from startup script to JVM
# ensuring clean shutdown when container is stopped.
ENV LAUNCH_JBOSS_IN_BACKGROUND 1

ENV KEYCLOAK_VERSION 3.0.0.Final
ENV OPERATING_MODE clustered

ENV OSO_ADDRESS tsrv.devshift.net:8443
ENV OSO_DOMAIN_NAME tsrv.devshift.net

USER root

RUN yum install -y epel-release jq git && yum clean all

ENV JBOSS_HOME /opt/jboss/keycloak

ADD keycloak-$KEYCLOAK_VERSION.tar.gz /opt/jboss/
RUN mkdir -p $JBOSS_HOME && cd /opt/jboss/keycloak-$KEYCLOAK_VERSION && cp -R * $JBOSS_HOME

WORKDIR $JBOSS_HOME

ADD install_certificate.sh /opt/jboss/keycloak/
RUN /opt/jboss/keycloak/install_certificate.sh

ADD standalone.xml /opt/jboss/keycloak/standalone/configuration
ADD standalone-ha.xml /opt/jboss/keycloak/standalone/configuration

RUN chown -R 1000:0 ${JBOSS_HOME} && chmod -R ug+rw ${JBOSS_HOME}

USER jboss

ADD docker-entrypoint.sh /opt/jboss/

ADD setLogLevel.xsl /opt/jboss/keycloak/
RUN java -jar /usr/share/java/saxon.jar -s:/opt/jboss/keycloak/standalone/configuration/standalone.xml -xsl:/opt/jboss/keycloak/setLogLevel.xsl -o:/opt/jboss/keycloak/standalone/configuration/standalone.xml && \
    java -jar /usr/share/java/saxon.jar -s:/opt/jboss/keycloak/standalone/configuration/standalone-ha.xml -xsl:/opt/jboss/keycloak/setLogLevel.xsl -o:/opt/jboss/keycloak/standalone/configuration/standalone-ha.xml && \
    rm /opt/jboss/keycloak/setLogLevel.xsl

ADD changeDatabase.xsl /opt/jboss/keycloak/
RUN java -jar /usr/share/java/saxon.jar -s:/opt/jboss/keycloak/standalone/configuration/standalone.xml -xsl:/opt/jboss/keycloak/changeDatabase.xsl -o:/opt/jboss/keycloak/standalone/configuration/standalone.xml && \
    java -jar /usr/share/java/saxon.jar -s:/opt/jboss/keycloak/standalone/configuration/standalone-ha.xml -xsl:/opt/jboss/keycloak/changeDatabase.xsl -o:/opt/jboss/keycloak/standalone/configuration/standalone-ha.xml && \
    rm /opt/jboss/keycloak/changeDatabase.xsl

RUN mkdir -p /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main; cd /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main; curl -O http://central.maven.org/maven2/org/postgresql/postgresql/9.3-1102-jdbc3/postgresql-9.3-1102-jdbc3.jar
ADD module.xml /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main/

EXPOSE 8080
ENTRYPOINT [ "/opt/jboss/docker-entrypoint.sh" ]

CMD ["--debug", "-b", "0.0.0.0"]
