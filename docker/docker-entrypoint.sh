#!/bin/bash

if [[ -z "${KEYSTORE_PASSWORD}" ]]; then
  KEYSTORE_PASSWORD="almighty"
fi

if [[ -z "${KEYCLOAK_SERVER_DOMAIN}" ]]; then
  KEYCLOAK_SERVER_DOMAIN="localhost"
fi

echo "Create keycloak store"
keytool -genkey -alias ${KEYCLOAK_SERVER_DOMAIN} -keyalg RSA -keystore keycloak.jks -validity 10950 -keypass $KEYSTORE_PASSWORD -storepass $KEYSTORE_PASSWORD << ANSWERS
${KEYCLOAK_SERVER_DOMAIN}
Keycloak
Red Hat
Westford
MA
US
yes
ANSWERS

echo "Import certificate to keycloak store"
keytool -importcert -trustcacerts -alias $OSO_DOMAIN_NAME-1 -keystore keycloak.jks -storepass ${KEYSTORE_PASSWORD} -file /opt/jboss/keycloak/InstallCert/$OSO_DOMAIN_NAME.cer << ANSWERS
yes
ANSWERS

mv keycloak.jks ./standalone/configuration

# Set the password of the keystore to the configuration file
sed -i -e "s/%%KEYSTORE_PASSWORD%%/${KEYSTORE_PASSWORD}/" ./standalone/configuration/standalone.xml
sed -i -e "s/%%KEYSTORE_PASSWORD%%/${KEYSTORE_PASSWORD}/" ./standalone/configuration/standalone-ha.xml

if [ $KEYCLOAK_USER ] && [ $KEYCLOAK_PASSWORD ]; then
    echo "Adding a new user..."
    /opt/jboss/keycloak/bin/add-user-keycloak.sh --user $KEYCLOAK_USER --password $KEYCLOAK_PASSWORD
fi


if [[ "${OPERATING_MODE}" == "clustered" ]]; then
  echo "Starting keycloak-server on clustered mode..."
  exec /opt/jboss/keycloak/bin/standalone.sh --server-config=standalone-ha.xml $@
else
  echo "Starting keycloak-server on standalone mode..."
  exec /opt/jboss/keycloak/bin/standalone.sh $@
fi
exit $?
