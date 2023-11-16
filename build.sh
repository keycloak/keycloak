#!/usr/bin/env bash

echo "B_"

#curl https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.deb --output java.deb
curl https://download.oracle.com/java/20/archive/jdk-20.0.2_linux-x64_bin.deb --output java.deb

dpkg -i java.deb

javac helloworld.java

java HelloWorld

cat file1_

#export JAVA_HOME=/usr/local/openjdk-20

./mvnw -pl quarkus/deployment,quarkus/dist,themes, -am -DskipTests clean install | tee log-$(date +%H-%M-%y-%m-%d).txt
