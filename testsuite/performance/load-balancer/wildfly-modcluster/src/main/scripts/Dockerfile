FROM jboss/base-jdk:8

ENV JBOSS_HOME /opt/jboss/wildfly
WORKDIR $JBOSS_HOME

ENV CONFIGURATION standalone.xml

# Ensure signals are forwarded to the JVM process correctly for graceful shutdown
ENV LAUNCH_JBOSS_IN_BACKGROUND 1
ENV JSTAT false

USER root
RUN yum -y install iproute

ADD wildfly ./
ADD *.sh /usr/local/bin/

RUN chown -R jboss . ;\
    chgrp -R jboss . ;\
    chmod -R -v ug+x bin/*.sh ;\
    chmod -R -v ug+x /usr/local/bin/ 

USER jboss

EXPOSE 8080
EXPOSE 9990
HEALTHCHECK  --interval=5s --timeout=5s --retries=12 CMD ["wildfly-healthcheck.sh"]
ENTRYPOINT ["docker-entrypoint.sh"]
