#! /bin/bash
set -euxo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# Delete the default catalog if it exists
sh -c "kubectl delete catalogsources operatorhubio-catalog -n olm | true"

kubectl apply -f $SCRIPT_DIR/../olm/testing-resources
