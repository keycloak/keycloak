FROM jboss/base-jdk:8

ENV JBOSS_HOME /opt/jboss/keycloak
WORKDIR $JBOSS_HOME

ENV CONFIGURATION standalone.xml

# Enables signals getting passed from startup script to JVM
# ensuring clean shutdown when container is stopped.
ENV LAUNCH_JBOSS_IN_BACKGROUND 1
ENV PROXY_ADDRESS_FORWARDING false
ENV JSTAT false

USER root
RUN yum install -y epel-release jq iproute && yum clean all

ADD keycloak ./
ADD *.sh /usr/local/bin/

USER root
RUN chown -R jboss .; chgrp -R jboss .; chmod -R -v ug+x bin/*.sh ; \
    chmod -R -v +x /usr/local/bin/ 

USER jboss

EXPOSE 8080
EXPOSE 9990
HEALTHCHECK  --interval=5s --timeout=5s --retries=12 CMD ["keycloak-healthcheck.sh"]
ENTRYPOINT ["docker-entrypoint.sh"]
