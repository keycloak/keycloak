#!/usr/bin/env bash

#curl https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.deb --output java.deb
#curl https://download.oracle.com/java/20/archive/jdk-20.0.2_linux-x64_bin.deb --output java.deb
#curl https://download.java.net/java/GA/jdk20/GPL/openjdk-20_linux-x64_bin.tar.gz --output java.tar.gz

curl -O https://download.java.net/java/GA/jdk20/GPL/openjdk-20_linux-x64_bin.tar.gz

#apt install -y libasound2 libc6-i386 libc6-x32 libfreetype6 libxi6 libxrender1 libxtst6

tar -xzf openjdk-20_linux-x64_bin.tar.gz 

export JAVA_HOME=$(pwd)/jdk-20

export PATH=$JAVA_HOME/bin:$PATH

./mvnw -pl quarkus/deployment,quarkus/dist,themes, -am -DskipTests clean install | tee log-$(date +%H-%M-%y-%m-%d).txt
