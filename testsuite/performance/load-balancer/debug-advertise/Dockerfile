FROM jboss/base-jdk:8

ADD SAdvertise.java .
RUN javac SAdvertise.java

ENTRYPOINT ["java", "SAdvertise"]
CMD ["0.0.0.0", "224.0.1.105", "23364"]
