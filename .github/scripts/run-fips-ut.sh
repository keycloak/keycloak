#!/usr/bin/env bash

rm -f /etc/system-fips
dnf install -y java-25-openjdk-devel crypto-policies-scripts
fips-mode-setup --enable --no-bootcfg
fips-mode-setup --check | grep "FIPS mode is enabled."
if [ $? -ne 0 ]; then
  exit 1
fi
export JAVA_HOME=/etc/alternatives/java_sdk_25

# Build all dependent modules
./mvnw install -nsu -B -am -pl crypto/default,crypto/fips1402 -DskipTests

./mvnw test -nsu -B -pl crypto/default,crypto/fips1402 -Dredhat.crypto-policies=true
if [ $? -ne 0 ]; then
  exit 1
fi

./mvnw test -nsu -B -pl crypto/default,crypto/fips1402 -Dredhat.crypto-policies=true -Dorg.bouncycastle.fips.approved_only=true
