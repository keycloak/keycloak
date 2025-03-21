#!/bin/bash

set -o pipefail

echo "Executing federation-sssd-setup.sh to prepare SSSD and PAM"
quarkus/dist/src/main/content/bin/federation-sssd-setup.sh

if [[ "true" == "$1" ]]; then
  echo "Adding users and groups for the test"

  printf "%b" "password\n" | kinit admin
  ipa group-add --desc='test group' testgroup
  ipa user-add emily --first=Emily --last=Jones --email=Emily@jones.com --random
  ipa group-add-member testgroup --users=emily
  ipa user-add bart --first=bart --last=bart --email= --random
  ipa user-add david --first=david --last=david --random
  kdestroy

  ldapmodify -D "cn=Directory Manager" -w password <<EOF
dn: uid=emily,cn=users,cn=accounts,dc=example,dc=test
changetype: modify
replace: userpassword
userpassword: emily123

dn: uid=bart,cn=users,cn=accounts,dc=example,dc=test
changetype: modify
replace: userpassword
userpassword: bart123

dn: uid=david,cn=users,cn=accounts,dc=example,dc=test
changetype: modify
replace: userpassword
userpassword: david123

EOF

  printf "%b" "password\n" | kinit admin
  ipa user-disable david
  kdestroy
fi

echo "Installing jdk-21 in the container"
dnf install -y java-21-openjdk-devel
export JAVA_HOME=/etc/alternatives/java_sdk_21

echo "Building quarkus keyclok server with SSSD integration"
./mvnw install -nsu -B -e -pl testsuite/integration-arquillian/servers/auth-server/quarkus -Pauth-server-quarkus

echo "Executing SSSD tests"
./mvnw -f testsuite/integration-arquillian/tests/other/sssd/pom.xml test -Psssd-testing -Pauth-server-quarkus
