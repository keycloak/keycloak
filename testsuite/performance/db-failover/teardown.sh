#!/bin/bash

. ./common.sh

echo Stopping Keycloak DB failover environment.

docker-compose -f docker-compose-db-failover.yml down -v

