#!/usr/bin/env bash
set -e
cd $(dirname "${BASH_SOURCE[0]}")

if [[ "$RUNNER_DEBUG" == "1" ]]; then
  set -x
fi

OPERATION=$1
REGION=$2

case $OPERATION in
  requirements)
    ansible-galaxy collection install -r requirements.yml
    pip3 install --user "ansible==9.*" boto3 botocore
  ;;
  create|delete|start|stop)
    if [ -f "env.yml" ]; then ANSIBLE_CUSTOM_VARS_ARG="-e @env.yml"; fi
    CLUSTER_NAME=${CLUSTER_NAME:-"keycloak_$(whoami)"}
    ansible-playbook aws_ec2.yml -v -e "region=$REGION" -e "operation=$OPERATION" -e "cluster_name=$CLUSTER_NAME" $ANSIBLE_CUSTOM_VARS_ARG "${@:3}"
  ;;
  *)
    echo "Invalid option!"
    echo "Available operations: requirements, create, delete, start, stop."
  ;;
esac
