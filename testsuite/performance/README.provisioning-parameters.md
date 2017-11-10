# Keycloak Performance Testsuite - Provisioning Parameters

## Keycloak Server Settings:

| Category    | Setting                       | Property                           | Default value                                                    |
|-------------|-------------------------------|------------------------------------|------------------------------------------------------------------|
| JVM         | Memory settings               | `keycloak.jvm.memory`              | -Xms64m -Xmx2g -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m   |
| Undertow    | HTTP Listener max connections | `keycloak.http.max-connections`    | 500                                                              |
|             | AJP Listener max connections  | `keycloak.ajp.max-connections`     | 500                                                              |
| IO          | Worker IO thread pool         | `keycloak.worker.io-threads`       | 2                                                                |
|             | Worker Task thread pool       | `keycloak.worker.task-max-threads` | 16                                                               |
| Datasources | Connection pool min size      | `keycloak.ds.min-pool-size`        | 10                                                               |
|             | Connection pool max size      | `keycloak.ds.max-pool-size`        | 100                                                              |
|             | Connection pool prefill       | `keycloak.ds.pool-prefill`         | true                                                             |
|             | Prepared statement cache size | `keycloak.ds.ps-cache-size`        | 100                                                              |

## Load Balancer Settings:

| Category    | Setting                       | Property                              | Default value                                                    |
|-------------|-------------------------------|---------------------------------------|------------------------------------------------------------------|
| JVM         | Memory settings               | `keycloak-lb.jvm.memory`              | -Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m |
| Undertow    | HTTP Listener max connections | `keycloak-lb.http.max-connections`    | 500                                                              |
| IO          | Worker IO thread pool         | `keycloak-lb.worker.io-threads`       | 2                                                                |
|             | Worker Task thread pool       | `keycloak-lb.worker.task-max-threads` | 16                                                               |

## Infinispan Server Settings

| Category    | Setting                       | Property                | Default value                                                                           |
|-------------|-------------------------------|-------------------------|-----------------------------------------------------------------------------------------|
| JVM         | Memory settings               | `infinispan.jvm.memory` | -Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m -XX:+DisableExplicitGC |

## Docker settings

By default, there are 4 CPU cores allocated: core 0 for monitoring, core 1 for database (MariaDB), and cores 2 and 3 for Keycloak server.
Default memory limits for database and Keycloak server are 2g. The `cpuset` and `memlimit` parameters set here are set to `cpuset` and
`mem_limit` parameters of docker-compose configuration. See docker-compose documentation for meaning of the values. How to set the parameters
correctly depends on number of factors - number of cpu cores, NUMA, available memory etc., hence it is out of scope of this document.

| Container   | Setting                       | Property                        | Default value                                         |
|-------------|-------------------------------|---------------------------------|-------------------------------------------------------|
| Keycloak    | Allocated CPUs                | `keycloak.docker.cpuset`        | 2-3                                                   |
|             | Allocated CPUs for DC1        | `keycloak.dc1.docker.cpuset`    | 2-3                                                   |
|             | Allocated CPUs for DC2        | `keycloak.dc2.docker.cpuset`    | 2-3                                                   |
|             | Available memory              | `keycloak.docker.memlimit`      | 2g                                                    |
| MariaDB     | Allocated CPUs                | `db.docker.cpuset`              | 1                                                     |
|             | Available memory              | `db.docker.memlimit`            | 2g                                                    |
| Monitoring  | Allocated CPUs                | `monitoring.docker.cpuset`      | 0                                                     |

