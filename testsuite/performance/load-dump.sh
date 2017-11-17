#!/bin/bash
DIRNAME=`dirname "$0"`
GATLING_HOME=$DIRNAME/tests
if [ -z $MARIADB_SERVICE ]; then export MARIADB_SERVICE=mariadb; fi

if [ -z "$DATASET" ]; then
    echo "Please specify DATASET env variable.";
    exit 1;
fi

if [ ! -f $GATLING_HOME/datasets/$DATASET.sql ]; then
    if [ ! -f $GATLING_HOME/datasets/$DATASET.sql.gz ]; then
        echo "Data dump file does not exist: $GATLING_HOME/datasets/$DATASET.sql(.gz)";
        echo "Usage: DATASET=[name] $0"
        exit 1;
    else
        FILE=$GATLING_HOME/datasets/$DATASET.sql.gz
        CMD="zcat $FILE"
    fi
else
    FILE=$GATLING_HOME/datasets/$DATASET.sql
    CMD="cat $FILE"
fi

DB_CONTAINER=performance_${MARIADB_SERVICE}_1
echo DB CONTAINER: $DB_CONTAINER

DB_READY=false
for (( i=1; i<=10; i++ )); do
    echo Waiting for DB to be ready.
    sleep 3
    docker exec -i $DB_CONTAINER mariadb-healthcheck.sh
    if [ $? -eq 0 ]; then DB_READY=true; break; fi
done
if ! $DB_READY; then echo The DB is not ready to receive connections.; exit 1; fi

echo "Importing dump file: $FILE"
echo "\$ $CMD | docker exec -i $DB_CONTAINER /usr/bin/mysql -u root --password=root keycloak"
$CMD | docker exec -i performance_mariadb_1 /usr/bin/mysql -u root --password=root keycloak
