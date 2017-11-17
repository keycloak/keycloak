FROM jboss/base-jdk:8

ENV JBOSS_HOME /opt/jboss/keycloak
ARG REMOTE_CACHES=false
WORKDIR $JBOSS_HOME

ENV CONFIGURATION standalone.xml
ENV DEBUG_USER admin
ENV DEBUG_USER_PASSWORD admin

# Enables signals getting passed from startup script to JVM
# ensuring clean shutdown when container is stopped.
ENV LAUNCH_JBOSS_IN_BACKGROUND 1
ENV PROXY_ADDRESS_FORWARDING false

ADD target/keycloak configs/ ./
ADD *.sh /usr/local/bin/

USER root
RUN chown -R jboss .; chgrp -R jboss .; \
    chmod -R -v +x /usr/local/bin/ ; \
    yum install -y epel-release jq iproute && yum clean all

USER jboss
# install mariadb JDBC driver
RUN curl --create-dirs --output modules/system/layers/base/org/mariadb/jdbc/main/mariadb-java-client-2.0.3.jar http://central.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/2.0.3/mariadb-java-client-2.0.3.jar ; \
    $JBOSS_HOME/bin/jboss-cli.sh --file=set-keycloak-ds.cli && \
    $JBOSS_HOME/bin/jboss-cli.sh --file=io-worker-threads.cli && \
    $JBOSS_HOME/bin/jboss-cli.sh --file=undertow.cli && \
    $JBOSS_HOME/bin/jboss-cli.sh --file=modcluster-simple-load-provider.cli && \
    if [ "$REMOTE_CACHES" == "true" ]; then $JBOSS_HOME/bin/jboss-cli.sh --file=add-remote-cache-stores.cli; fi && \
    cd $JBOSS_HOME/standalone; rm -rf configuration/standalone_xml_history log data tmp ; \
    $JBOSS_HOME/bin/add-user.sh -u $DEBUG_USER -p $DEBUG_USER_PASSWORD

ADD module.xml modules/system/layers/base/org/mariadb/jdbc/main/

EXPOSE 8080
EXPOSE 9990
HEALTHCHECK  --interval=5s --timeout=5s --retries=12 CMD ["keycloak-healthcheck.sh"]
ENTRYPOINT ["docker-entrypoint.sh"]