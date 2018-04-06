#!/bin/bash

if [ -z $INFLUX_DATASOURCE_NAME ]; then INFLUX_DATASOURCE_NAME=influx_datasource; fi

if [ -z $INFLUX_HOST ]; then export INFLUX_HOST=influx; fi
if [ -z $INFLUX_DATABASE ]; then export INFLUX_DATABASE=cadvisor; fi


echo Starting Grafana
./run.sh "${@}" &
timeout 10 bash -c "until </dev/tcp/localhost/3000; do sleep 1; done"

echo Checking if datasource '$INFLUX_DATASOURCE_NAME' exists
curl -s 'http://admin:admin@localhost:3000/api/datasources' | grep $INFLUX_DATASOURCE_NAME
DS_EXISTS=$?

if [ $DS_EXISTS -eq 0 ]; then
    echo "Datasource '$INFLUX_DATASOURCE_NAME' already exists in Grafana."
else
    echo "Datasource '$INFLUX_DATASOURCE_NAME' not found in Grafana. Creating..."

    curl -s -H "Content-Type: application/json" \
        -X POST http://admin:admin@localhost:3000/api/datasources \
        -d @- <<EOF
    {
        "name": "${INFLUX_DATASOURCE_NAME}",
        "type": "influxdb",
        "isDefault": false,
        "access": "proxy",
        "url": "http://${INFLUX_HOST}:8086",
        "database": "${INFLUX_DATABASE}"
    }
EOF

fi

echo Restarting Grafana
pkill grafana-server
timeout 10 bash -c "while </dev/tcp/localhost/3000; do sleep 1; done"

exec ./run.sh "${@}"
