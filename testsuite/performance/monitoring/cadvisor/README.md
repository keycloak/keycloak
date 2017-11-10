# CAdvisor configured for InfluxDB

Google CAdvisor tool configured to export data into InfluxDB service.

## Customization for InfluxDB

This image adds a custom `entrypoint.sh` on top of the original CAdvisor image which:
- checks if `$INFLUX_DATABASE` exists on `$INFLUX_HOST`
- creates the DB if it doesn't exist
- starts `cadvisor` with storage driver set for the Influx DB
