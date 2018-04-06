#!/bin/bash

if [ -z $INFLUX_HOST ]; then export INFLUX_HOST=influx; fi
if [ -z $INFLUX_DATABASE ]; then export INFLUX_DATABASE=cadvisor; fi

# Check if DB exists
curl -s -G "http://$INFLUX_HOST:8086/query?pretty=true" --data-urlencode "q=SHOW DATABASES" | grep $INFLUX_DATABASE
DB_EXISTS=$?

if [ $DB_EXISTS -eq 0 ]; then
    echo "Database '$INFLUX_DATABASE' already exists on InfluxDB server '$INFLUX_HOST:8086'"
else
    echo "Creating database '$INFLUX_DATABASE' on InfluxDB server '$INFLUX_HOST:8086'"
    curl -i -XPOST http://$INFLUX_HOST:8086/query --data-urlencode "q=CREATE DATABASE $INFLUX_DATABASE"
fi

/usr/bin/cadvisor -logtostderr -docker_only -storage_driver=influxdb -storage_driver_db=cadvisor -storage_driver_host=$INFLUX_HOST:8086 $@

