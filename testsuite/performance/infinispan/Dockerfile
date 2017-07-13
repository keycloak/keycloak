FROM jboss/infinispan-server:8.2.6.Final
#FROM jboss/infinispan-server:9.1.0.Final

USER root
RUN yum -y install iproute
USER jboss

ENV CONFIGURATION clustered.xml

ADD configs/ ./
ADD *.sh /usr/local/bin/

USER root
RUN chmod -v +x /usr/local/bin/*.sh
USER jboss

RUN $INFINISPAN_SERVER_HOME/bin/ispn-cli.sh --file=add-keycloak-caches.cli; \
    $INFINISPAN_SERVER_HOME/bin/ispn-cli.sh --file=private-interface-for-jgroups-socket-bindings.cli; \
    cd $INFINISPAN_SERVER_HOME/standalone; rm -rf configuration/standalone_xml_history log data tmp

HEALTHCHECK  --interval=5s --timeout=5s --retries=12 CMD ["infinispan-healthcheck.sh"]
ENTRYPOINT [ "docker-entrypoint-custom.sh" ]
