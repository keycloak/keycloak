#!/usr/bin/env bash

#curl https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.deb --output java.deb
#curl https://download.oracle.com/java/20/archive/jdk-20.0.2_linux-x64_bin.deb --output java.deb
#curl https://download.java.net/java/GA/jdk20/GPL/openjdk-20_linux-x64_bin.tar.gz --output java.tar.gz

#curl -O https://download.java.net/java/GA/jdk20/GPL/openjdk-20_linux-x64_bin.tar.gz

#curl https://download.oracle.com/java/20/archive/jdk-20_linux-x64_bin.deb --output java.deb

curl https://download.java.net/java/GA/jdk20.0.2/6e380f22cbe7469fa75fb448bd903d8e/9/GPL/openjdk-20.0.2_linux-x64_bin.tar.gz --output jdk.tar.gz

#apt install -y libasound2 libc6-i386 libc6-x32 libfreetype6 libxi6 libxrender1 libxtst6

echo "--1--"

#dpkg -i java.deb

tar -xzvf jdk.tar.gz

export JAVA_HOME=$(pwd)/jdk-20.0.2

export PATH=$JAVA_HOME/bin:$PATH

echo "--2-- $(which javac)"
echo "--3-- $(readlink -f $( which javac ))"

./mvnw -pl quarkus/deployment,quarkus/dist,themes, -am -DskipTests clean install | tee log-$(date +%H-%M-%y-%m-%d).txt

echo "Running build command for MSQL database"

java -jar quarkus/server/target/lib/quarkus-run.jar build --db=mysql