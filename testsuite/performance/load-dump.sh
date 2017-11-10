#!/bin/bash
DIRNAME=`dirname "$0"`
GATLING_HOME=$DIRNAME/tests

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

echo "Importing dump file: $FILE"
echo "\$ $CMD | docker exec -i performance_mariadb_1 /usr/bin/mysql -u root --password=root keycloak"
$CMD | docker exec -i performance_mariadb_1 /usr/bin/mysql -u root --password=root keycloak
