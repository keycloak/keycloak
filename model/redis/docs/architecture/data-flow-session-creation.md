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

# Data Flow: Session Creation

```mermaid
sequenceDiagram
    participant User
    participant KC as Keycloak Node
    participant Provider as RedisUserSessionProvider
    participant Redis as Redis Cluster
    participant PubSub as Redis Pub/Sub
    participant Node2 as Other Keycloak Nodes
    User ->> KC: POST /realms/master/login-actions/authenticate
    activate KC

    KC->>KC: Authenticate credentials
    KC->>Provider: createUserSession(realm, user, client)
    activate Provider

    Provider->>Provider: Serialize session to JSON (~2KB)
    Note over Provider: Session data includes:<br/>- User ID, realm, client<br/>- Auth timestamp<br/>- Session attributes

    Provider->>Redis: SETEX kc:session:{id} TTL {json}
    activate Redis
    Note over Redis: TTL = 1800s (30 min)<br/>Auto-expires after TTL
    Redis-->>Provider: OK
    deactivate Redis

    Provider->>PubSub: PUBLISH kc:cluster:events<br/>{"type": "SESSION_CREATED", "id": "..."}
    activate PubSub
    PubSub-->>Provider: OK
    deactivate PubSub

    Provider-->>KC: UserSessionModel
    deactivate Provider

    KC-->>User: 200 OK + Session Cookie
    deactivate KC

    Note over PubSub,Node2: Cluster Distribution

    PubSub->>Node2: SESSION_CREATED event
    activate Node2
    Node2->>Node2: Invalidate local cache<br/>(if exists)
    deactivate Node2

    Note over Redis: Session Availability
    User ->> Node2: GET /realms/master/account<br/>(with session cookie)
    activate Node2
    Node2->>Redis: GET kc:session:{id}
    activate Redis
    Redis-->>Node2: {json session data}
    deactivate Redis
    Node2-->>User: 200 OK (Account page)
    deactivate Node2

    Note over Redis: Optimistic Locking for Updates

    User->>KC: Update session attributes
    activate KC
    KC->>Provider: updateSession(session)
    activate Provider

    Provider->>Redis: Lua Script:<br/>CHECK version == expected<br/>IF match: SETEX new data<br/>ELSE: return conflict
    activate Redis
    Redis-->>Provider: OK (version updated)
    deactivate Redis

    Provider->>PubSub: PUBLISH SESSION_UPDATED
    PubSub->>Node2: Invalidate cache

    Provider-->>KC: Success
    deactivate Provider
    KC-->>User: 200 OK
    deactivate KC

    Note over Redis: Automatic Expiration

    Redis->>Redis: TTL expires (after 30 min idle)
    Note over Redis: Session automatically<br/>deleted by Redis
```
