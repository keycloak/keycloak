FROM mposolda/eap64

ADD keycloak-eap6-trigger.sh /keycloak-eap6-trigger.sh
RUN chmod u+x /keycloak-eap6-trigger.sh

# TODO: REmove as it's in base EAP64 image already
RUN cd /tmp
RUN wget http://search.maven.org/remotecontent?filepath=mysql/mysql-connector-java/5.1.32/mysql-connector-java-5.1.32.jar
RUN mv mysql-connector-*.jar /

ENV JBOSS_HOME /opt/eap6
ENV JBOSS_MODULES_HOME $JBOSS_HOME/modules/system/layers/base
ENV JBOSS_TYPE eap6
ENV NODE_PREFIX eap

EXPOSE 8787

CMD [ "/keycloak-eap6-trigger.sh" ]