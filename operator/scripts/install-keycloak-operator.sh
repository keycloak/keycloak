#! /bin/bash
set -euxo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# Delete the default catalog if it exists
sh -c "kubectl delete catalogsources operatorhubio-catalog -n olm | true"

kubectl apply -f $SCRIPT_DIR/../olm/testing-resources/catalog.yaml

# Wait for the catalog to be healthy
max_retries=200
c=0
while [[ $(kubectl get catalogsources test-catalog -o jsonpath="{.status.connectionState.lastObservedState}") != "READY" ]]
do
  echo "waiting for the test-catalog to be ready"
  ((c++)) && ((c==max_retries)) && exit -1
  sleep 1
done

kubectl apply -f $SCRIPT_DIR/../olm/testing-resources/operatorgroup.yaml
kubectl apply -f $SCRIPT_DIR/../olm/testing-resources/subscription.yaml
