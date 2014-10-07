FROM jboss/wildfly

USER root

# Update yum and install required programs
RUN yum install -y unzip && yum install -y wget && yum install -y mc && yum -y install nc
RUN yum clean all

# Download mysql driver
RUN cd /tmp
RUN wget http://search.maven.org/remotecontent?filepath=mysql/mysql-connector-java/5.1.32/mysql-connector-java-5.1.32.jar
RUN mv *.jar mysql-connector-java-5.1.32.jar

# Drop wildfly
RUN rm -rf /opt/wildfly*

# Download and unpack EAP63 distribution TODO: Check if it's an issue for EAP 6.3
RUN cd /
RUN wget https://dl.dropboxusercontent.com/u/5525920/jboss-eap-6.3.0.zip
RUN sleep 3
RUN unzip -q jboss-eap-6.3.0.zip

# Make sure the distribution is available from a well-known place
RUN mv jboss-eap-6.3 /opt/eap63

RUN rm -rf jboss-eap-6.3.0.zip

EXPOSE 8787

CMD [ "/bin/bash" ]