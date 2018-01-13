FROM jboss/base-jdk:8

ADD Advertise.java .
RUN javac Advertise.java

ENTRYPOINT ["java", "Advertise"]
CMD ["224.0.1.105", "23364"]
