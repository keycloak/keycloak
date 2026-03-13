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

# 🧪 ATDD Test Suite Overview

**Acceptance Test-Driven Development (ATDD)** for Keycloak Redis Session Provider

---

## 📋 Table of Contents

1. [What is ATDD?](#what-is-atdd)
2. [Architecture](#architecture)
3. [Test Coverage](#test-coverage)
4. [Quick Start](#quick-start)
5. [Detailed Execution Guide](#detailed-execution-guide)
6. [Advanced Usage](#advanced-usage)
7. [Development Workflows](#development-workflows)
8. [Troubleshooting](#troubleshooting)

---

## 🎯 What is ATDD?

The ATDD test suite provides **end-to-end acceptance testing** for the Keycloak Redis Session Provider, ensuring that:

- ✅ Sessions are correctly stored and retrieved from Redis
- ✅ Provider works with both **standalone Redis** and **Redis Cluster**
- ✅ Session lifecycle (create, update, delete, expire) works correctly
- ✅ Authentication flows persist session data properly
- ✅ Cluster failover and network partitions are handled gracefully

### Key Features

- **BDD-Style Tests:** Written in Gherkin (Cucumber) for readability
- **Dual-Mode Testing:** Validates both standalone and cluster Redis configurations
- **Real Integration:** Tests against actual Keycloak, Redis, and PostgreSQL instances
- **Docker-Based:** Reproducible environments using Docker Compose
- **33 Comprehensive Tests:** Covering all major session management scenarios

---

## 🏗️ Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                    ATDD Test Suite                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌─────────────┐  │
│  │   Cucumber   │───▶│     Test     │───▶│   Docker    │  │
│  │  Step Defs   │    │   Context    │    │  Compose    │  │
│  └──────────────┘    └──────────────┘    └─────────────┘  │
│         │                    │                    │         │
│         ▼                    ▼                    ▼         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           Test Infrastructure Services               │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  • Keycloak (with Redis Provider)                    │  │
│  │  • Redis (Standalone or Cluster)                     │  │
│  │  • PostgreSQL (Keycloak DB)                          │  │
│  │  • Redis Commander (UI)                              │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Test Infrastructure

#### Standalone Mode
- **Redis:** Single instance on `localhost:16379`
- **Keycloak:** `localhost:18080`
- **PostgreSQL:** `localhost:15432`
- **Redis Commander:** `localhost:18081`
- **Test Execution:** From host machine (fast, direct access)

#### Cluster Mode
- **Redis Cluster:** 6 nodes (3 masters + 3 replicas) on `localhost:16379-16390`
- **Keycloak:** `localhost:18080`
- **PostgreSQL:** `localhost:15432`
- **Redis Commander:** `localhost:18081`
- **Test Execution:** Inside Docker container (cluster network access required)

---

## 📊 Test Coverage

### Test Scenarios (33 Total)

#### 1. **User Session Management** (10 tests)
- Create session on login
- Retrieve session by ID
- Update session attributes
- Session persistence across restarts
- Session expiration (TTL)
- Offline sessions
- Multiple concurrent sessions
- Session removal on logout

#### 2. **Authentication Sessions** (8 tests)
- Create auth session
- Complete login flow (auth → user session)
- Update auth session data
- Add execution state
- Add client notes
- Auth session expiration
- Restart persistence
- Cleanup after login

#### 3. **Client Sessions** (5 tests)
- Create client sessions
- Associate with user sessions
- Update client session data
- Remove client sessions
- Multiple clients per user

#### 4. **Login Failure Tracking** (4 tests)
- Record login failures
- Increment failure count
- IP-based tracking
- Failure expiration

#### 5. **Action Tokens** (3 tests)
- Create single-use tokens
- Consume tokens (single-use enforcement)
- Token expiration

#### 6. **Offline Sessions** (3 tests)
- Create offline sessions
- Extended TTL validation
- Offline client sessions

### Test Tags

Tests are organized with Cucumber tags for selective execution:

- `@critical` - Critical path tests (must always pass)
- `@user-session` - User session tests
- `@auth-session` - Authentication session tests
- `@client-session` - Client session tests
- `@login-failure` - Login failure tracking
- `@action-token` - Action token tests
- `@ttl` - Time-to-live / expiration tests
- `@offline` - Offline session tests
- `@cluster` - Cluster-specific tests

---

## 🚀 Quick Start

### Prerequisites

1. **Docker & Docker Compose** installed
2. **Maven** installed (for building)
3. **Java 17+** installed

### Build the Provider

```bash
cd /path/to/keycloak-redis-provider-impl/model/redis
mvn clean package -DskipTests
```

### Run Tests (Simplest Way)

#### Test with Standalone Redis
```bash
cd src/test/resources
./atdd-env.sh standalone
```

✅ This single command:
- Builds and starts standalone Redis
- Starts Keycloak with the Redis provider
- Starts PostgreSQL
- Waits for all services to be ready
- Runs all 33 tests from host
- Shows results

#### Test with Redis Cluster
```bash
cd src/test/resources
./atdd-env.sh cluster
```

✅ This single command:
- Builds and starts 6-node Redis cluster
- Starts Keycloak with the Redis provider
- Starts PostgreSQL
- Waits for all services to be ready
- Runs all 33 tests inside Docker (cluster network)
- Shows results

---

## 📖 Detailed Execution Guide

### Script Interface

The ATDD environment is managed via `atdd-env.sh` script with two usage patterns:

1. **Simple Mode** - For quick testing (recommended)
2. **Advanced Mode** - For granular control

---

### 1️⃣ Simple Mode (Recommended)

#### Standalone Testing
```bash
./atdd-env.sh standalone
```

**What it does:**
- ✅ Starts standalone Redis
- ✅ Starts Keycloak & PostgreSQL
- ✅ Waits for services to be ready
- ✅ Runs tests from host machine
- ✅ Shows results

**Expected output:**
```
🚀 Running complete standalone test cycle...
✅ Found Redis provider JAR: keycloak-model-redis-999.0.0-SNAPSHOT.jar
🚀 Starting ATDD test environment in standalone mode...
   Redis: ✅
   PostgreSQL: ✅
   Keycloak: ✅
✅ ATDD test environment is ready! (Mode: standalone)

🧪 Running ATDD tests...
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
✅ standalone test cycle complete!
```

#### Cluster Testing
```bash
./atdd-env.sh cluster
```

**What it does:**
- ✅ Starts Redis cluster (6 nodes: 3 masters + 3 replicas)
- ✅ Starts Keycloak & PostgreSQL
- ✅ Waits for services to be ready
- ✅ Runs tests inside Docker container (cluster network access)
- ✅ Shows results

**Expected output:**
```
🚀 Running complete cluster test cycle...
✅ Found Redis provider JAR: keycloak-model-redis-999.0.0-SNAPSHOT.jar
🚀 Starting ATDD test environment in cluster mode...
   Redis: ✅
   PostgreSQL: ✅
   Keycloak: ✅
✅ ATDD test environment is ready! (Mode: cluster)

🐳 Running ATDD tests inside Docker network...
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
✅ cluster test cycle complete!
```

---

### 2️⃣ Advanced Mode (Granular Control)

#### Environment Control

**Start environment only (without tests):**
```bash
./atdd-env.sh start standalone        # Start standalone
./atdd-env.sh start cluster           # Start cluster
```

**Stop environment:**
```bash
./atdd-env.sh stop standalone         # Stop standalone
./atdd-env.sh stop cluster            # Stop cluster
```

**Restart environment:**
```bash
./atdd-env.sh restart cluster         # Restart cluster
```

**Show service status:**
```bash
./atdd-env.sh status cluster          # Check what's running
```

**Clean up (remove containers & volumes):**
```bash
./atdd-env.sh clean standalone        # Remove standalone env
./atdd-env.sh clean cluster           # Remove cluster env
```

#### Testing Control

**Run tests only (environment must be started first):**
```bash
./atdd-env.sh test standalone         # Test standalone
./atdd-env.sh test cluster            # Test cluster (in Docker)
```

**Run specific test tags:**
```bash
./atdd-env.sh test standalone @critical           # Critical tests only
./atdd-env.sh test cluster @user-session          # User session tests
./atdd-env.sh test cluster "@critical and @ttl"   # Combined tags
```

**Run tests from host (cluster mode, advanced):**
```bash
# Start cluster
./atdd-env.sh start cluster

# Run tests from host (faster, but may not work for all cluster tests)
cd /path/to/project
mvn test -Dtest=CucumberTestRunner
```

#### Monitoring & Debugging

**View logs (all services):**
```bash
./atdd-env.sh logs standalone         # Tail all logs
./atdd-env.sh logs cluster            # Tail all logs
```

**View specific service logs:**
```bash
./atdd-env.sh logs cluster keycloak-test      # Keycloak logs
./atdd-env.sh logs cluster redis-cluster-1    # Redis node 1
./atdd-env.sh logs cluster postgres-test      # PostgreSQL logs
```

**Access Redis Commander UI:**
```bash
# Start environment
./atdd-env.sh start cluster

# Open browser to http://localhost:18081
# Login: admin/admin
# View/inspect Redis data in real-time
```

#### Development

**Rebuild provider and restart:**
```bash
./atdd-env.sh rebuild cluster         # Rebuilds JAR + restarts
```

This is useful during development:
1. Make code changes
2. Run `./atdd-env.sh rebuild cluster`
3. Tests run automatically with new code

---

### 🧠 Smart Behaviors

#### Automatic Test Execution Method

The script automatically chooses the correct test execution method:

| Mode | Test Execution | Why? |
|------|----------------|------|
| **Standalone** | From host machine | Fast, direct connection to `localhost:16379` |
| **Cluster** | Inside Docker container | Required for cluster network access to all 6 nodes |

You don't need to remember `test-docker` vs `test` - the script handles it automatically!

#### Automatic Property Configuration

Tests use a **common base + environment override** pattern:

| File | Purpose |
|------|----------|
| `application-test.properties` | Common properties (shared across all environments) |
| `application-test-standalone.properties` | Standalone-specific overrides (localhost:16379) |
| `application-test-docker-cluster.properties` | Cluster-specific overrides (redis-cluster-1:6379) |

The script sets `-Dtest.environment=standalone` or `-Dtest.environment=docker-cluster` to automatically load the correct overrides.

---

## 🔧 Advanced Usage

### Custom Environment Variables

Override default passwords for security:

```bash
# Set custom passwords
export KEYCLOAK_ADMIN_PASSWORD="mySecurePassword123"
export REDIS_COMMANDER_PASSWORD="anotherSecurePass"

# Start environment with custom passwords
./atdd-env.sh start cluster
```

Available environment variables:
- `KEYCLOAK_ADMIN_USER` (default: `admin`)
- `KEYCLOAK_ADMIN_PASSWORD` (default: `admin`)
- `REDIS_COMMANDER_USER` (default: `admin`)
- `REDIS_COMMANDER_PASSWORD` (default: `admin`)

### Service URLs

When environment is running:

| Service | URL | Credentials |
|---------|-----|-------------|
| **Keycloak Admin Console** | http://localhost:18080/admin | admin/admin |
| **Test Realm** | http://localhost:18080/realms/test-realm | - |
| **Redis Commander** | http://localhost:18081 | admin/admin |
| **PostgreSQL** | localhost:15432 | keycloak/password |
| **Redis (Standalone)** | localhost:16379 | - |
| **Redis Cluster** | localhost:16379-16390 | - |

### Direct Maven Test Execution

For advanced debugging or IDE integration:

```bash
# Start environment
./atdd-env.sh start standalone

# Run tests via Maven
cd /path/to/project
mvn test -Dtest=CucumberTestRunner

# Run with specific tags
mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@critical"

# Run with debug
mvn test -Dtest=CucumberTestRunner -Dmaven.surefire.debug
```

---

## 💼 Development Workflows

### Workflow 1: Quick Validation

**Use case:** Quickly validate that both modes work

```bash
cd src/test/resources

# Test standalone
./atdd-env.sh standalone

# Test cluster
./atdd-env.sh cluster

# Clean up
./atdd-env.sh clean standalone
./atdd-env.sh clean cluster
```

**Time:** ~3-4 minutes total

---

### Workflow 2: Active Development

**Use case:** Making code changes and testing iteratively

```bash
cd src/test/resources

# Start cluster environment once
./atdd-env.sh start cluster

# --- Make code changes in IDE ---

# Quick test (don't restart environment)
./atdd-env.sh test cluster

# --- More code changes ---

# Full rebuild and test
./atdd-env.sh rebuild cluster

# View logs if there are issues
./atdd-env.sh logs cluster keycloak-test

# Clean up when done
./atdd-env.sh clean cluster
```

**Time:** 
- Initial start: ~30s
- Quick test: ~45s
- Rebuild: ~1-2 min
- Total iteration time: <2 min

---

### Workflow 3: Debugging Specific Tests

**Use case:** Investigating a failing test scenario

```bash
cd src/test/resources

# Start environment with the failing mode
./atdd-env.sh start cluster

# Open Redis Commander in browser
open http://localhost:18081

# Run specific test tag
./atdd-env.sh test cluster @user-session

# Watch Redis data in Commander during test execution

# View detailed logs
./atdd-env.sh logs cluster keycloak-test

# Keep environment running for investigation
# ... debug code ...

# Test again
./atdd-env.sh test cluster @user-session

# Clean up
./atdd-env.sh stop cluster
```

---

### Workflow 4: CI/CD Pipeline

**Use case:** Automated testing in CI/CD

```bash
#!/bin/bash
set -e

cd src/test/resources

# Set secure passwords for CI
export KEYCLOAK_ADMIN_PASSWORD="${CI_KEYCLOAK_PASSWORD}"
export REDIS_COMMANDER_PASSWORD="${CI_REDIS_PASSWORD}"

# Test standalone
./atdd-env.sh standalone
./atdd-env.sh clean standalone

# Test cluster
./atdd-env.sh cluster
./atdd-env.sh clean cluster

echo "✅ All ATDD tests passed"
```

---

## 🛠️ Troubleshooting

### Common Issues

#### Issue: "JAR not found"

**Error:**
```
❌ Redis provider JAR not found in target/
```

**Solution:**
```bash
cd /path/to/project
mvn clean package -DskipTests
```

---

#### Issue: Port conflicts

**Error:**
```
Error: Port 18080 is already in use
```

**Solution:**
```bash
# Check what's using the port
lsof -i :18080

# Stop conflicting service or use clean command
./atdd-env.sh clean standalone
./atdd-env.sh clean cluster
```

---

#### Issue: Services not ready

**Error:**
```
❌ Keycloak not ready after timeout
```

**Solution:**
```bash
# Check service status
./atdd-env.sh status cluster

# Check logs
./atdd-env.sh logs cluster keycloak-test

# Restart environment
./atdd-env.sh restart cluster
```

---

#### Issue: Test failures in cluster mode

**Error:**
```
WRONGTYPE Operation against a key holding the wrong kind of value
```

**Solution:**
This is a data type mismatch. Ensure:
1. Redis cluster is properly initialized
2. Tests are running inside Docker (not from host)
3. Correct properties file is being used

```bash
# Clean and restart
./atdd-env.sh clean cluster
./atdd-env.sh cluster
```

---

#### Issue: Docker out of space

**Error:**
```
Error response from daemon: no space left on device
```

**Solution:**
```bash
# Clean up all test environments
./atdd-env.sh clean standalone
./atdd-env.sh clean cluster

# Prune unused Docker resources
docker system prune -a --volumes
```

---

### Debug Mode

Enable verbose output:

```bash
# Add -x for bash debug mode
bash -x ./atdd-env.sh cluster
```

---

## 📚 Additional Resources

### Configuration Files

- **Properties:**
  - `application-test.properties` - Common properties (credentials, realms, users, timeouts)
  - `application-test-standalone.properties` - Standalone overrides (localhost:16379, cluster=false)
  - `application-test-docker-cluster.properties` - Cluster overrides (redis-cluster-1:6379, cluster=true)

- **Docker Compose:**
  - `docker-compose-atdd-standalone.yml` - Standalone Redis services
  - `docker-compose-atdd-cluster.yml` - Redis Cluster services (6 nodes)

- **Test Features:**
  - `src/test/resources/features/*.feature` - Gherkin test scenarios

- **Step Definitions:**
  - `src/test/java/org/keycloak/models/redis/test/steps/` - Test implementation

### Key Classes

- `TestContext.java` - Shared test context and Redis connection management
- `RedisTestCommands.java` - Unified Redis command interface (standalone + cluster)
- `RedisKeyConstants.java` - Centralized Redis key naming
- `CucumberTestRunner.java` - Main test runner

---

## 📊 Test Results

### Success Criteria

All tests must pass in **both** standalone and cluster modes:

```
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Test Reports

After test execution, detailed reports are available:

- **Surefire Reports:** `target/surefire-reports/`
- **Cucumber JSON:** `target/cucumber-reports/`
- **JaCoCo Coverage:** `target/jacoco.exec`

### Viewing Reports

```bash
# HTML report
open target/surefire-reports/index.html

# Cucumber report
open target/cucumber-reports/cucumber.html
```

---

## ✅ Summary

### Simple Usage (90% of cases)

```bash
./atdd-env.sh standalone    # Test standalone Redis
./atdd-env.sh cluster       # Test Redis cluster
```

### Advanced Usage (Power users)

```bash
./atdd-env.sh start cluster           # Start only
./atdd-env.sh test cluster @critical  # Test with tags
./atdd-env.sh logs cluster            # Monitor logs
./atdd-env.sh rebuild cluster         # Rebuild + restart
./atdd-env.sh clean cluster           # Clean up
```

### Key Benefits

- ✅ **Simple:** Two commands for 90% of use cases
- ✅ **Smart:** Automatically handles cluster vs standalone differences
- ✅ **Complete:** 33 comprehensive tests covering all scenarios
- ✅ **Fast:** Iterative testing without full restarts
- ✅ **Reliable:** Docker-based reproducible environments

---

**Need help?** Run `./atdd-env.sh help` for inline documentation
