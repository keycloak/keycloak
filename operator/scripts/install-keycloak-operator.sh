#! /bin/bash
set -euo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

INSTALL_NAMESPACE=${1:-default}

echo "$(date +"%T") Delete the default catalog if it exists"
sh -c "kubectl delete catalogsources operatorhubio-catalog -n olm | true"

kubectl apply -f $SCRIPT_DIR/../olm/testing-resources/catalog.yaml

# Wait for the catalog to be healthy
max_retries=200
c=0
while [[ $(kubectl get catalogsources test-catalog -o jsonpath="{.status.connectionState.lastObservedState}") != "READY" ]]
do
  echo "$(date +"%T") Waiting for the test-catalog to be ready"
  ((c++)) && ((c==max_retries)) && exit -1
  sleep 1
done

echo "$(date +"%T") The test-catalog is ready"
kubectl apply -f $SCRIPT_DIR/../olm/testing-resources/operatorgroup.yaml -n $INSTALL_NAMESPACE
kubectl apply -f $SCRIPT_DIR/../olm/testing-resources/subscription.yaml -n $INSTALL_NAMESPACE
