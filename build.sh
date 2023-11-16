#!/usr/bin/env bash

echo "B_"

which javac >> file1_ 2>&1

echo "____" | tee file1_

which java >> file1_ 2>&1

cat file1_

export JAVA_HOME=/usr/local/openjdk-20

./mvnw -pl quarkus/deployment,quarkus/dist,themes, -am -DskipTests clean install | tee log-$(date +%H-%M-%y-%m-%d).txt
