# Ansible Role `mvn_ec2_runner`

Ansible role for executing `mvn` commands against a Keycloak src on a remote node.

Role assumes presence of host inventory file and a matching SSH key for "sudoer" access to the hosts.
The hosts are expected to be included in `keycloak` group.

## Parameters

See `defaults/main.yml` for default values.

### Execution
- `mvn_params`: The `mvn` command to execute on the remote nodes.

### Other
- `kc_home`: Location of the Keycloak src on the remote node.


## Example Playbook

An example playbook `keycloak.yml` that applies the role to hosts in the `keycloak` group:
```
- hosts: keycloak
  roles: [mvn]
```

## Run keycloak-benchmark

Run:
```
ansible-playbook -i ${CLUSTER_NAME}_${REGION}_inventory.yml mvn.yml \
  -e "mvn_params=\"mvn clean install\""
```
