#!/bin/bash

. ./common.sh

CONCAT_SQL=$(< db-failover/concat.sql)
CONCAT_SQL_COMMAND='mysql -N -B -u keycloak --password=keycloak -e "$CONCAT_SQL" keycloak'
ROWS_SQL=$(eval docker-compose -f docker-compose-db-failover.yml exec mariadb_1 $CONCAT_SQL_COMMAND | tr -dc '[:print:]')
ROWS_SQL=${ROWS_SQL%UNION }
ROWS_SQL_COMMAND='mysql -u keycloak --password=keycloak -e "$ROWS_SQL" keycloak'

for (( i=1; i <= $NODES; i++)); do
    ROWS[i]=$(eval docker-compose -f docker-compose-db-failover.yml exec mariadb_$i $ROWS_SQL_COMMAND)
done

DIFF=0
for (( i=2; i <= $NODES; i++)); do
    echo Node 1 vs Node $(( i )):
    diff -y --suppress-common-lines <(echo "${ROWS[1]}") <(echo "${ROWS[i]}")
    if [ $? -eq 0 ]; then echo No difference.; else DIFF=1; fi
done

exit $DIFF
