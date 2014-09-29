FROM fedora:20

RUN cd /
RUN yum -y install wget && yum -y install unzip
RUN yum clean all

RUN wget https://dl.dropboxusercontent.com/u/5525920/apache24-modcluster131.zip
RUN unzip -q apache24-modcluster131.zip

ADD httpd-run /bin/httpd-run
RUN chmod u+x /bin/httpd-run

ADD httpd.conf  /opt/jboss/httpd/httpd/conf/httpd.conf

EXPOSE 80 10001
CMD ["/bin/httpd-run"]
