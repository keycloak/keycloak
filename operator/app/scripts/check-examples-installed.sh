#! /bin/bash
set -euxo pipefail

max_retries=500
c=0
while [[ $(kubectl get keycloaks/example-kc -o jsonpath="{.status.conditions[?(@.type == 'Ready')].status}") != "true" ]]
do
  echo "waiting for Keycloak example-kc status"
  ((c++)) && ((c==max_retries)) && exit -1
  sleep 1
done

c=0
while [[ $(kubectl get keycloakrealmimports/example-count0-kc -o jsonpath="{.status.conditions[?(@.type == 'Done')].status}") != "true" ]]
do
  echo "waiting for Keycloak Realm Import example-count0-kc status"
  ((c++)) && ((c==max_retries)) &&  exit -1
  sleep 1
done
