# Azure SQL Automation

This folder contains scripts, a composite action, and Ansible helpers to:
- Create an Azure SQL Server + database and the Keycloak DB user
- Provision an Azure VM and run Maven/Keycloak tasks on it (Ansible role)

---

## Prerequisites

Make sure that your Azure subscription is registered to use the `Microsoft.Sql` resource provider in order to create SQL resources. You can do this via the Azure Portal or Azure CLI:

```bash
# Ensure correct subscription is selected
az account set --subscription <your-subscription-id>

# Check registration status
az provider show --namespace Microsoft.Sql --query registrationState -o table

# Register the provider
az provider register --namespace Microsoft.Sql
```

---

## Files

### Azure-specific scripts
- **`azure_common.sh`** - Shared defaults and environment checks
- **`azure_create_sql.sh`** - Create resource group, server, database and DB user using Azure CLI + sqlcmd
- **`azure_vm_manager.sh`** - CLI wrapper to create/delete Azure VM and produce inventory via Ansible

### Common files shared with EC2 automation
- **`mvn_remote_runner.sh`** - Runs the existing `mvn.yml` Ansible playbook against the created Azure or EC2 VM
- **`keycloak_remote_installer.sh`** - Shell script that runs the Ansible playbook to install Keycloak on the provisioned VM
- **Ansible playbooks and roles** - Under `.github/scripts/ansible/roles/`

---

## GitHub Secrets and Configuration

### Required Secret

**`AZURE_CREDENTIALS`** *(required)* - Service principal JSON used by the `azure/login` action

Create via Azure CLI:
```bash
az ad sp create-for-rbac \
  --name "keycloak-ci" \
  --role contributor \
  --scopes /subscriptions/<your-subscription-id> \
  --sdk-auth
```

### Optional Variables (override defaults)

- **`AZURE_ADMIN_USER`** - SQL Server admin username *(default: `sqladmin`)*
- **`AZURE_DB_USER`** - Keycloak database username *(default: `keycloak`)*

### Optional Secrets (override auto-generated passwords)

- **`AZURE_ADMIN_PASSWORD`** - SQL Server admin password *(default: auto-generated)*
- **`AZURE_DB_PASSWORD`** - Keycloak database user password *(default: auto-generated)*