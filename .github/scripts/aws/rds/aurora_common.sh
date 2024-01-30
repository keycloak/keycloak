#!/usr/bin/env bash
set -e

function requiredEnv() {
  for ENV in $@; do
      if [ -z "${!ENV}" ]; then
        echo "${ENV} variable must be set"
        exit 1
      fi
  done
}

requiredEnv AURORA_CLUSTER AURORA_REGION

export AURORA_ENGINE=${AURORA_ENGINE:-"aurora-postgresql"}
export AURORA_ENGINE_VERSION=${AURORA_ENGINE_VERSION:-"15.3"}
export AURORA_INSTANCES=${AURORA_INSTANCES:-"2"}
export AURORA_INSTANCE_CLASS=${AURORA_INSTANCE_CLASS:-"db.t4g.large"}
export AURORA_PASSWORD=${AURORA_PASSWORD:-"secret99"}
export AURORA_SECURITY_GROUP_NAME=${AURORA_SECURITY_GROUP_NAME:-"${AURORA_CLUSTER}-security-group"}
export AURORA_SUBNET_A_CIDR=${AURORA_SUBNET_A_CIDR:-"192.168.0.0/19"}
export AURORA_SUBNET_B_CIDR=${AURORA_SUBNET_B_CIDR:-"192.168.32.0/19"}
export AURORA_SUBNET_GROUP_NAME=${AURORA_SUBNET_GROUP_NAME:-"${AURORA_CLUSTER}-subnet-group"}
export AURORA_VPC_CIDR=${AURORA_VPC_CIDR:-"192.168.0.0/16"}
export AURORA_USERNAME=${AURORA_USERNAME:-"keycloak"}
export AWS_REGION=${AWS_REGION:-${AURORA_REGION}}
export AWS_PAGER=""
