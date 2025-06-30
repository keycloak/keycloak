# Ansible Role `aws_ec2`

Ansible role for creating, deleting, stopping and starting AWS EC2 instances 
for running keycloak tests.

## Prerequisities

Role requires Ansible Collection `amazon.aws` version `6.0.0` or higher.

Role assumes that user is authenticated to use AWS CLI, ie. that authentication 
variables `AWS_ACCESS_KEY` and `AWS_SECRET_KEY` are set in the environment.


## Parameters
- `region`: AWS region for the resources to be created in.
- `cluster_name`: Unique name of the instance cluster within the region. Defaults to `keycloak_{{ cluster_identifier }}`.
- `cluster_identifier`: Identifier to distingish multiple clusters within the region. Defaults to `${USER}`.
- `cluster_size`: Number of EC2 instances to be created.
- `ami_name`: Name of the AMI image to be used for spawning instances.
- `instance_type`: [AWS instance type](https://aws.amazon.com/ec2/instance-types/).
- `instance_volume_size`: Size of instance storage device.
- `instance_device`: Path to Linux storage device.

For defaults see `defaults/main.yml`.


## Example Playbook

Example playbook `aws_ec2.yml`:
```
- hosts: localhost
  connection: local
  roles: [aws_ec2_client]
```

## Create Instances

Using the example playbook run:
```
ansible-playbook aws_ec2.yml -e region=<REGION> -e operation=create
```

Replace <REGION> with actual value, e.g. `us-west-1`.

Optionally you can override other parameters by `-e PARAMETER=VALUE` or `-e @PARAMS.yml`.

This operation will create the following 2 files:
- `{{ cluster_name }}_{{ region }}.pem` - private SSH key.
- `{{ cluster_name }}_{{ region }}_inventory.yml` - an Ansible host inventory file.

```
keycloak:
  children:
    "{{ cluster_name }}_{{ region }}":
      vars:
        ansible_user: ec2-user
        ansible_become: yes
        ansible_ssh_private_key_file: "{{ cluster_name }}_{{ region }}.pem"
      hosts:
        host-1-ip-address:
        host-2-ip-address:
        ...
```

Notice that the created hosts will be included in Ansible group `keycloak` 
and subgroup `{{ cluster_name }}_{{ region }}`.


## Stop and Start instances

Using the example playbook run:
```
ansible-playbook aws_ec2.yml -e region=<REGION> -e operation=stop
```

After the instances are stopped their public IP addresses will be de-allocated.

```
ansible-playbook aws_ec2.yml -e region=<REGION> -e operation=start
```

After the instances are started again the role will re-create the host inventory file with updated public IP addresses.


## Delete Instances
Using the example playbook run:
```
ansible-playbook aws_ec2.yml -e region=<REGION> -e operation=delete
```

This will remove created AWS resources and delete the host inventory file and private key.
