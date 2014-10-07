FROM jboss/wildfly

USER root

# Update yum and install required programs
RUN yum install -y unzip && yum install -y wget && yum install -y mc && yum -y install nc
RUN yum clean all

# Download mysql driver
RUN cd /tmp
RUN wget http://search.maven.org/remotecontent?filepath=mysql/mysql-connector-java/5.1.32/mysql-connector-java-5.1.32.jar
RUN mv *.jar /mysql-connector-java-5.1.32.jar

ADD keycloak-wildfly-trigger.sh /keycloak-wildfly-trigger.sh
RUN chmod u+x /keycloak-wildfly-trigger.sh

ENV JBOSS_HOME /opt/wildfly-8.1.0.Final
ENV JBOSS_MODULES_HOME $JBOSS_HOME/modules/system/layers/base
ENV JBOSS_TYPE wildfly
ENV NODE_PREFIX wf

EXPOSE 8787

CMD [ "/keycloak-wildfly-trigger.sh" ]

