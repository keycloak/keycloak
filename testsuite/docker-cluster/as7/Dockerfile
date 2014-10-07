FROM mposolda/as7

ADD keycloak-as7-trigger.sh /keycloak-as7-trigger.sh
RUN chmod u+x /keycloak-as7-trigger.sh

ENV JBOSS_HOME /opt/as7
ENV JBOSS_MODULES_HOME $JBOSS_HOME/modules
ENV JBOSS_TYPE as7
ENV NODE_PREFIX as

EXPOSE 8787

CMD [ "/keycloak-as7-trigger.sh" ]