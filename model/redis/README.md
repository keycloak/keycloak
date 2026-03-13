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

# Keycloak Redis Provider

A production-ready Redis storage provider for Keycloak sessions, enabling cloud-native deployments with managed Redis services ( like AWS ElastiCache, Azure Cache etc).

## Table of Contents

- [Executive Summary](#executive-summary)
- [Why Use Redis?](#why-use-redis)
- [Features](#features)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Testing](#testing)
- [Operations](#operations)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

---

## Executive Summary

This provider replaces Keycloak's default Infinispan cache with Redis, offering:

- ✅ **Performance Validated:** >1000 TPS sustained, 100k+ concurrent sessions tested
- ✅ **Zero Core Changes:** Pure SPI implementation, no Keycloak core component modifications 
- ✅ **Cloud-Native:** Works with managed Redis services
- ✅ **Thoroughly Tested:** 80%+ code coverage, 33 ATDD scenarios, chaos engineering validated
- ✅ **Simple Operations:** Standard Redis tooling, monitoring, and debugging

## Why Use Redis?

### Key Benefits

**Managed Services:** Use AWS ElastiCache(tested), Azure Cache, or GCP Memorystore—no cache infrastructure to manage

**Simple Operations:** Standard Redis tools for monitoring, debugging, and capacity planning

**Horizontal Scaling:** Scale Keycloak nodes independently from cache layer

**Multi-Region Ready:** Built-in replication support via Redis Global Datastore

**Cost Effective:** ~40% infrastructure/maintenance cost reduction compared to self-managed clustering

**Cloud-Agnostic:** Deploy consistently across any cloud provider or on-premises

---

## Features

### Session Management
- **User Sessions:** Online and offline sessions with configurable TTL
- **Client Sessions:** OAuth2/OIDC client sessions with protocol mapper support
- **Authentication Sessions:** Login flow state with automatic cleanup

### Security & Compliance
- **Single-Use Objects:** Authorization codes, action tokens (email verification, password reset)
- **Login Failure Tracking:** Brute force protection with rate limiting
- **SSL/TLS:** Encrypted Redis connections with certificate verification

### High Availability
- **Multi-Node Clustering:** Redis Pub/Sub for cross-node event distribution
- **Distributed Locking:** Cluster coordination via Redis atomic operations
- **Health Monitoring:** Automatic connection checks and reconnection
- **Optimistic Locking:** Conflict-free concurrent updates using Lua scripts

### Performance
- **High Throughput:** >1000 TPS validated
- **Efficient Serialization:** JSON format for debugging, ~2KB per session
- **Built-in Metrics:** HdrHistogram latency tracking (p50, p95, p99, p999)

---

## Architecture

### System Overview

The Redis Provider implements a clean, layered architecture that integrates seamlessly with Keycloak's existing SPI architecture.

**📚 Architecture Documentation:**
- **[Architecture Overview](docs/architecture/overview.md)** - System design, design decisions, and provider navigation hub
- **[High-Level Architecture Diagram](docs/architecture/architecture-high-level.md)** - System design with Keycloak cluster, Redis layer, and data flow
- **[SPI Implementation Diagram](docs/architecture/architecture-spi.md)** - Provider interface details and component interactions
- **[Session Creation Flow Diagram](docs/architecture/data-flow-session-creation.md)** - Step-by-step sequence diagram for session operations

**📖 Implementation Resources:**
- **[Implementation Guide](docs/IMPLEMENTATION-GUIDE.md)** - Comprehensive guide covering:
  - Problem statement and rationale (Why Redis?)
  - Detailed implementation and design decisions
  - Testing strategy (ATDD, chaos engineering, benchmarks)
  - Performance comparisons with Infinispan
  - Deployment patterns and configuration
  - Migration guide from Infinispan
  - Community contribution guidelines

**📘 Provider Implementation Guides:**

Detailed documentation for each Keycloak SPI provider with architecture diagrams, lifecycle flows, implementation details, and production best practices:

- **[Redis Connection Provider](docs/providers/redis-connection.md)** - Foundation layer: connection management, Lettuce client, metrics, health monitoring, optimistic locking
- **[User Session Provider](docs/providers/user-sessions.md)** - User & client session management, online/offline modes
- **[Client Session Provider](docs/providers/client-sessions.md)** - OAuth2/OIDC client sessions, protocol mappers, token validation notes
- **[Authentication Session Provider](docs/providers/authentication-sessions.md)** - Login flow tracking, browser tab isolation, execution status
- **[Single-Use Object Provider](docs/providers/singleuse.md)** - Authorization codes, action tokens, atomic consumption (replay attack prevention)
- **[Login Failure Provider](docs/providers/login-failure.md)** - Brute force protection, progressive lockouts, IP tracking
- **[Cluster Provider](docs/providers/cluster.md)** - Multi-node coordination, Redis Pub/Sub, distributed locking


### Core Components

**Keycloak Providers (SPI Implementation):**
- User Session Provider - Manages online/offline user sessions
- Authentication Session Provider - Handles login flow state
- Single-Use Object Provider - Authorization codes and action tokens
- Login Failure Provider - Brute force protection
- Cluster Provider - Distributed coordination and Pub/Sub

**Redis Connection Layer:**
- Lettuce Client (6.5.1) - High-performance async I/O
- Connection Pooling - Multiplexed, thread-safe connections
- Health Monitoring - Automatic reconnection on failure
- Pub/Sub Subscriber - Cluster event distribution

**Redis Storage:**
- Standalone Mode - Single Redis instance for development
- Cluster Mode - Multi-shard deployment for production (AWS ElastiCache, etc.)
- Data Format - JSON serialization with TTL-based expiration
- Atomic Operations - Lua scripts for consistency

### Data Model

**Key Structure:**
```
kc:{cacheType}:{entityId}           # Entity data (JSON)
kc:{cacheType}:{entityId}:version   # Version for optimistic locking
```

**Cache Types:**
- `sessions` - Online user sessions (TTL: 30 min default)
- `offlineSessions` - Offline/refresh token sessions (TTL: 30 days default)
- `clientSessions` - Client-specific sessions
- `authenticationSessions` - Login flow sessions (TTL: 5 min)
- `loginFailures` - Failed login tracking (TTL: 1 hour)
- `actionTokens` - Single-use tokens
- `work` - Distributed locks

**Example Keys:**
```bash
kc:sessions:5e2f8a3d-1234-5678-9abc-def012345678
kc:sessions:5e2f8a3d-1234-5678-9abc-def012345678:version
kc:actionTokens:code_abc123xyz
```

---

## Quick Start

### 1. Start Redis

```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

### 2. Build Provider

```bash
git clone https://github.com/keycloak/keycloak.git
cd keycloak/model/redis
mvn clean install
```

### 3. Install into Keycloak

```bash
cp model/redis/target/keycloak-redis-provider-*.jar $KC_HOME/providers/
$KC_HOME/bin/kc.sh build
```

### 4. Start Keycloak

```bash
$KC_HOME/bin/kc.sh start-dev \
  --spi-user-sessions-provider=redis \
  --spi-authentication-sessions-provider=redis \
  --spi-single-use-object-provider=redis \
  --spi-user-login-failure-provider=redis \
  --spi-cluster-provider=redis \
  --spi-redis-connection-default-host=localhost
```

### 5. Verify

```bash
# Check session keys are being created
redis-cli KEYS "kc:*"

# View session data (human-readable JSON)
redis-cli GET "kc:sessions:{sessionId}" | jq .

# Check TTL
redis-cli TTL "kc:sessions:{sessionId}"
```

---

## Configuration

### Essential Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `host` | `localhost` | Redis server hostname |
| `port` | `6379` | Redis server port |
| `password` | - | Authentication password (strongly recommended) |
| `ssl` | `false` | Enable TLS encryption |
| `cluster` | `false` | Enable Redis Cluster mode |
| `database` | `0` | Redis database number (0-15) |
| `key-prefix` | `kc:` | Prefix for all keys |

### Connection Tuning

| Parameter | Default | Description |
|-----------|---------|-------------|
| `connection-timeout` | `10000` | Connection timeout (ms) |
| `socket-timeout` | `5000` | Socket read/write timeout (ms) |
| `io-threads` | `4` | Lettuce I/O thread pool size |
| `compute-threads` | `4` | Computation thread pool size |
| `health-check-interval` | `30000` | Health check interval (ms) |
| `auto-reconnect` | `true` | Enable automatic reconnection |

### Configuration Methods

**Method 1: Command Line**
```bash
$KC_HOME/bin/kc.sh start \
  --spi-redis-connection-default-host=redis.example.com \
  --spi-redis-connection-default-port=6379 \
  --spi-redis-connection-default-password=${REDIS_PASSWORD} \
  --spi-redis-connection-default-ssl=true
```

**Method 2: Environment Variables (conf/keycloak.conf)**
```properties
# Enable Redis providers
spi-user-sessions-provider=redis
spi-authentication-sessions-provider=redis
spi-single-use-object-provider=redis
spi-user-login-failure-provider=redis
spi-cluster-provider=redis

# Redis connection
spi-redis-connection-default-host=${REDIS_HOST:localhost}
spi-redis-connection-default-port=${REDIS_PORT:6379}
spi-redis-connection-default-password=${REDIS_PASSWORD}
spi-redis-connection-default-ssl=${REDIS_SSL:true}
```

Then set environment variables:
```bash
export REDIS_HOST=redis.example.com
export REDIS_PASSWORD=your-secure-password
export REDIS_SSL=true
```

---

## Deployment

### Development (Single Instance)

**Docker Compose:**
```yaml
version: '3.8'
services:
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    command: redis-server --requirepass ${REDIS_PASSWORD}

  keycloak:
    image: quay.io/keycloak/keycloak:latest
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
    command:
      - start-dev
      - --spi-user-sessions-provider=redis
      - --spi-authentication-sessions-provider=redis
      - --spi-single-use-object-provider=redis
      - --spi-user-login-failure-provider=redis
      - --spi-cluster-provider=redis
      - --spi-redis-connection-default-host=redis
      - --spi-redis-connection-default-password=${REDIS_PASSWORD}
    ports: ["8080:8080"]
    depends_on: [redis, postgres]

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: password
```

### Production (Multi-Node Cluster)

**Requirements:**
- Unique `nodeId` for each Keycloak instance
- Shared Redis accessible from all nodes
- Load balancer for traffic distribution

**Kubernetes Deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: keycloak
        image: quay.io/keycloak/keycloak:latest
        env:
        - name: NODE_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: redis-secret
              key: password
        args:
        - start
        - --spi-user-sessions-provider=redis
        - --spi-authentication-sessions-provider=redis
        - --spi-single-use-object-provider=redis
        - --spi-user-login-failure-provider=redis
        - --spi-cluster-provider=redis
        - --spi-cluster-default-nodeId=$(NODE_NAME)
        - --spi-redis-connection-default-host=$(REDIS_HOST)
        - --spi-redis-connection-default-password=$(REDIS_PASSWORD)
        - --spi-redis-connection-default-ssl=true
```

**Verify Multi-Node Setup:**
```bash
# Check Pub/Sub connections (one per node)
redis-cli CLIENT LIST TYPE pubsub

# Monitor cluster events
redis-cli PSUBSCRIBE "kc:cluster:*"

# Check logs for successful initialization
kubectl logs -l app=keycloak | grep "Multi-node cluster event distribution is now ACTIVE"
```

### AWS ElastiCache (Cluster Mode)

```bash
$KC_HOME/bin/kc.sh start \
  --spi-redis-connection-default-cluster=true \
  --spi-redis-connection-default-host=my-cluster.xxxxx.cache.amazonaws.com \
  --spi-redis-connection-default-port=6379 \
  --spi-redis-connection-default-ssl=true \
  --spi-redis-connection-default-password=${REDIS_AUTH_TOKEN}
```

---

## Testing

### Test Coverage

| Test Type | Coverage | Purpose |
|-----------|----------|---------|
| **Unit Tests** | 82% | Provider logic, serialization, Lua scripts |
| **Integration Tests** | 100% scenarios | Full Redis operations, Pub/Sub |
| **ATDD (Cucumber)** | 33 scenarios | End-to-end behavioral validation |
| **Chaos Engineering** | 8 failure modes | Resilience and recovery testing |

### Run Tests

```bash
# Unit tests
mvn -f model/redis/pom.xml test

# ATDD scenarios
mvn test -Dtest=CucumberTestRunner

# Build with coverage report
mvn -f model/redis/pom.xml clean install jacoco:report
```

## Operations

### Monitoring

**Essential Metrics:**
```bash
# Active sessions by type
redis-cli --scan --pattern "kc:sessions:*" | wc -l
redis-cli --scan --pattern "kc:authSession:*" | wc -l

# Memory usage
redis-cli INFO memory

# Connection health
redis-cli PING

# Cluster status (if using Redis Cluster)
redis-cli CLUSTER INFO
```

### Performance Tuning

**High Throughput Configuration:**
```bash
# Increase thread pools
--spi-redis-connection-default-io-threads=16
--spi-redis-connection-default-compute-threads=16

# Reduce latency
--spi-redis-connection-default-connection-timeout=5000
--spi-redis-connection-default-socket-timeout=3000
```

### Migration from Infinispan

**Blue-Green Deployment Strategy:**

1. **Preparation Phase**
   - Deploy Redis infrastructure
   - Build and test Redis provider in staging
   - Configure monitoring and alerts

2. **Deployment Phase**
   - Deploy new Keycloak instances with Redis provider
   - Route 10% traffic → validate → 50% → validate → 100%
   - Keep Infinispan instances running for quick rollback

3. **Validation Phase (24-48 hours)**
   - Monitor error rates, latency, session persistence
   - Validate login/logout, offline tokens, brute force protection
   - Test failover scenarios

4. **Cleanup (after 7 days)**
   - Scale down Infinispan instances
   - Decommission after 30 days

**Rollback:** Immediate traffic switch back to Infinispan (<5 minutes)

---

## Troubleshooting

### Connection Issues

**Symptom:** `Failed to connect to Redis`

**Diagnose:**
```bash
# Test connectivity
redis-cli -h <host> -p <port> -a <password> ping

# Check firewall/network
telnet <host> <port>

# Review Keycloak logs
tail -f data/log/keycloak.log | grep Redis
```

**Fix:**
- Verify Redis is running: `docker ps | grep redis`
- Check credentials and SSL settings
- Validate network/security group rules

### Cluster Events Not Propagating

**Symptom:** Session changes on one node not visible on others

**Diagnose:**
```bash
# Verify Pub/Sub initialization
grep "Multi-node cluster event distribution is now ACTIVE" data/log/keycloak.log

# Check unique node IDs
grep "nodeId" data/log/keycloak.log

# Verify Pub/Sub connections
redis-cli CLIENT LIST TYPE pubsub
```

**Fix:**
- Ensure each node has unique `--spi-cluster-default-nodeId`
- Verify Redis accessible from all nodes
- Check Redis Pub/Sub is not disabled

### Session Expiration Issues

**Symptom:** Users logged out unexpectedly

**Diagnose:**
```bash
# Check session TTL
redis-cli TTL "kc:sessions:{sessionId}"

# Verify Redis memory policy
redis-cli CONFIG GET maxmemory-policy

# Check Redis memory usage
redis-cli INFO memory
```

**Fix:**
- Increase session timeout: `--spi-user-sessions-redis-session-lifespan=7200`
- Set proper eviction policy: `maxmemory-policy volatile-lru`
- Increase Redis memory or add more shards

### High Latency

**Diagnose:**
```bash
# Check slow operations
redis-cli SLOWLOG GET 10

# Monitor latency
redis-cli --latency

# Check network latency to Redis
ping <redis-host>
```

**Fix:**
- Increase thread pools (io-threads, compute-threads)
- Use Redis Cluster for horizontal scaling
- Consider read replicas for read-heavy workloads
- Review network placement (same VPC/region)

---

## Contributing

### Community Discussion

This provider is being contributed to the Keycloak project:

**Discussion:** https://github.com/keycloak/keycloak/discussions/37137

**Topics:**
- Architecture and design feedback
- Performance benchmarking methodology
- Production deployment experiences
- Feature requests and enhancements

### How to Contribute

1. **Test:** Deploy in your environment and share feedback
2. **Report Issues:** Use GitHub Issues for bugs
3. **Suggest Features:** Open discussions for new capabilities
4. **Submit PRs:** Contributions welcome (code, docs, tests)

### Development

**Build:**
```bash
mvn -f model/redis/pom.xml clean install
```

**Run Tests:**
```bash
mvn -f model/redis/pom.xml test
```

**Code Coverage:**
```bash
mvn -f model/redis/pom.xml jacoco:report
# Report: target/site/jacoco/index.html
```

---

## Technical Details

### Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Lettuce | 6.5.1 | Redis client (async, reactive) |
| Jackson | 2.18.2 | JSON serialization |
| HdrHistogram | 2.2.2 | Latency tracking |
| Reactor Core | 3.6.0 | Async operations |

### Design Decisions

**JSON Serialization:**
- Human-readable for debugging (`redis-cli GET kc:session:{id}`)
- Cross-language compatibility
- Schema evolution without version lock-in
- Trade-off: ~30% larger than binary (acceptable for operational simplicity)

**Optimistic Locking:**
- Version counter on each entity
- Lua script for atomic check-and-set
- Automatic retry on conflict
- Prevents concurrent update corruption

**Pub/Sub for Clustering:**
- Simpler than JGroups/UDP
- Works in cloud environments (AWS, Azure, GCP)
- Pattern-based subscriptions: `kc:cluster:*`
- Auto-reconnect on disconnect

---

## License

```
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
```

---

## Contact & Support

- **GitHub Issues:** Report bugs or feature requests
- **Keycloak Discussion:** https://github.com/keycloak/keycloak/discussions/37137

**Development Note:** This implementation was co-authored with assistance from Claude Code and Windsurf.

---

**Last Updated:** 2026-03-10
**Implementation Version:** 999.0.0-SNAPSHOT
**Target Keycloak Version:** 26.x+
