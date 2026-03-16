#!/bin/bash

# ATDD Test Environment Management Script
# Simplified Usage: ./atdd-env.sh {standalone|cluster|start|stop|status|logs|clean}
# Default: standalone (start + test)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}/../../.."

# Parse command to determine mode
COMMAND="${1:-standalone}"
MODE=""

# Simplified commands: 'standalone' or 'cluster' runs everything
case "$COMMAND" in
    standalone|cluster)
        MODE="$COMMAND"
        ;;
    start|stop|restart|status|clean|rebuild)
        # Traditional commands - check for mode in $2
        MODE="${2:-standalone}"
        ;;
    logs|test)
        # Commands that need mode context
        MODE="${2:-standalone}"
        ;;
    *)
        MODE="standalone"
        ;;
esac

# Set compose file based on mode
if [ "$MODE" = "cluster" ]; then
    COMPOSE_FILE="${SCRIPT_DIR}/docker-compose-atdd-cluster.yml"
else
    COMPOSE_FILE="${SCRIPT_DIR}/docker-compose-atdd-standalone.yml"
    MODE="standalone"
fi

function find_and_export_jar() {
    # Find the Redis provider JAR using wildcard pattern
    JAR_FILE=$(find "${PROJECT_ROOT}/target" -maxdepth 1 -name "keycloak-model-redis-*.jar" -not -name "*-sources.jar" -not -name "*-tests.jar"  -type f | head -n 1)

    if [ -z "$JAR_FILE" ]; then
        echo "❌ Redis provider JAR not found in ${PROJECT_ROOT}/target/"
        echo "   Looking for: keycloak-model-redis-*.jar"
        echo "   Please build the project first:"
        echo "   cd ${PROJECT_ROOT} && mvn clean package -DskipTests"
        exit 1
    fi

    # Export as environment variable for docker-compose
    export REDIS_PROVIDER_JAR="$JAR_FILE"
    echo "✅ Found Redis provider JAR: $(basename $JAR_FILE)"
}

function check_jar() {
    find_and_export_jar
}

function start_environment() {
    echo "🚀 Starting ATDD test environment in $MODE mode..."
    echo "   Using compose file: $(basename $COMPOSE_FILE)"
    echo "   Using properties: $PROPERTIES_FILE"
    check_jar
    
    # Stop the other mode's containers to prevent port conflicts
    if [ "$MODE" = "cluster" ]; then
        # Starting cluster, check if standalone is running
        if docker ps --format '{{.Names}}' | grep -q "redis-atdd-test"; then
            echo "   Stopping standalone containers to avoid port conflicts..."
            docker-compose -f "${SCRIPT_DIR}/docker-compose-atdd.yml" down 2>/dev/null || true
        fi
    else
        # Starting standalone, check if cluster is running
        if docker ps --format '{{.Names}}' | grep -q "redis-cluster-1"; then
            echo "   Stopping cluster containers to avoid port conflicts..."
            docker-compose -f "${SCRIPT_DIR}/docker-compose-atdd-cluster.yml" down 2>/dev/null || true
        fi
    fi
    
    docker-compose -f "$COMPOSE_FILE" up -d --remove-orphans
    
    echo ""
    echo "⏳ Waiting for services to be healthy..."
    sleep 10
    
    # Wait for Redis
    echo -n "   Redis: "
    if [ "$MODE" = "cluster" ]; then
        timeout 30 bash -c 'until docker exec redis-cluster-1 redis-cli -p 6379 ping 2>/dev/null | grep -q PONG; do sleep 1; done' && echo "✅" || echo "❌"
    else
        timeout 30 bash -c 'until docker exec redis-atdd-test redis-cli ping 2>/dev/null | grep -q PONG; do sleep 1; done' && echo "✅" || echo "❌"
    fi
    
    # Wait for PostgreSQL
    echo -n "   PostgreSQL: "
    timeout 30 bash -c 'until docker exec postgres-atdd-test pg_isready -U keycloak 2>/dev/null | grep -q "accepting connections"; do sleep 1; done' && echo "✅" || echo "❌"
    
    # Wait for Keycloak
    echo -n "   Keycloak: "
    timeout 180 bash -c 'until curl -sf http://localhost:18080/health/ready >/dev/null 2>&1; do sleep 3; done' && echo "✅" || echo "❌"
    
    # Extra wait to ensure Keycloak is fully initialized
    echo "   Waiting for Keycloak initialization..."
    sleep 5
    
    echo ""
    echo "✅ ATDD test environment is ready! (Mode: $MODE)"
    echo ""
    echo "📊 Service URLs:"
    echo "   Keycloak Admin: http://localhost:18080/admin (admin/admin)"
    echo "   Test Realm: http://localhost:18080/realms/test-realm"
    echo "   Redis Commander: http://localhost:18081 (admin/admin)"
    if [ "$MODE" = "cluster" ]; then
        echo "   Redis Cluster: localhost:16379-16390 (6 nodes: 3 masters + 3 replicas)"
    else
        echo "   Redis: localhost:16379 (standalone)"
    fi
    echo "   PostgreSQL: localhost:15432"
    echo ""
    echo "🧪 Run tests with:"
    echo "   cd ${PROJECT_ROOT}"
    echo "   mvn test -Dtest=CucumberTestRunner"
}

function stop_environment() {
    echo "🛑 Stopping ATDD test environment..."
    docker-compose -f "$COMPOSE_FILE" down
    echo "✅ Environment stopped"
}

function restart_environment() {
    echo "🔄 Restarting ATDD test environment..."
    stop_environment
    sleep 2
    start_environment
}

function show_status() {
    echo "📊 ATDD Test Environment Status:"
    echo ""
    
    # Show JAR file being used
    find_and_export_jar 2>/dev/null || echo "⚠️  No JAR file found"
    
    echo ""
    docker-compose -f "$COMPOSE_FILE" ps
}

function show_logs() {
    SERVICE=${1:-""}
    if [ -z "$SERVICE" ]; then
        echo "📜 Showing logs for all services (Ctrl+C to exit)..."
        docker-compose -f "$COMPOSE_FILE" logs -f
    else
        echo "📜 Showing logs for $SERVICE (Ctrl+C to exit)..."
        docker-compose -f "$COMPOSE_FILE" logs -f "$SERVICE"
    fi
}

function clean_environment() {
    echo "🧹 Cleaning up ATDD test environment (including volumes)..."
    docker-compose -f "$COMPOSE_FILE" down -v
    echo "✅ Environment cleaned"
}

function rebuild_and_restart() {
    echo "🔨 Rebuilding Redis provider and restarting environment..."
    
    # Build the provider
    echo "   Building provider..."
    cd "$PROJECT_ROOT"
    mvn clean package -DskipTests
    
    # Restart environment
    cd "$SCRIPT_DIR"
    restart_environment
}

function run_tests() {
    TAG=${1:-""}
    echo "🧪 Running ATDD tests..."
    echo "   Test environment: $MODE"
    cd "$PROJECT_ROOT"
    
    # Find the maven-settings.xml file (go up to keycloak-redis-provider-impl root)
    # PROJECT_ROOT is model/redis, so we need to go up 2 levels to reach keycloak-redis-provider-impl
    MAVEN_SETTINGS="$(cd "$PROJECT_ROOT/../.." && pwd)/maven-settings.xml"
    
    if [ -z "$TAG" ]; then
        mvn test -s "$MAVEN_SETTINGS" -Dtest=CucumberTestRunner -Dtest.environment=$MODE
    else
        echo "   Running tests with tag: $TAG"
        mvn test -s "$MAVEN_SETTINGS" -Dtest=CucumberTestRunner -Dtest.environment=$MODE -Dcucumber.filter.tags="$TAG"
    fi
}

function run_tests_in_docker() {
    echo "🐳 Running ATDD tests inside Docker network (for cluster mode)..."
    
    if [ "$MODE" != "cluster" ]; then
        echo "❌ Docker test runner is only available in cluster mode"
        echo "   Usage: ./atdd-env.sh test cluster"
        exit 1
    fi
    
    echo "   Test environment: docker-cluster"
    
    # Build and run test container
    echo "   Building test runner image..."
    cd "$SCRIPT_DIR"
    docker-compose -f "$COMPOSE_FILE" build test-runner
    
    echo "   Running tests in Docker container..."
    docker-compose -f "$COMPOSE_FILE" --profile test run --rm test-runner
    
    echo ""
    echo "✅ Test results available in target/surefire-reports/"
}

function run_all() {
    echo "🚀 Running complete $MODE test cycle..."
    echo ""
    
    # Start environment
    start_environment
    
    echo ""
    echo "⏳ Waiting 5 seconds before running tests..."
    sleep 5
    echo ""
    
    # Run tests based on mode
    if [ "$MODE" = "cluster" ]; then
        run_tests_in_docker
    else
        run_tests ""
    fi
    
    echo ""
    echo "✅ $MODE test cycle complete!"
}

# Main command handler
case "$1" in
    standalone|cluster)
        # Simplified mode commands - start environment and run tests
        run_all
        ;;
    start)
        start_environment
        ;;
    stop)
        stop_environment
        ;;
    restart)
        restart_environment
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs "$3"
        ;;
    clean)
        clean_environment
        ;;
    rebuild)
        rebuild_and_restart
        ;;
    test)
        # Smart test - use docker for cluster, host for standalone
        if [ "$MODE" = "cluster" ]; then
            run_tests_in_docker
        else
            run_tests "$3"
        fi
        ;;
    help|--help|-h)
        echo "🧪 ATDD Test Environment Manager"
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "SIMPLE USAGE (Recommended):"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""
        echo "  $0 standalone    🚀 Start standalone Redis + run tests"
        echo "  $0 cluster       🚀 Start Redis cluster + run tests in Docker"
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "ADVANCED COMMANDS:"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""
        echo "Environment Control:"
        echo "  $0 start [standalone|cluster]     Start environment only"
        echo "  $0 stop [standalone|cluster]      Stop environment"
        echo "  $0 restart [standalone|cluster]   Restart environment"
        echo "  $0 status [standalone|cluster]    Show service status"
        echo "  $0 clean [standalone|cluster]     Remove all containers & volumes"
        echo ""
        echo "Testing:"
        echo "  $0 test [standalone|cluster] [@tag]   Run tests only"
        echo "  $0 rebuild [standalone|cluster]       Rebuild JAR & restart"
        echo ""
        echo "Monitoring:"
        echo "  $0 logs [standalone|cluster] [service]   View logs"
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "EXAMPLES:"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""
        echo "  # Quick start & test (most common)"
        echo "  $0 standalone              # Test with single Redis"
        echo "  $0 cluster                 # Test with Redis cluster"
        echo ""
        echo "  # Just start environment"
        echo "  $0 start cluster           # Start cluster only"
        echo ""
        echo "  # Run specific test tags"
        echo "  $0 test standalone @critical"
        echo ""
        echo "  # View logs"
        echo "  $0 logs cluster keycloak-test"
        echo ""
        echo "  # Clean up"
        echo "  $0 stop cluster            # Stop services"
        echo "  $0 clean cluster           # Remove everything"
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "INFO:"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""
        echo "  Standalone: Single Redis instance on localhost:16379"
        echo "  Cluster:    6-node Redis cluster (3 masters + 3 replicas)"
        echo ""
        echo "  Service URLs (when running):"
        echo "    • Keycloak:        http://localhost:18080/admin"
        echo "    • Redis Commander: http://localhost:18081"
        echo "    • PostgreSQL:      localhost:15432"
        echo ""
        echo "  Default credentials: admin/admin"
        echo ""
        ;;
    *)
        echo "❌ Unknown command: $1"
        echo ""
        echo "Run '$0 help' for usage information"
        echo ""
        echo "Quick start:"
        echo "  $0 standalone    # Test with standalone Redis"
        echo "  $0 cluster       # Test with Redis cluster"
        exit 1
        ;;
esac
