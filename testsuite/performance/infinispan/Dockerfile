FROM jboss/infinispan-server:8.2.6.Final
#FROM jboss/infinispan-server:9.1.0.Final

ARG LOCAL_SITE
ARG REMOTE_SITE

ARG MANAGEMENT_USER
ARG MANAGEMENT_USER_PASS

USER root
RUN yum -y install iproute
USER jboss

ENV CONFIGURATION clustered.xml

ADD configs/ ./
ADD *.sh /usr/local/bin/

RUN sed -e s/%LOCAL_SITE%/$LOCAL_SITE/ -e s/%REMOTE_SITE%/$REMOTE_SITE/ add-keycloak-caches.cli.template > add-keycloak-caches.cli

USER root
RUN chmod -v +x /usr/local/bin/*.sh
USER jboss

RUN if [ ! -z "$MANAGEMENT_USER" ]; then $INFINISPAN_SERVER_HOME/bin/add-user.sh -u $MANAGEMENT_USER -p $MANAGEMENT_USER_PASS ; fi
RUN $INFINISPAN_SERVER_HOME/bin/ispn-cli.sh --file=add-private-network-interface.cli; \
    $INFINISPAN_SERVER_HOME/bin/ispn-cli.sh --file=add-keycloak-caches.cli; \
    cd $INFINISPAN_SERVER_HOME/standalone; rm -rf configuration/standalone_xml_history log data tmp

HEALTHCHECK  --interval=5s --timeout=5s --retries=12 CMD ["infinispan-healthcheck.sh"]
ENTRYPOINT [ "docker-entrypoint-custom.sh" ]
