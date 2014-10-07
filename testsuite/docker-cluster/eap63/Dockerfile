FROM mposolda/eap63

ADD keycloak-eap63-trigger.sh /keycloak-eap63-trigger.sh
RUN chmod u+x /keycloak-eap63-trigger.sh

ENV JBOSS_HOME /opt/eap63
ENV JBOSS_MODULES_HOME $JBOSS_HOME/modules/system/layers/base
ENV JBOSS_TYPE eap63
ENV NODE_PREFIX eap

EXPOSE 8787

CMD [ "/keycloak-eap63-trigger.sh" ]