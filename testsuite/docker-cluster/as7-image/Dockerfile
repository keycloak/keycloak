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

ENV AS7_VERSION 7.1.1.Final

# Download and unpack AS7 distribution
RUN cd /opt
RUN wget http://download.jboss.org/jbossas/7.1/jboss-as-$AS7_VERSION/jboss-as-$AS7_VERSION.zip
RUN sleep 3
RUN unzip -q jboss-as-$AS7_VERSION.zip

# Make sure the distribution is available from a well-known place
RUN mv jboss-as-$AS7_VERSION /opt/as7

RUN rm -rf jboss-as-$AS7_VERSION.zip

EXPOSE 8787

CMD [ "/bin/bash" ]
