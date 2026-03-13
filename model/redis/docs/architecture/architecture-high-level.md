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

# Keycloak High-Level Architecture

```mermaid
%%{init: {'theme':'default', 'themeVariables': {'background':'#ffffff', 'lineColor':'#888888', 'primaryTextColor':'#333333', 'secondaryTextColor':'#333333', 'tertiaryTextColor':'#333333'}}}%%
graph TB
    subgraph kc["Keycloak Cluster (Stateless)"]
        direction LR
        node1["Node 1<br/>4 vCPU / 8GB<br/>━━━━━━━━━<br/>Redis SPI"]
        node2["Node 2<br/>4 vCPU / 8GB<br/>━━━━━━━━━<br/>Redis SPI"]
        dots["..."]
        nodeN["Node N<br/>4 vCPU / 8GB<br/>━━━━━━━━━<br/>Redis SPI"]
    end

    pubsub["Redis Pub/Sub<br/>(Cluster Events)"]

    subgraph redis["Redis Cluster <br/> (Managed - AWS ElastiCache)"]
        direction TB
        spacer[" "]
        subgraph shards[" "]
            direction LR
            subgraph s1["Shard 1"]
                m1["Master"]
                r1["Replica"]
            end
            subgraph s2["Shard 2"]
                m2["Master"]
                r2["Replica"]
            end
            subgraph sN["Shard N"]
                mN["Master"]
                rN["Replica"]
            end
        end
        spacer ~~~ shards
    end

    postgres["PostgreSQL Database<br/>(Persistent State)"]

    node1 --> pubsub
    node2 --> pubsub
    nodeN --> pubsub

    pubsub --> redis

    redis -.-> postgres

    note1["• Session Data (TTL-managed)<br/>• Distributed Locks (Lua scripts)<br/>• Action Tokens (Single-use)<br/>• Login Failure Tracking"]

    style kc fill:#dae8fc,stroke:#6c8ebf
    style node1 fill:#d5e8d4,stroke:#82b366
    style node2 fill:#d5e8d4,stroke:#82b366
    style nodeN fill:#d5e8d4,stroke:#82b366
    style pubsub fill:#ffe6cc,stroke:#d79b00
    style redis fill:#f8cecc,stroke:#b85450
    style shards fill:#f8cecc,stroke:#f8cecc
    style spacer fill:none,stroke:none
    style postgres fill:#d5e8d4,stroke:#82b366
    style s1 fill:#fff2cc,stroke:#d6b656
    style s2 fill:#fff2cc,stroke:#d6b656
    style sN fill:#fff2cc,stroke:#d6b656
    style m1 fill:#e1d5e7,stroke:#9673a6
    style r1 fill:#e1d5e7,stroke:#9673a6
    style m2 fill:#e1d5e7,stroke:#9673a6
    style r2 fill:#e1d5e7,stroke:#9673a6
    style mN fill:#e1d5e7,stroke:#9673a6
    style rN fill:#e1d5e7,stroke:#9673a6
```
