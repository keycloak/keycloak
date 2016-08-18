#!/bin/bash

export DIRECT_GRANT_RESPONSE=$(curl -i --request POST http://localhost:8080/auth/realms/master/protocol/openid-connect/token --header "Accept: application/json" --header "Content-Type: application/x-www-form-urlencoded" --data "grant_type=password&username=admin&password=admin&client_id=admin-cli")

echo -e "\n\nSENT RESOURCE-OWNER-PASSWORD-CREDENTIALS-REQUEST. OUTPUT IS:\n\n";
echo $DIRECT_GRANT_RESPONSE;

export ACCESS_TOKEN=$(echo $DIRECT_GRANT_RESPONSE | grep "access_token" | sed 's/.*\"access_token\":\"\([^\"]*\)\".*/\1/g');
echo -e "\n\nACCESS TOKEN IS \"$ACCESS_TOKEN\"";

echo -e "\n\nSENDING UN-AUTHENTICATED REQUEST. THIS SHOULD FAIL WITH 401: ";
curl -i --request POST http://localhost:8080/auth/realms/master/example/companies-auth --data "{ \"name\": \"auth foo company\" }" --header "Content-type: application/json"

echo -e "\n\nSENDING AUTHENTICATED REQUEST. THIS SHOULD SUCCESSFULY CREATE COMPANY AND SUCCESS WITH 201: ";
curl -i --request POST http://localhost:8080/auth/realms/master/example/companies-auth --data "{ \"name\": \"auth foo company\" }" --header "Content-type: application/json" --header "Authorization: Bearer $ACCESS_TOKEN";

echo -e "\n\nSEARCH COMPANIES: ";
curl -i --request GET http://localhost:8080/auth/realms/master/example/companies-auth --header "Accept: application/json" --header "Authorization: Bearer $ACCESS_TOKEN";

