#!/usr/bin/env bash

#curl https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.deb --output java.deb
curl https://download.oracle.com/java/20/archive/jdk-20.0.2_linux-x64_bin.deb --output java.deb

apt install libasound2 libc6-i386 libc6-x32 libfreetype6 libxi6 libxrender1 libxtst6

dpkg -i java.deb

./mvnw -pl quarkus/deployment,quarkus/dist,themes, -am -DskipTests clean install | tee log-$(date +%H-%M-%y-%m-%d).txt
