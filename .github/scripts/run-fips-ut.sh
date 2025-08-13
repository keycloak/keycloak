#!/bin/bash

rm -f /etc/system-fips
dnf install -y java-21-openjdk-devel crypto-policies-scripts
fips-mode-setup --enable --no-bootcfg
fips-mode-setup --is-enabled
if [ $? -ne 0 ]; then
  exit 1
fi
export JAVA_HOME=/etc/alternatives/java_sdk_21

# Build all dependent modules
./mvnw install -nsu -B -am -pl crypto/default,crypto/fips1402 -DskipTests

./mvnw test -nsu -B -pl crypto/default,crypto/fips1402 -Dcom.redhat.fips=true
if [ $? -ne 0 ]; then
  exit 1
fi
./mvnw test -nsu -B -pl crypto/default,crypto/fips1402 -Dcom.redhat.fips=true -Dorg.bouncycastle.fips.approved_only=true
