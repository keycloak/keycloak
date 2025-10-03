Azure SQL automation

This folder contains scripts, a composite action, and Ansible helpers to:
- Create an Azure SQL Server + database and the Keycloak DB user
- Provision an Azure VM and run Maven/Keycloak tasks on it (Ansible role)

Files
- `azure_common.sh` - shared defaults and environment checks
- `azure_create.sh` - create resource group, server, database and DB user using Azure CLI + sqlcmd
- `azure_delete.sh` - delete the resource group (removes server and DB)
- `azure_vm_cli.sh` - CLI wrapper to create/delete Azure VM and produce inventory via Ansible

Common files shared with EC2 automation:
- `mvn_vm_runner.sh` - runs the existing `mvn.yml` Ansible playbook against the created Azure or EC2 VM
- `keycloak_vm_installer.sh` - shell script that runs the Ansible playbook to install Keycloak on the provisioned VM
- Ansible playbooks and roles under `.github/scripts/ansible/roles/`

Required GitHub secrets and values
- `AZURE_CREDENTIALS` (repo secret) — service principal JSON used by the `azure/login` action or by Ansible on CI. Create via `az ad sp create-for-rbac --name "keycloak-ci" --role contributor --scopes /subscriptions/<your-subscription-id> --sdk-auth`.
- `AZURE_ADMIN_PASSWORD` (repo secret) — admin password for the SQL server created by the workflow.
- `AZURE_DB_PASSWORD` (repo secret) — password for the Keycloak DB user.