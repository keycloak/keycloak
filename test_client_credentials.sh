#!/usr/bin/env bash

echo "Creating a token for Keycloak to reach Kube (with default audience)"

kubectl create token default > /tmp/token

echo "Creating a token for Client Credentials Grant"
kubectl create token --audience "http://localhost:8081/auth/realms/master" default > /tmp/token_client_credentials


echo "Using a Signed JWT to access Keycloak"

curl -v -X POST "http://localhost:8081/auth/realms/master/protocol/openid-connect/token" \
-H "Content-Type: application/x-www-form-urlencoded" \
--data-urlencode "grant_type=client_credentials" \
--data-urlencode "client_id=signed-jwt-test" \
--data-urlencode "client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer" \
--data-urlencode "client_assertion=$(cat /tmp/token_client_credentials)"



curl -v -X POST "http://example-kc-service:8080/auth/realms/master/protocol/openid-connect/token" \
-H "Content-Type: application/x-www-form-urlencoded" \
--data-urlencode "grant_type=client_credentials" \
--data-urlencode "client_id=signed-jwt-test" \
--data-urlencode "client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer" \
--data-urlencode "client_assertion=$(cat /run/secrets/tokens/test-aud-token)"