#!/bin/bash -x

dnf install -y java-21-openjdk-devel
fips-mode-setup --enable --no-bootcfg
fips-mode-setup --is-enabled
if [ $? -ne 0 ]; then
  exit 1
fi
STRICT_OPTIONS=""
if [ "$1" = "strict" ]; then
  STRICT_OPTIONS="-Dauth.server.fips.mode=strict -Dauth.server.supported.keystore.types=BCFKS -Dauth.server.keystore.type=bcfks -Dauth.server.supported.rsa.key.sizes=2048,4096"
fi
echo "STRICT_OPTIONS: $STRICT_OPTIONS"
TESTS=`testsuite/integration-arquillian/tests/base/testsuites/suite.sh fips`
echo "Tests: $TESTS"
export JAVA_HOME=/etc/alternatives/java_sdk_21
set -o pipefail

# Build adapter distributions
./mvnw install -DskipTests -f distribution/pom.xml
if [ $? -ne 0 ]; then
  exit 1
fi

# Build app servers
./mvnw install -DskipTests -Pbuild-app-servers -f testsuite/integration-arquillian/servers/app-server/pom.xml
if [ $? -ne 0 ]; then
  exit 1
fi

# Prepare Quarkus distribution with BCFIPS
./mvnw install -e -pl testsuite/integration-arquillian/servers/auth-server/quarkus -Pauth-server-quarkus,auth-server-fips140-2
if [ $? -ne 0 ]; then
  exit 1
fi

# Profile app-server-wildfly needs to be explicitly set for FIPS tests
./mvnw test -Dsurefire.rerunFailingTestsCount=$SUREFIRE_RERUN_FAILING_COUNT -nsu -B -Pauth-server-quarkus,auth-server-fips140-2,app-server-wildfly -Dcom.redhat.fips=false $STRICT_OPTIONS -Dtest=$TESTS -pl testsuite/integration-arquillian/tests/base 2>&1 | misc/log/trimmer.sh
