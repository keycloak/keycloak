#!/bin/bash -x

rm -f /etc/system-fips
dnf install -y java-21-openjdk-devel
fips-mode-setup --enable --no-bootcfg
fips-mode-setup --is-enabled
if [ $? -ne 0 ]; then
  exit 1
fi
STRICT_OPTIONS=""
TESTSUITE_NAME="FipsNonStrictTestSuite"
if [ "$1" = "strict" ]; then
  STRICT_OPTIONS="-Dauth.server.fips.mode=strict -Dauth.server.supported.keystore.types=BCFKS -Dauth.server.keystore.type=bcfks -Dauth.server.supported.rsa.key.sizes=2048,3072,4096"
  TESTSUITE_NAME="FipsStrictTestSuite"
fi
echo "STRICT_OPTIONS: $STRICT_OPTIONS"
TESTS=`testsuite/integration-arquillian/tests/base/testsuites/suite.sh fips`
echo "Tests: $TESTS"
export JAVA_HOME=/etc/alternatives/java_sdk_21
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
./mvnw test -Dsurefire.rerunFailingTestsCount=$SUREFIRE_RERUN_FAILING_COUNT -nsu -B -Pauth-server-quarkus,auth-server-fips140-2,app-server-wildfly -Dcom.redhat.fips=false $STRICT_OPTIONS -Dtest=$TESTS -pl testsuite/integration-arquillian/tests/base 2>&1 | misc/log/trimmer.sh

# New Base Tests
./mvnw package -nsu -B -Dcom.redhat.fips=false -Dtest=$TESTSUITE_NAME -pl tests/base
