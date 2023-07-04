#!/bin/bash

set -o pipefail

echo "Modifying /etc/sssd/sssd.conf file"
if ! grep -q ^ldap_user_extra_attrs /etc/sssd/sssd.conf; then
  sed -i '/ldap_tls_cacert/a ldap_user_extra_attrs = mail:mail, sn:sn, givenname:givenname, telephoneNumber:telephoneNumber' /etc/sssd/sssd.conf
fi
if ! grep -q ^user_attributes /etc/sssd/sssd.conf; then
  sed -i '/allowed_uids/a user_attributes = +mail, +telephoneNumber, +givenname, +sn' /etc/sssd/sssd.conf
fi

systemctl restart sssd
sss_cache -E

echo "Creating /etc/pam.d/keycloak file for PAM"
cat >/etc/pam.d/keycloak <<EOF
auth    required   pam_sss.so
account required   pam_sss.so
EOF

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

echo "Installing jdk-17 in the container"
dnf install -y java-17-openjdk-devel
export JAVA_HOME=/etc/alternatives/java_sdk_17

echo "Building quarkus keyclok server with SSSD integration"
./mvnw install -nsu -B -e -pl testsuite/integration-arquillian/servers/auth-server/quarkus -Pauth-server-quarkus

echo "Executing SSSD tests"
./mvnw -f testsuite/integration-arquillian/tests/other/sssd/pom.xml test -Psssd-testing -Pauth-server-quarkus
