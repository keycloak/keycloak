#! /bin/bash
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

NAMESPACE=${1:-default}

kubectl apply -n "${NAMESPACE}" -f "${SCRIPT_DIR}/../src/test/resources/example-postgres.yaml"
kubectl apply -n "${NAMESPACE}" -f "${SCRIPT_DIR}/../src/test/resources/example-db-secret.yaml"
kubectl apply -n "${NAMESPACE}" -f "${SCRIPT_DIR}/../src/test/resources/example-tls-secret.yaml"
kubectl apply -n "${NAMESPACE}" -f "${SCRIPT_DIR}/../src/test/resources/example-keycloak.yaml"
kubectl apply -n "${NAMESPACE}" -f "${SCRIPT_DIR}/../src/test/resources/example-realm.yaml"

# Wait for the CRs to be ready
"${SCRIPT_DIR}"/check-examples-installed.sh ${NAMESPACE}

