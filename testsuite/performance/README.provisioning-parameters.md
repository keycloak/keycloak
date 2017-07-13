# Keycloak Performance Testsuite - Provisioning Parameters

## Keycloak Server Settings:

| Category    | Setting                       | Property                           | Default value                                                    |
|-------------|-------------------------------|------------------------------------|------------------------------------------------------------------|
| JVM         | Memory settings               | `keycloak.jvm.memory`              | -Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m |
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

## CPUs

At the moment it is not possible to dynamically parametrize the number of CPUs for a service via Maven properties or environment variables.

To change the default value (`cpus: 1`) it is necessary to edit the Docker Compose file.


### Example: Keycloak service using 2 CPU cores

`docker-compose.yml` and `docker-compose-cluster.yml`:
```
services:
    ...
    keycloak:
        ...
        cpus: 2
        ...
```

`docker-compose-crossdc.yml`:
```
services:
    ...
    keycloak_dc1:
        ...
        cpus: 2
        ...
    keycloak_dc2:
        ...
        cpus: 2
        ...
```
