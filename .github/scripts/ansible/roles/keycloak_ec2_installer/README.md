# Ansible Role `keycloak_ec2_installer`

Ansible role for installing Keycloak sources and build dependencies on remote nodes.

Role assumes presence of host inventory file and a matching SSH key for "sudoer" access to the hosts.
The hosts are expected to be included in `keycloak` group.

## Parameters

See `defaults/main.yml` for default values.

### Execution
- `keycloak_src`: Path to a local `*.zip` file containing the Keycloak src

### Other
- `update_system_packages`: Whether to update the system packages. Defaults to `no`.
- `install_java`: Whether to install OpenJDK on the system. Defaults to `yes`.
- `java_version`: Version of OpenJDK to be installed. Defaults to `21`.


## Example Playbook

An example playbook `keycloak.yml` that applies the role to hosts in the `keycloak` group:
```
- hosts: keycloak
  roles: [keycloak]
```

## Run keycloak-benchmark

Run:
```
ansible-playbook -i ${CLUSTER_NAME}_${REGION}_inventory.yml keycloak.yml \
  -e "keycloak_src=\"/tmp/keycloak.zip\""
```
