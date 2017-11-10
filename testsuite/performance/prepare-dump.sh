#!/bin/bash
DIRNAME=`dirname "$0"`
GATLING_HOME=$DIRNAME/tests

if [ -z "$DATASET" ]; then
    echo "This script requires DATASET env variable to be set"
    exit 1
fi

echo "Exporting dump file"
docker exec performance_mariadb_1 /usr/bin/mysqldump -u root --password=root keycloak > $DATASET.sql
if [ $? -ne 0 ]; then
    echo "Failed!"
    exit $?
fi

gzip $DATASET.sql
mv $DATASET.sql.gz $GATLING_HOME/datasets/