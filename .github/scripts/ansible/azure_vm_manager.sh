#!/usr/bin/env bash
set -euo pipefail

# Usage: ./azure_vm_manager.sh <create|delete> <region> <cluster_name>
ACTION=${1:-}
REGION=${2:-}
CLUSTER=${3:-}

if [[ -z "$ACTION" || -z "$REGION" || -z "$CLUSTER" ]]; then
  echo "Usage: $0 <create|delete> <region> <cluster_name>"
  exit 1
fi

SSH_KEY="$CLUSTER"_"$REGION"
ADMIN_USER="azureuser"
VM_SIZE="Standard_D2s_v5"
IMAGE="Ubuntu2404"

if [[ "$ACTION" == "create" ]]; then
  # 1. Resource group (created via idempotent command)
  az group create --name "$CLUSTER" --location "$REGION"

  # 2. SSH key
  if [[ ! -f "${SSH_KEY}" ]]; then
    ssh-keygen -t rsa -b 2048 -f "${SSH_KEY}" -N ""
  fi

  # 3. Network security group
  az network nsg create --resource-group "$CLUSTER" --name "$CLUSTER-nsg"
  az network nsg rule create --resource-group "$CLUSTER" --nsg-name "$CLUSTER-nsg" --name Allow-SSH --protocol Tcp --priority 1000 --destination-port-range 22 --access Allow --direction Inbound

  # 4. Public IP
  az network public-ip create --resource-group "$CLUSTER" --name "$CLUSTER-pip" --allocation-method Static

  # 5. Virtual network
  az network vnet create --resource-group "$CLUSTER" --name "$CLUSTER-vnet" --address-prefix 10.0.0.0/16

  # 6. Subnet
  az network vnet subnet create --resource-group "$CLUSTER" --vnet-name "$CLUSTER-vnet" --name "$CLUSTER-subnet" --address-prefix 10.0.0.0/24

  # 7. Network interface
  az network nic create --resource-group "$CLUSTER" --name "$CLUSTER-nic" --vnet-name "$CLUSTER-vnet" --subnet "$CLUSTER-subnet" --network-security-group "$CLUSTER-nsg" --public-ip-address "$CLUSTER-pip"

  # 8. VM
  az vm create \
    --resource-group "$CLUSTER" \
    --name "$CLUSTER-vm" \
    --nics "$CLUSTER-nic" \
    --image "$IMAGE" \
    --size "$VM_SIZE" \
    --admin-username "$ADMIN_USER" \
    --ssh-key-values "${SSH_KEY}.pub" \
    --authentication-type ssh \
    --no-wait

  # 9. Wait for VM IP
  az vm wait --created --resource-group "$CLUSTER" --name "$CLUSTER-vm"
  VM_IP=$(az network public-ip show --resource-group "$CLUSTER" --name "$CLUSTER-pip" --query ipAddress -o tsv)

  # 10. Wait for SSH
  for i in {1..30}; do
    if nc -z "$VM_IP" 22; then break; fi
    sleep 5
  done

  # 11. Create inventory file with both host and group
  cat > "${CLUSTER}_${REGION}_inventory.yml" <<EOF
all:
  hosts:
    keycloak:
      ansible_host: $VM_IP
      ansible_user: $ADMIN_USER
      ansible_ssh_private_key_file: ${SSH_KEY}
  children:
    keycloak_group:
      hosts:
        keycloak:
EOF

  echo "VM provisioned. Inventory: ${CLUSTER}_${REGION}_inventory.yml"

elif [[ "$ACTION" == "delete" ]]; then
  az group delete --name "$CLUSTER" --yes --no-wait
  echo "Resource group $CLUSTER deletion initiated."
else
  echo "Unknown action: $ACTION"
  exit 2
fi
