#!/bin/bash

dnf install -y java-17-openjdk-devel crypto-policies-scripts
fips-mode-setup --enable --no-bootcfg
fips-mode-setup --is-enabled
if [ $? -ne 0 ]; then
  exit 1
fi
echo "fips.provider.7=XMLDSig" >>/etc/alternatives/java_sdk_17/conf/security/java.security
export JAVA_HOME=/etc/alternatives/java_sdk_17
./mvnw test -nsu -B -am -pl crypto/default,crypto/fips1402 -Dcom.redhat.fips=true
if [ $? -ne 0 ]; then
  exit 1
fi
./mvnw test -nsu -B -am -pl crypto/default,crypto/fips1402 -Dcom.redhat.fips=true -Dorg.bouncycastle.fips.approved_only=true
