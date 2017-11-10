FROM jboss/wildfly

ADD configs/ ./
ADD *.sh /usr/local/bin/

USER root
RUN chmod -v +x /usr/local/bin/*.sh
RUN yum -y install iproute
USER jboss

RUN $JBOSS_HOME/bin/jboss-cli.sh --file=mod-cluster-balancer.cli
RUN $JBOSS_HOME/bin/jboss-cli.sh --file=undertow.cli
RUN $JBOSS_HOME/bin/jboss-cli.sh --file=io-worker-threads.cli; \
    cd $JBOSS_HOME/standalone; rm -rf configuration/standalone_xml_history log data tmp

HEALTHCHECK  --interval=5s --timeout=5s --retries=12 CMD ["wildfly-healthcheck.sh"]
ENTRYPOINT [ "docker-entrypoint.sh" ]
