FROM jboss/base-jdk:8

ENV KEYCLOAK_COMMIT_SHA b5b0f433ac84f12259dac66f8665445db7a24d68
ENV KEYCLOAK_VERSION 3.0.0.CR1-SNAPSHOT
# Enables signals getting passed from startup script to JVM
# ensuring clean shutdown when container is stopped.
ENV LAUNCH_JBOSS_IN_BACKGROUND 1

MAINTAINER Hector Fernandez <hfernand@redhat.com>

USER root

RUN yum install -y epel-release jq && yum clean all

USER jboss

ADD ../distribution/server-dist/target/keycloak-$KEYCLOAK_VERSION.tar.gz /opt/jboss/
RUN mv /opt/jboss/keycloak-$KEYCLOAK_VERSION /opt/jboss/keycloak

ADD docker-entrypoint.sh /opt/jboss/
ADD setLogLevel.xsl /opt/jboss/keycloak/
RUN java -jar /usr/share/java/saxon.jar -s:/opt/jboss/keycloak/standalone/configuration/standalone.xml -xsl:/opt/jboss/keycloak/setLogLevel.xsl -o:/opt/jboss/keycloak/standalone/configuration/standalone.xml

ENV JBOSS_HOME /opt/jboss/keycloak

EXPOSE 8080
ENTRYPOINT [ "/opt/jboss/docker-entrypoint.sh" ]

CMD ["-b", "0.0.0.0"]
