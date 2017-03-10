#!/bin/bash

# Import the certificate
cd /opt/jboss/keycloak/InstallCert/
java InstallCert $OSO_ADDRESS << ANSWERS
1
ANSWERS

if [[ -z "${KEYSTORE_PASSWORD}" ]]; then
  KEYSTORE_PASSWORD="almighty"
fi

if [[ -z "${KEYCLOAK_SERVER_DOMAIN}" ]]; then
  KEYCLOAK_SERVER_DOMAIN="localhost"
fi

echo "Export the certificate into the keystore"
keytool -exportcert -alias $OSO_DOMAIN_NAME-1 -keystore jssecacerts -storepass changeit -file $OSO_DOMAIN_NAME.cer

echo "Import certificate into system keystore"
keytool -importcert -alias $OSO_DOMAIN_NAME-1 -keystore $JAVA_HOME/jre/lib/security/cacerts -storepass changeit -file $OSO_DOMAIN_NAME.cer << ANSWERS
yes
ANSWERS

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
keytool -importcert -alias $OSO_DOMAIN_NAME-1 -keystore keycloak.jks -storepass ${KEYSTORE_PASSWORD} -file $OSO_DOMAIN_NAME.cer << ANSWERS
yes
ANSWERS

mv keycloak.jks ../standalone/configuration

cd /opt/jboss/keycloak

# Set the password of the keystore to the configuration file
sed -i -e "s/%%KEYSTORE_PASSWORD%%/${KEYSTORE_PASSWORD}/" ./standalone/configuration/standalone.xml

if [ $KEYCLOAK_USER ] && [ $KEYCLOAK_PASSWORD ]; then
    echo "Adding a new user..."
    /opt/jboss/keycloak/bin/add-user-keycloak.sh --user $KEYCLOAK_USER --password $KEYCLOAK_PASSWORD
fi

echo "Starting keycloak-server..."

exec /opt/jboss/keycloak/bin/standalone.sh $@
exit $?
