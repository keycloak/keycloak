# Grafana configured with InfluxDB

## Customization for InfluxDB

This image adds a custom `entrypoint.sh` on top of the original Grafana image which:
- checks if `$INFLUX_DATASOURCE_NAME` exists in Grafana
- creates the datasource if it doesn't exist
- adds a custom dashboard configured to query for the datasource
- starts Grafana
