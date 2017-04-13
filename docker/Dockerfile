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

# Install little pcp pmcd server for metrics collection
# would prefer only pmcd, and not the /bin/pm*tools etc.
COPY pcp.repo /etc/yum.repos.d/pcp.repo
RUN yum install -y pcp && yum clean all && \
    mkdir -p /etc/pcp /var/run/pcp /var/lib/pcp /var/log/pcp && \
    chown -R 1000 /etc/pcp /var/run/pcp /var/lib/pcp /var/log/pcp && \
    chmod -R ug+rw /etc/pcp /var/run/pcp /var/lib/pcp /var/log/pcp
COPY ./keycloak+pmcd.sh /keycloak+pmcd.sh
EXPOSE 44321

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

RUN sed -ie 's@\(</dependencies>\)@    <module name="org.postgresql.jdbc"/>\n    \1@' /opt/jboss/keycloak/modules/system/layers/base/org/jgroups/main/module.xml

EXPOSE 8080
ENTRYPOINT [ "/keycloak+pmcd.sh" ]

CMD ["--debug", "-b", "0.0.0.0"]
