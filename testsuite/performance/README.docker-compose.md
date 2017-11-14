# Keycloak Performance Testsuite - Docker Compose

## Requirements:
- Maven 3.1.1+
- Unpacked Keycloak server distribution in `keycloak/target` folder.
- Docker 1.13+
- Docker Compose 1.14+

### Keycloak Server Distribution
To unpack the current Keycloak server distribution into `keycloak/target` folder:
1. Build and install the distribution by running `mvn install -Pdistribution` from the root of the Keycloak project.
2. Unpack the installed artifact by running `mvn process-resources` from the Performance Testsuite module.

## Deployments

### Singlenode Deployment
- Build / rebuild: `docker-compose build`
- Start services: `docker-compose up -d --build`
  Note: The `--build` parameter triggers a rebuild/restart of changed services if they are already running.
- Stop services: `docker-compose down -v`. If you wish to keep the container volumes skip the `-v` option.

### Keycloak Cluster Deployment
- Generate docker-compose-cluster.yml file: `./generate-docker-compose-cluster.sh`. Use `export CPUSETS="set1 set2 setN"` to customize.
- Build / rebuild: `docker-compose -f docker-compose-cluster.yml build`
- Start services: `docker-compose -f docker-compose-cluster.yml up -d --build`
- Scaling KC nodes: `docker-compose -f docker-compose-cluster.yml up -d --build --scale keycloak=2`
- Stop services: `docker-compose -f docker-compose-cluster.yml down -v`. If you wish to keep the container volumes skip the `-v` option.

### Cross-DC Deployment
- Generate docker-compose-crossdc.yml file: `./generate-docker-compose-crossdc.sh`. Use `export CPUSETS_DC1="set1 set2 setN"; export CPUSETS_DC2="set3 set4 setL"` to customize.
- Build / rebuild: `docker-compose -f docker-compose-crossdc.yml build`
- Start services: `docker-compose -f docker-compose-crossdc.yml up -d --build`
- Scaling KC nodes: `docker-compose -f docker-compose-crossdc.yml up -d --build --scale keycloak_dc1=2 --scale keycloak_dc2=3`
- Stop services: `docker-compose -f docker-compose-crossdc.yml down -v`. If you wish to keep the container volumes skip the `-v` option.

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
