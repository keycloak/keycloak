#!/usr/bin/env bash
set -e

if [[ "$RUNNER_DEBUG" == "1" ]]; then
  set -x
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
source ${SCRIPT_DIR}/aurora_common.sh

AURORA_VPC=$(aws ec2 describe-vpcs \
  --filters "Name=tag:AuroraCluster,Values=${AURORA_CLUSTER}" \
  --query "Vpcs[*].VpcId" \
  --output text
)

# Delete the Aurora DB cluster and instances
for i in $( aws rds describe-db-clusters --db-cluster-identifier ${AURORA_CLUSTER} --query "DBClusters[0].DBClusterMembers[].DBInstanceIdentifier" --output text ); do
  echo "Deleting Aurora DB instance ${i}"
  aws rds delete-db-instance --db-instance-identifier "${i}" --skip-final-snapshot || true
done

aws rds delete-db-cluster \
  --db-cluster-identifier ${AURORA_CLUSTER} \
  --skip-final-snapshot \
  || true

for i in $( aws rds describe-db-clusters --db-cluster-identifier ${AURORA_CLUSTER} --query "DBClusters[0].DBClusterMembers[].DBInstanceIdentifier" --output text ); do
  aws rds wait db-instance-deleted --db-instance-identifier "${i}"
done

aws rds wait db-cluster-deleted --db-cluster-identifier ${AURORA_CLUSTER} || true

# Delete the Aurora subnet group
aws rds delete-db-subnet-group --db-subnet-group-name ${AURORA_SUBNET_GROUP_NAME} || true

# Delete the Aurora subnets
AURORA_SUBNETS=$(aws ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${AURORA_VPC}" \
  --query "Subnets[*].SubnetId" \
  --output text
)
for AURORA_SUBNET in ${AURORA_SUBNETS}; do
  aws ec2 delete-subnet --subnet-id ${AURORA_SUBNET}
done

# Delete the Aurora VPC Security Group
AURORA_SECURITY_GROUP_ID=$(aws ec2 describe-security-groups \
  --filters "Name=vpc-id,Values=${AURORA_VPC}" "Name=group-name,Values=${AURORA_SECURITY_GROUP_NAME}" \
  --query "SecurityGroups[*].GroupId" \
  --output text
)
if [ -n "${AURORA_SECURITY_GROUP_ID}" ]; then
  aws ec2 delete-security-group --group-id ${AURORA_SECURITY_GROUP_ID} --region ${AURORA_REGION}
fi

# Detach the internet gateway from the VPC and remove
INTERNET_GATEWAY=$(aws ec2 describe-internet-gateways \
  --filters "Name=tag:AuroraCluster,Values=${AURORA_CLUSTER}" \
  --query "InternetGateways[*].InternetGatewayId" \
  --output text
)

aws ec2 detach-internet-gateway \
  --internet-gateway-id ${INTERNET_GATEWAY} \
  --vpc-id ${AURORA_VPC} \
  || true

aws ec2 delete-internet-gateway --internet-gateway-id ${INTERNET_GATEWAY} || true

# Delete the Aurora VPC, retrying 5 times in case that dependencies are not removed instantly
n=0
until [ "$n" -ge 20 ]
do
   aws ec2 delete-vpc --vpc-id ${AURORA_VPC} && break
   n=$((n+1))
   echo "Unable to remove VPC ${AURORA_VPC}. Attempt ${n}"
   sleep 10
done
