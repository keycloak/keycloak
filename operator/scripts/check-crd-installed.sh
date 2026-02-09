#! /bin/bash
set -euo pipefail

CRD=$1
max_retries=240
c=0
while ! kubectl get "${CRD}"
do
  echo "$(date +"%T") Waiting for ${CRD} CRD"
  ((c++)) && ((c==max_retries)) && exit -1
  sleep 1
done
