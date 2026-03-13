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

# Architecture Overview

> **⚠️ EXPERIMENTAL FEATURE:** This provider requires the `redis-storage` feature flag to be enabled. See the [main README](../../README.md#enabling-the-feature) for details.

## System Architecture

The Keycloak Redis Provider replaces the default Infinispan cache with Redis for session storage, providing a cloud-native alternative with managed service support.

### Architecture Diagrams

For visual representations, see:
- [High-Level Architecture](architecture-high-level.md) - Complete system architecture
- [SPI Implementation](architecture-spi.md) - Provider interface details
- [Session Creation Flow](data-flow-session-creation.md) - Data flow sequence

## Core Components

### 1. Provider Layer (SPI Implementation)

**[User Session Provider](docs/providers/user-sessions.md)**
- Manages user and client sessions
- Handles online and offline sessions
- TTL-based expiration

**[Authentication Session Provider](docs/providers/authentication-sessions.md)**
- Manages login flow sessions
- Short-lived (5 minutes default)
- Supports OIDC and SAML flows

**[Single-Use Object Provider](docs/providers/singleuse.md)**
- Authorization codes
- Action tokens (email verification, password reset)
- One-time use enforcement

**[Login Failure Provider](docs/providers/login-failure.md)**
- Brute force protection
- Failed login tracking
- Temporary account locks

**[Cluster Provider](docs/providers/cluster.md)**
- Distributed coordination
- Redis Pub/Sub for event distribution
- Cross-node cache invalidation

### 2. Connection Layer

**[Redis Connection Provider](docs/providers/redis-connection.md)**
- Manages Redis client lifecycle (singleton pattern)
- Connection pooling (Lettuce) for standalone and cluster modes
- Health monitoring and automatic reconnection
- Metrics collection (HdrHistogram for latency tracking)
- Optimistic locking via Lua scripts
- Batch operations (MGET/MSET/bulk delete)

**DefaultRedisClientFactory**
- Creates Redis clients (Standalone or Cluster mode)
- SSL/TLS configuration
- Connection string parsing

### 3. Storage Layer

**Redis Data Model**

```
Key Structure:
kc:{cacheType}:{entityId}           # Entity data (JSON)
kc:{cacheType}:{entityId}:version   # Optimistic locking version

Cache Types:
- sessions            # Online user sessions
- offlineSessions     # Offline/refresh token sessions
- clientSessions      # Client-specific sessions
- authenticationSessions  # Login flow sessions
- loginFailures       # Failed login tracking
- actionTokens        # Single-use tokens
- work                # Distributed locks
```

## Design Decisions

### JSON Serialization

**Chosen**: JSON over binary serialization

**Rationale**:
- Human-readable for debugging (`redis-cli GET kc:session:{id}`)
- Cross-language compatibility
- Schema evolution without version lock-in
- Operational simplicity > raw performance
- Trade-off: ~30% larger than binary (acceptable)

### Optimistic Locking

**Implementation**: Version counter + Lua scripts

**Mechanism**:
1. Each entity has a version key: `kc:{cacheType}:{id}:version`
2. Read operation retrieves both data and version
3. Update operation uses Lua script for atomic check-and-set
4. Automatic retry on version conflict

**Benefits**:
- Prevents lost updates in concurrent scenarios
- No distributed locks needed for updates
- Atomic operations via Redis Lua scripts

### Pub/Sub for Clustering

**Chosen**: Redis Pub/Sub over JGroups/UDP

**Rationale**:
- Simpler than JGroups configuration
- Works in cloud environments (AWS, Azure, GCP)
- No UDP/multicast requirements
- Standard Redis feature
- Auto-reconnect on disconnect

**Pattern**: `kc:cluster:*` for all cluster events

### TTL-Based Expiration

**Implementation**: Redis `PSETEX` for millisecond precision

**Benefits**:
- Automatic cleanup (no manual deletion)
- Memory management by Redis
- Configurable per session type
- Aligns with Redis eviction policies

**Eviction Policy**: `volatile-lru` (evict sessions with TTL first)

## Resilience & Failure Handling

### Failure Scenarios Tested

1. **Redis Primary Failure**: Auto-failover to replica
2. **Network Partition**: Circuit breaker + reconnect
3. **Keycloak Node Crash**: Seamless failover
4. **Concurrent Updates**: Optimistic locking prevents corruption
5. **Split-Brain**: Lua scripts prevent data corruption

**Result**: Zero session data loss across all failure modes

## Deployment Modes

### Standalone Redis

- Single Redis instance
- Simple configuration
- Suitable for development
- No high availability

### Redis Cluster Mode

- Multi-shard deployment
- Horizontal scaling
- Hash slot distribution
- Production-ready

### Managed Services Tested

- AWS ElastiCache (Cluster Mode)
- Built-in monitoring

---

## Provider Implementation Deep Dives

The Redis provider implements 5 Keycloak SPIs, all built on top of a shared Redis connection layer. Each has comprehensive documentation covering architecture, implementation details, and production considerations.

### 🔧 Foundation Layer

**[Redis Connection Provider](../providers/redis-connection.md)**
- **Purpose**: Singleton connection infrastructure shared by all Redis providers
- **Key Features**: Dual-mode support (standalone/cluster), optimistic locking, batch operations, Pub/Sub, comprehensive metrics
- **Critical Details**: Lettuce connection pooling, HdrHistogram latency tracking, Lua scripts for atomicity, health monitoring
- **Performance**: <1ms p95 GET latency, <2ms p95 PUT latency, automatic reconnection
- **Use Cases**: Foundation for all Redis operations, connection management, health monitoring, metrics collection

### 🔐 Session Management

**[User Session Provider](../providers/user-sessions.md)**
- **Purpose**: Manages authenticated user sessions (online and offline)
- **Key Features**: Deferred write pattern, optimistic locking, TTL-based expiration, session indexing
- **Critical Details**: Token validation notes, O(1) lookups via Redis sorted sets
- **Use Cases**: Login sessions, refresh tokens, "remember me" functionality

**[Client Session Provider](../providers/client-sessions.md)**
- **Purpose**: Links user sessions to specific OAuth2/OIDC applications
- **Key Features**: Protocol mapper support, client scopes, deferred write batching
- **Critical Details**: OAuth2 token validation notes (STARTED_AT_NOTE, USER_SESSION_STARTED_AT_NOTE)
- **Use Cases**: OAuth2 authorization flows, OIDC tokens per client, protocol-specific session data

**[Authentication Session Provider](../providers/authentication-sessions.md)**
- **Purpose**: Tracks login flow state during authentication (short-lived)
- **Key Features**: Root/tab structure, execution status tracking, OAuth parameter storage (state, nonce, PKCE)
- **Critical Details**: Browser tab isolation, TTL-based cleanup (5 minutes default), restartSession() reuses root ID
- **Use Cases**: Login forms, MFA flows, SAML/OIDC authentication, device authorization

### 🛡️ Security & Protection

**[Single-Use Object Provider](../providers/singleuse.md)**
- **Purpose**: Manages authorization codes and action tokens (one-time use)
- **Key Features**: Atomic remove operation (prevents replay attacks), TTL expiration, putIfAbsent for uniqueness
- **Critical Details**: Redis DEL is atomic (get-and-delete), prevents double consumption
- **Use Cases**: OAuth2 auth codes, password reset tokens, email verification links, CSRF tokens

**[Login Failure Provider](../providers/login-failure.md)**
- **Purpose**: Tracks failed login attempts for brute force protection
- **Key Features**: Progressive lockouts, IP tracking, automatic expiration, temporary and permanent lockouts
- **Critical Details**: Eager write pattern (security-critical), no optimistic locking (best-effort counters)
- **Use Cases**: Account lockouts, rate limiting, suspicious activity detection, brute force mitigation

### 🌐 Cluster Coordination

**[Cluster Provider](../providers/cluster.md)**
- **Purpose**: Distributed coordination across multiple Keycloak nodes
- **Key Features**: Redis Pub/Sub for events, distributed locking (SETNX), node ID tracking, cluster startup time
- **Critical Details**: Pattern subscription (kc:cluster:*), echo prevention (sender filters own events)
- **Use Cases**: Session invalidation broadcasts, distributed task coordination, cluster health monitoring

---

## Implementation Patterns Across Providers

### Common Design Patterns

**1. Deferred Write Pattern** (User Sessions, Client Sessions, Auth Sessions)
- **How**: Batches multiple modifications into single Redis write per HTTP request
- **When Used**: Session updates (e.g., `setNote()`, `setLastSessionRefresh()`, token storage)
- **Benefit**: Reduces Redis load by 10x+ (e.g., 10 updates = 1 write)
- **Implementation**: Transaction callbacks via `markModified()` → `persist()` → `redis.put()` (plain PSETEX, no CAS)
- **Common Use Cases**:
    - Storing OAuth2 tokens in client sessions (`ACCESS_TOKEN`, `REFRESH_TOKEN`, `ID_TOKEN` notes)
    - Updating session refresh timestamps
    - Storing SSO session attributes
    - Updating protocol-specific data
- **Trade-off**: In-memory updates, persisted at transaction commit (last-write-wins on conflicts)

**2. Eager Write Pattern** (Login Failures)
- **How**: Immediate write to Redis on every modification
- **Benefit**: Guaranteed persistence for security-critical data
- **Implementation**: Every setter calls `save()` immediately
- **Trade-off**: More Redis writes, but no data loss on crash

**3. Optimistic Locking** (User Sessions, Client Sessions)

- **How**: CAS (Compare-And-Set) Lua script with version counter on each entity
- **When Used**: Session creation only (e.g., `createUserSession()`, `createClientSession()`)
- **Benefit**: Prevents duplicate session creation in multi-node deployments
- **Implementation**:
    - CAS Lua script executes: `GET version → PSETEX data → SET version+1 → PEXPIRE version`
    - Version key: `kc:{cache}:{id}:_ver`
    - Version 0 = "create only if doesn't exist" (equivalent to SETNX)
- **Common Use Cases**:
    - User login (create user session atomically)
    - Application authorization (create client session atomically)
    - Prevent race conditions when multiple nodes process same login simultaneously
- **Trade-off**: Requires version tracking, small overhead for CAS script execution
- **Note**: Session updates use deferred write (plain PSETEX, no version check) for performance

**4. Atomic Operations** (Single-Use Objects)
- **How**: Redis DEL returns deleted value atomically
- **Benefit**: Prevents replay attacks (authorization code reuse)
- **Implementation**: `redis.remove()` wraps DEL command
- **Trade-off**: None — Redis guarantees atomicity

**5. TTL-Based Expiration** (All Providers)
- **How**: Redis PSETEX sets millisecond-precision TTL on every write
- **Benefit**: Automatic cleanup (no background jobs needed), memory management by Redis
- **Implementation**: Every `put()` includes TTL parameter
- **Trade-off**: TTL resets on every write (persistent attackers reset expiration)


### When to Use Each Provider Doc

| Your Goal | Read This Provider Doc |
|-----------|------------------------|
| Understand Redis connection infrastructure | [Redis Connection](../providers/redis-connection.md) |
| Learn Lettuce client configuration | [Redis Connection](../providers/redis-connection.md) |
| Optimize Redis connection pooling | [Redis Connection](../providers/redis-connection.md) |
| Monitor Redis latency and metrics | [Redis Connection](../providers/redis-connection.md) |
| Understand how user login sessions work | [User Sessions](../providers/user-sessions.md) |
| Learn OAuth2/OIDC token management | [Client Sessions](../providers/client-sessions.md) |
| Debug login flow issues (credentials, MFA) | [Authentication Sessions](../providers/authentication-sessions.md) |
| Implement action tokens or auth codes | [Single-Use Objects](../providers/singleuse.md) |
| Configure brute force protection | [Login Failures](../providers/login-failure.md) |
| Setup multi-node Keycloak cluster | [Cluster](../providers/cluster.md) |
| Optimize Redis write performance | [Client Sessions](../providers/client-sessions.md) - Deferred Write Pattern |
| Prevent replay attacks | [Single-Use Objects](../providers/singleuse.md) - Atomic Remove Operation |
| Troubleshoot session expiration | [User Sessions](../providers/user-sessions.md) - TTL Management |
| Track failed login attempts | [Login Failures](../providers/login-failure.md) - Progressive Lockouts |
| Implement distributed locking | [Cluster](../providers/cluster.md) - SETNX Pattern |
| Handle concurrent session updates | [User Sessions](../providers/user-sessions.md) - Optimistic Locking |

---

## See Also

- [High-Level Architecture Diagram](architecture-high-level.md) - Visual system overview
- [SPI Implementation Diagram](architecture-spi.md) - Provider interface details
- [Session Creation Flow](data-flow-session-creation.md) - Step-by-step sequence