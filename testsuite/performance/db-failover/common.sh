function killNode {
    echo Killing mariadb_${1}
    docker-compose -f docker-compose-db-failover.yml kill mariadb_${1}
}

function reconnectNode {
    N=$1
    NR=$(( N + 1 )); if [ "$NR" -gt "$NODES" ]; then NR=1; fi
    export MARIADB_RUNNING_HOST=mariadb_${NR}
    echo Attempting failback of mariadb_${N}, connecting to running cluster member mariadb_${NR}
    docker-compose -f docker-compose-db-failover.yml up -d mariadb_${N} 
}

if [ -z $NODES ]; then export NODES=2; fi
if [ -z $MARIADB_OPTIONS ]; then export MARIADB_OPTIONS=""; fi
if [ -z $START_KEYCLOAK ]; then export START_KEYCLOAK=false; fi

cd ..
