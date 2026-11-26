#!/usr/bin/env bash

JAVA_VERSION=25

rm -f /etc/system-fips
dnf install -y "java-${JAVA_VERSION}-openjdk-devel"
fips-mode-setup --enable --no-bootcfg
fips-mode-setup --check | grep "FIPS mode is enabled."
if [ $? -ne 0 ]; then
  exit 1
fi
export JAVA_HOME="/etc/alternatives/java_sdk_${JAVA_VERSION}"
# JDK 25.0.5+ offers the X25519MLKEM768 hybrid TLS group, which fails in FIPS mode when resolving from Maven Central
export MAVEN_OPTS="$MAVEN_OPTS -Djdk.tls.namedGroups=secp256r1,secp384r1,secp521r1,x25519,ffdhe2048,ffdhe3072"

# Build all dependent modules
./mvnw install -nsu -B -am -pl crypto/default,crypto/fips1402 -DskipTests

./mvnw test -nsu -B -pl crypto/default,crypto/fips1402 -Dcom.redhat.fips=true -Dredhat.crypto-policies=true
if [ $? -ne 0 ]; then
  exit 1
fi

./mvnw test -nsu -B -pl crypto/default,crypto/fips1402 -Dcom.redhat.fips=true -Dredhat.crypto-policies=true -Dorg.bouncycastle.fips.approved_only=true
