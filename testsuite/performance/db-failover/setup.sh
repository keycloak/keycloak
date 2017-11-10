#!/bin/bash

. ./common.sh

if [ -z "$DB_BOOTSTRAP_TIMEOUT" ]; then DB_BOOTSTRAP_TIMEOUT=10; fi
if [ -z "$DB_JOIN_TIMEOUT" ]; then DB_JOIN_TIMEOUT=5; fi

echo Setting up Keycloak DB failover environment:

echo Starting DB bootstrap instance.
docker-compose -f docker-compose-db-failover.yml up -d --build mariadb_bootstrap
echo Waiting $DB_BOOTSTRAP_TIMEOUT s for the DB to initialize.
sleep $DB_BOOTSTRAP_TIMEOUT

MARIADB_HOSTS=""
for (( i=1; i<=$NODES; i++ )); do

    MARIADB_HOSTS=$MARIADB_HOSTS,mariadb_$i:3306

    echo Starting DB node $i.
    docker-compose -f docker-compose-db-failover.yml up -d mariadb_$i
    echo Waiting $DB_JOIN_TIMEOUT s for the DB node to join
    echo
    sleep $DB_JOIN_TIMEOUT

done

echo Turning off the DB bootstrap instance.
docker-compose -f docker-compose-db-failover.yml stop mariadb_bootstrap

export MARIADB_HOSTS=${MARIADB_HOSTS/,/}
echo MARIADB_HOSTS=$MARIADB_HOSTS

if $START_KEYCLOAK; then
    echo Starting Keycloak server.
    docker-compose -f docker-compose-db-failover.yml up -d --build keycloak
    ./healthcheck.sh
fi
