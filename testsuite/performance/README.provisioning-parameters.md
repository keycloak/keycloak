# Keycloak Performance Testsuite - Provisioning Parameters

## Overview of Provisioned Services

### Testing

| Deployment      | Keycloak Server                          | Database           | Load Balancer      | Infinispan Server  |
|-----------------|------------------------------------------|--------------------|--------------------|--------------------|
| *Singlenode*    | 1 instance                               | 1 instance         | -                  | -                  |
| *Cluster*       | N instances                              | 1 instance         | 1 instance         | -                  |
| *Cross-DC*      | K instances in DC1 + L instances in DC2  | 1 instance per DC  | 1 instance per DC  | 1 instance per DC  |

### Monitoring

| Deployment      | CAdvisor    | Influx DB   | Grafana     |
|-----------------|-------------|-------------|-------------|
| *Monitoring*    | 1 instance  | 1 instance  | 1 instance  |


## Service Parameters

### Keycloak Server

| Category    | Setting                       | Property                           | Default Value                                                      |
|-------------|-------------------------------|------------------------------------|--------------------------------------------------------------------|
| Keycloak    | Server version                | `server.version`                   | `${project.version}` from the project `pom.xml` file.              |
|             | Admin user                    | `keycloak.admin.user`              | `admin`                                                            |
|             | Admin user's password         | `keycloak.admin.password`          | `admin`                                                            |
| Scaling<sup>[1]</sup> | Scale for cluster   | `keycloak.scale`                   | Maximum size<sup>[2]</sup> of cluster.                             |
|             | Scale for DC1                 | `keycloak.dc1.scale`               | Maximum size of DC1.                                               |
|             | Scale for DC2                 | `keycloak.dc2.scale`               | Maximum size of DC2.                                               |
| Docker      | Allocated CPUs                | `keycloak.docker.cpusets`          | `2-3` for singlenode, `2 3` for cluster deployment                 |
|             | Allocated CPUs for DC1        | `keycloak.dc1.docker.cpusets`      | `2`                                                                |
|             | Allocated CPUs for DC2        | `keycloak.dc2.docker.cpusets`      | `3`                                                                |
|             | Available memory              | `keycloak.docker.memlimit`         | `2500m`                                                            |
| JVM         | Memory settings               | `keycloak.jvm.memory`              | `-Xms64m -Xmx2g -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m`   |
| Undertow    | HTTP Listener max connections | `keycloak.http.max-connections`    | `50000`                                                            |
|             | AJP Listener max connections  | `keycloak.ajp.max-connections`     | `50000`                                                            |
| IO          | Worker IO thread pool         | `keycloak.worker.io-threads`       | `2`                                                                |
|             | Worker Task thread pool       | `keycloak.worker.task-max-threads` | `16`                                                               |
| Datasources | Connection pool min size      | `keycloak.ds.min-pool-size`        | `10`                                                               |
|             | Connection pool max size      | `keycloak.ds.max-pool-size`        | `100`                                                              |
|             | Connection pool prefill       | `keycloak.ds.pool-prefill`         | `true`                                                             |
|             | Prepared statement cache size | `keycloak.ds.ps-cache-size`        | `100`                                                              |

**[ 1 ]** The scaling parameters are optional. They can be set within interval from 1 to the maximum cluster size].
If not set they are automatically set to the maximum size of the cluster (DC1/DC2 respectively).

**[ 2 ]** Maximum cluster size is determined by provisioner-specific parameter such as `keycloak.docker.cpusets` for the default *docker-compose* provisioner.
The maximum cluster size corresponds to the number of cpusets.

### Database

| Category    | Setting                       | Property                           | Default Value                                                      |
|-------------|-------------------------------|------------------------------------|--------------------------------------------------------------------|
| Docker      | Allocated CPUs                | `db.docker.cpusets`                | `1`                                                                |
|             | Allocated CPUs for DC1        | `db.dc1.docker.cpusets`            | `1`                                                                |
|             | Allocated CPUs for DC2        | `db.dc2.docker.cpusets`            | `1`                                                                |
|             | Available memory              | `db.docker.memlimit`               | `2g`                                                               |

### Load Balancer

| Category    | Setting                       | Property                     | Default Value                                                       |
|-------------|-------------------------------|------------------------------|---------------------------------------------------------------------|
| Docker      | Allocated CPUs                | `lb.docker.cpusets`          | `1`                                                                 |
|             | Allocated CPUs for DC1        | `lb.dc1.docker.cpusets`      | `1`                                                                 |
|             | Allocated CPUs for DC2        | `lb.dc2.docker.cpusets`      | `1`                                                                 |
|             | Available memory              | `lb.docker.memlimit`         | `1500m`                                                             |
| JVM         | Memory settings               | `lb.jvm.memory`              | `-Xms64m -Xmx1024m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m` |
| Undertow    | HTTP Listener max connections | `lb.http.max-connections`    | `50000`                                                             |
| IO          | Worker IO thread pool         | `lb.worker.io-threads`       | `2`                                                                 |
|             | Worker Task thread pool       | `lb.worker.task-max-threads` | `16`                                                                |

### Infinispan Server

| Category    | Setting                       | Property                        | Default Value                                                                             |
|-------------|-------------------------------|---------------------------------|-------------------------------------------------------------------------------------------|
| Docker      | Allocated CPUs for DC1        | `infinispan.dc1.docker.cpusets` | `1`                                                                                       |
|             | Allocated CPUs for DC2        | `infinispan.dc2.docker.cpusets` | `1`                                                                                       |
|             | Available memory              | `infinispan.docker.memlimit`    | `1500m`                                                                                   |
| JVM         | Memory settings               | `infinispan.jvm.memory`         | `-Xms64m -Xmx1g -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m -XX:+DisableExplicitGC`   |

### Monitoring

| Category    | Setting                       | Property                    | Default Value   |
|-------------|-------------------------------|-----------------------------|-----------------|
| Docker      | Allocated CPUs                | `monitoring.docker.cpusets` | `0`             |
| JMX         | Management user               | `management.user`           | Not set.        |
|             | Management user's password    | `management.user.password`  | Not set.        |

By setting the `managemen.user` and `management.user.password` parameters it is possible 
to add a management user to all WildFly-backed services (*Keycloak Server*, *Infinispan Server* and the *Load Balancer*).
Unless both parameters are explicitly provided during the provisioning phase the user will not be added 
and it won't be possible to log into the management console or access JMX.


## Note on Docker settings

By default, there are 4 CPU cores allocated: core 0 for monitoring, core 1 for database (MariaDB), and cores 2 and 3 for Keycloak server.
Default memory limits for database and Keycloak server are 2g. The `cpuset` and `memlimit` parameters set here are set to `cpuset` and
`mem_limit` parameters of docker-compose configuration. See docker-compose documentation for meaning of the values. How to set the parameters
correctly depends on number of factors - number of cpu cores, NUMA, available memory etc., hence it is out of scope of this document.

### Example CPU Settings

| HW          | Development Machine  | "Fat Box"    |
|-------------|----------------------|--------------|
| Cores       | 4                    | 48           |
| NUMA Nodes  | 0-3                  | 0-23, 24-47  |

#### Cluster

| Setting                            | Development Machine  | "Fat Box"                   |
|------------------------------------|----------------------|-----------------------------|
| `monitoring.docker.cpusets`        | 0                    | 0                           |
| `db.docker.cpusets`                | 1                    | 1                           |
| `lb.docker.cpusets`                | 1                    | 2                           |
| `keycloak.docker.cpusets`          | 2-3                  | 3-6 7-10 11-16 … 43-46      |

#### Cross-DC

| Setting                            | Development Machine  | "Fat Box"                      |
|------------------------------------|----------------------|--------------------------------|
| `monitoring.docker.cpusets`        | 0                    | 0                              |
| `db.dc1.docker.cpusets`            | 1                    | 1                              |
| `lb.dc1.docker.cpusets`            | 1                    | 2                              |
| `infinispan.dc1.docker.cpusets`    | 1                    | 3                              |
| `keycloak.dc1.docker.cpusets`      | 2                    | 4-7 8-11 12-15 16-19 20-23     |
| `db.dc2.docker.cpusets`            | 1                    | 24                             |
| `lb.dc2.docker.cpusets`            | 1                    | 25                             |
| `infinispan.dc2.docker.cpusets`    | 1                    | 26                             |
| `keycloak.dc2.docker.cpusets`      | 3                    | 27-30 31-34 35-38 39-42 43-46  |
