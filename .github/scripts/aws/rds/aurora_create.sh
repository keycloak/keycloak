#!/usr/bin/env bash
set -e

if [[ "$RUNNER_DEBUG" == "1" ]]; then
  set -x
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
source ${SCRIPT_DIR}/aurora_common.sh

EXISTING_INSTANCES=$(aws rds describe-db-instances \
  --query "DBInstances[?starts_with(DBInstanceIdentifier, '${AURORA_CLUSTER}')].DBInstanceIdentifier" \
  --output text
)
if [ -n "${EXISTING_INSTANCES}" ]; then
  echo "Aurora instances '${EXISTING_INSTANCES}' already exist in the '${AWS_REGION}' region"
  exit 1
fi

# Create the Aurora VPC
AURORA_VPC=$(aws ec2 create-vpc \
  --cidr-block ${AURORA_VPC_CIDR} \
  --tag-specifications "ResourceType=vpc, Tags=[{Key=AuroraCluster,Value=${AURORA_CLUSTER}},{Key=Name,Value=Aurora Cluster ${AURORA_CLUSTER}}]" \
  --query "Vpc.VpcId" \
  --output text
)

# Each region may have different availability-zones, so we need to ensure that we use an az that exists
IFS='	' read -a AZS <<< "$(aws ec2 describe-availability-zones --region ${AURORA_REGION} --query "AvailabilityZones[].ZoneName" --output text)"

# Create the Aurora Subnets
SUBNET_A=$(aws ec2 create-subnet \
  --availability-zone "${AZS[0]}" \
  --vpc-id ${AURORA_VPC} \
  --cidr-block ${AURORA_SUBNET_A_CIDR} \
  --query "Subnet.SubnetId" \
  --output text
)

SUBNET_B=$(aws ec2 create-subnet \
  --availability-zone "${AZS[1]}" \
  --vpc-id ${AURORA_VPC} \
  --cidr-block ${AURORA_SUBNET_B_CIDR} \
  --query "Subnet.SubnetId" \
  --output text
)

AURORA_PUBLIC_ROUTE_TABLE_ID=$(aws ec2 describe-route-tables \
  --filters Name=vpc-id,Values=${AURORA_VPC} \
  --query "RouteTables[0].RouteTableId" \
  --output text
)

aws ec2 associate-route-table \
  --route-table-id ${AURORA_PUBLIC_ROUTE_TABLE_ID} \
  --subnet-id ${SUBNET_A}

aws ec2 associate-route-table \
  --route-table-id ${AURORA_PUBLIC_ROUTE_TABLE_ID} \
  --subnet-id ${SUBNET_B}

# Create Aurora Subnet Group
aws rds create-db-subnet-group \
  --db-subnet-group-name ${AURORA_SUBNET_GROUP_NAME} \
  --db-subnet-group-description "Aurora DB Subnet Group" \
  --subnet-ids ${SUBNET_A} ${SUBNET_B}

# Create an Aurora VPC Security Group
AURORA_SECURITY_GROUP_ID=$(aws ec2 create-security-group \
  --group-name ${AURORA_SECURITY_GROUP_NAME} \
  --description "Aurora DB Security Group" \
  --vpc-id ${AURORA_VPC} \
  --query "GroupId" \
  --output text
)

# Make the Aurora endpoint accessible outside the VPC
## Create Internet gateway
INTERNET_GATEWAY=$(aws ec2 create-internet-gateway \
  --tag-specifications "ResourceType=internet-gateway, Tags=[{Key=AuroraCluster,Value=${AURORA_CLUSTER}},{Key=Name,Value=Aurora Cluster ${AURORA_CLUSTER}}]" \
  --query "InternetGateway.InternetGatewayId" \
  --output text
)

aws ec2 attach-internet-gateway \
  --internet-gateway-id ${INTERNET_GATEWAY} \
  --vpc-id ${AURORA_VPC}

aws ec2 create-route \
  --route-table-id ${AURORA_PUBLIC_ROUTE_TABLE_ID} \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id ${INTERNET_GATEWAY}

## Enable DNS hostnames required for publicly accessible Aurora instances
aws ec2 modify-vpc-attribute  \
  --vpc-id ${AURORA_VPC} \
  --enable-dns-hostnames

## Ensure the Postgres port is accessible outside the VPC
aws ec2 authorize-security-group-ingress \
  --group-id ${AURORA_SECURITY_GROUP_ID} \
  --ip-permissions "FromPort=5432,ToPort=5432,IpProtocol=tcp,IpRanges=[{CidrIp=0.0.0.0/0}]"

# Create the Aurora DB cluster and instance
aws rds create-db-cluster \
    --db-cluster-identifier ${AURORA_CLUSTER} \
    --database-name keycloak \
    --engine ${AURORA_ENGINE} \
    --engine-version ${AURORA_ENGINE_VERSION} \
    --master-username ${AURORA_USERNAME} \
    --master-user-password ${AURORA_PASSWORD} \
    --vpc-security-group-ids ${AURORA_SECURITY_GROUP_ID} \
    --db-subnet-group-name ${AURORA_SUBNET_GROUP_NAME} \
    --tags "Key=keepalive" # Add keepalive tag to prevent keycloak-benchmark reaper from removing DB during nightly runs

# For now only two AZs in each region are supported due to the two subnets created above
for i in $( seq ${AURORA_INSTANCES} ); do
  aws rds create-db-instance \
    --db-cluster-identifier ${AURORA_CLUSTER} \
    --db-instance-identifier "${AURORA_CLUSTER}-instance-${i}" \
    --db-instance-class ${AURORA_INSTANCE_CLASS} \
    --engine ${AURORA_ENGINE} \
    --availability-zone "${AZS[$(((i - 1) % ${#AZS[@]}))]}" \
    --publicly-accessible
done

for i in $( seq ${AURORA_INSTANCES} ); do
  aws rds wait db-instance-available --db-instance-identifier "${AURORA_CLUSTER}-instance-${i}"
done

export AURORA_ENDPOINT=$(aws rds describe-db-clusters \
  --db-cluster-identifier ${AURORA_CLUSTER} \
  --query "DBClusters[*].Endpoint" \
  --output text
)
