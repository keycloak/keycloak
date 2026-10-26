#!/usr/bin/env bash
set -x

JAVA_VERSION=25

rm -f /etc/system-fips
dnf install -y "java-${JAVA_VERSION}-openjdk-devel"
fips-mode-setup --enable --no-bootcfg
fips-mode-setup --check | grep "FIPS mode is enabled."
if [ $? -ne 0 ]; then
  exit 1
fi
STRICT_OPTIONS=""
TESTSUITE_NAME="FipsNonStrictTestSuite"
if [ "$1" = "strict" ]; then
  STRICT_OPTIONS="-Dauth.server.fips.mode=strict -Dauth.server.supported.keystore.types=BCFKS -Dauth.server.keystore.type=bcfks -Dauth.server.supported.rsa.key.sizes=2048,3072,4096"
  TESTSUITE_NAME="FipsStrictTestSuite"
else
  # RH OpenJDK ships cacerts as JKS, but merging the default truststore assumes PKCS12, which BCFIPS rejects in non-strict mode
  # Remove or modify once a proper fix is merged eg https://github.com/keycloak/keycloak/pull/54171
  export JAVA_OPTS_APPEND="-Djavax.net.ssl.trustStore=/etc/alternatives/java_sdk_${JAVA_VERSION}/lib/security/cacerts -Djavax.net.ssl.trustStoreType=JKS -Djavax.net.ssl.trustStorePassword=changeit"
fi
echo "STRICT_OPTIONS: $STRICT_OPTIONS"
TESTS=`testsuite/integration-arquillian/tests/base/testsuites/suite.sh fips`
if [ $? -ne 0 ]; then
  echo "Invalid tests to execute: $TESTS"
  exit 1
fi
echo "Tests: $TESTS"
export JAVA_HOME="/etc/alternatives/java_sdk_${JAVA_VERSION}"
set -o pipefail

# Build adapter distributions
./mvnw -B install -DskipTests -f distribution/pom.xml
if [ $? -ne 0 ]; then
  exit 1
fi

# Build app servers
./mvnw -B install -DskipTests -Pbuild-app-servers -f testsuite/integration-arquillian/servers/app-server/pom.xml
if [ $? -ne 0 ]; then
  exit 1
fi

# Prepare Quarkus distribution with BCFIPS
./mvnw -B install -e -pl testsuite/integration-arquillian/servers/auth-server/quarkus -Pauth-server-quarkus,auth-server-fips140-2
if [ $? -ne 0 ]; then
  exit 1
fi

# Profile app-server-wildfly needs to be explicitly set for FIPS tests
./mvnw test -Dsurefire.rerunFailingTestsCount=$SUREFIRE_RERUN_FAILING_COUNT -nsu -B -Pauth-server-quarkus,auth-server-fips140-2,app-server-wildfly -Dcom.redhat.fips=false -Dredhat.crypto-policies=false $STRICT_OPTIONS -Dtest=$TESTS -pl testsuite/integration-arquillian/tests/base 2>&1 | misc/log/trimmer.sh
if [ $? -ne 0 ]; then
  exit 1
fi

# New Base Tests
./mvnw package -nsu -B -Dcom.redhat.fips=false -Dredhat.crypto-policies=false -Dtest=$TESTSUITE_NAME -pl tests/base
