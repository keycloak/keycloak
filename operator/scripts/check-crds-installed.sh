#! /bin/bash
set -euo pipefail

max_retries=240
c=0
while ! kubectl get keycloaks
do
  echo "$(date +"%T") Waiting for Keycloak CRD"
  ((c++)) && ((c==max_retries)) && exit -1
  sleep 1
done

c=0
while ! kubectl get keycloakrealmimports
do
  echo "$(date +"%T") Waiting for Keycloak Realm Import CRD"
  ((c++)) && ((c==max_retries)) && exit -1
  sleep 1
done
