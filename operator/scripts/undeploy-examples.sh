#! /bin/bash
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

NAMESPACE=${1:-default}
kubectl delete -n "${NAMESPACE}" -f "${SCRIPT_DIR}/../src/test/resources/example-postgres.yaml"
kubectl delete -n "${NAMESPACE}" -f "${SCRIPT_DIR}/../src/test/resources/example-db-secret.yaml"
kubectl delete -n "${NAMESPACE}" -f "${SCRIPT_DIR}/../src/test/resources/example-tls-secret.yaml"
kubectl delete -n "${NAMESPACE}" -f "${SCRIPT_DIR}/../src/test/resources/example-keycloak.yaml"
kubectl delete -n "${NAMESPACE}" -f "${SCRIPT_DIR}/../src/test/resources/example-realm.yaml"
