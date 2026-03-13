<!--
Copyright 2026 Capital One Financial Corporation and/or its affiliates
and other contributors as indicated by the @author tags.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Keycloak Redis Provider Architecture

```mermaid
graph TB
    subgraph keycloak["Keycloak Core (Unmodified)"]
        direction TB
        spi["SPI Interface Layer"]

        subgraph spis["Service Provider Interfaces"]
            direction LR
            us["UserSession<br/>Provider SPI"]
            as["AuthSession<br/>Provider SPI"]
            suo["SingleUse Object<br/>SPI"]
            lf["LoginFailure<br/>Provider SPI"]
            cp["Cluster<br/>Provider SPI"]
        end

        spi --> spis
    end

    subgraph redis_module["Redis Provider Module"]
        direction TB

        subgraph providers["Provider Implementations"]
            direction LR
            rusp["RedisUserSession<br/>Provider"]
            rasp["RedisAuthenticationSession<br/>Provider"]
            rsuo["RedisSingleUseObject<br/>Provider"]
            rlf["RedisUserLoginFailure<br/>Provider"]
            rcp["RedisCluster<br/>Provider"]
        end

        rcp_conn["Redis Connection<br/>Provider"]

        subgraph core["Core Components"]
            direction LR
            lettuce["Lettuce Client<br/>(Async I/O via Netty)"]
            pubsub_comp["Redis Pub/Sub<br/>(Cluster Events)"]
            lua["Lua Scripts<br/>(Atomic Ops)"]
            json["JSON Serialization<br/>(Jackson)"]
        end

        providers --> rcp_conn
        rcp_conn --> core
    end

    subgraph external["External Infrastructure"]
        direction LR
        redis_standalone["Redis<br/>Standalone"]
        redis_cluster["Redis<br/>Cluster Mode"]
        elasticache["AWS<br/>ElastiCache"]
    end

    spis -.->|implements| providers
    core --> external

    style keycloak fill:#dae8fc,stroke:#6c8ebf
    style spi fill:#e1d5e7,stroke:#9673a6
    style spis fill:#f5f5f5,stroke:#666666
    style redis_module fill:#fff2cc,stroke:#d6b656
    style providers fill:#d5e8d4,stroke:#82b366
    style rcp_conn fill:#ffe6cc,stroke:#d79b00
    style core fill:#f8cecc,stroke:#b85450
    style external fill:#e1d5e7,stroke:#9673a6
```
