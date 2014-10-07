#!/bin/bash

chmod u+x /keycloak-docker-cluster/shared-files/keycloak-run-node.sh
chmod u+x /keycloak-docker-cluster/shared-files/keycloak-base-prepare.sh
chmod u+x /keycloak-docker-cluster/shared-files/deploy-examples.sh

echo "Permissions changed. Triggering keycloak-run-node.sh"
/keycloak-docker-cluster/shared-files/keycloak-run-node.sh
