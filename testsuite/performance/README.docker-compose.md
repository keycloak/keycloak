# Keycloak Performance Testsuite - Docker Compose Provisioner

## Supported Deployments

| Deployment   | Available Operations                                  | Orchestration Template          |
| ------------ | ----------------------------------------------------- | ------------------------------- |
| `singlenode` | `provision`, `teardown`, `export-dump`, `import-dump` | `docker-compose.yml`            |
| `cluster`    | `provision`, `teardown`, `export-dump`, `import-dump` | `docker-compose-cluster.yml`*   |
| `crossdc`    | `provision`, `teardown`, `export-dump`, `import-dump` | `docker-compose-crossdc.yml`*   |
| `monitoring` | `provision`, `teardown`                               | `docker-compose-monitoring.yml` |

The docker-compose orchestration templates are located in `tests/src/main/docker-compose` directory.

**[*]** The cluster and crossdc templates are generated dynamically during the `provision` operation based on provided `cpusets` parameter.
One Keycloak service entry is generated for each cpuset. This is a workaround for limitations of the default docker-compose scaling mechanism 
which only allows setting `cpuset` per service, not per container. For more predictable performance results it is necessary for each 
Keycloak server to have an exclusive access to specific CPU cores.

## Debugging docker containers:
- List started containers: `docker ps`. It's useful to watch continuously: `watch docker ps`.
  To list compose-only containers use: `docker-compose ps`, but this doesn't show container health status.
- Watch logs of a specific container: `docker logs -f crossdc_mariadb_dc1_1`.
- Watch logs of all containers managed by docker-compose: `docker-compose logs -f`.
- List networks: `docker network ls`
- Inspect network: `docker network inspect NETWORK_NAME`. Shows network configuration and currently connected containers.

## Network Addresses
### KC
`10.i.1.0/24` One network per DC. For single-DC deployments `i = 0`, for cross-DC deployment `i ∈ ℕ` is an index of particular DC.
### Load Balancing
`10.0.2.0/24` Network spans all DCs.
### DB Replication
`10.0.3.0/24` Network spans all DCs.
### ISPN Replication
`10.0.4.0/24` Network spans all DCs.
