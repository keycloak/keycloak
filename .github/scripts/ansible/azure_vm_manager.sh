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
  # 1. Resource group
  az group create --name "$CLUSTER" --location "$REGION"

  # 2. SSH key
  if [[ ! -f "${SSH_KEY}" ]]; then
    ssh-keygen -t rsa -b 2048 -f "${SSH_KEY}" -N ""
  fi

  # 3. VM (networking resources are created automatically)
  VM_IP=$(az vm create \
    --resource-group "$CLUSTER" \
    --name "$CLUSTER-vm" \
    --image "$IMAGE" \
    --size "$VM_SIZE" \
    --admin-username "$ADMIN_USER" \
    --ssh-key-values "${SSH_KEY}.pub" \
    --authentication-type ssh \
    --public-ip-address-allocation static \
    --query publicIpAddress -o tsv)

  # 4. Wait for SSH
  for i in {1..30}; do
    if nc -z "$VM_IP" 22; then break; fi
    sleep 5
  done

  # 5. Create inventory file
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
