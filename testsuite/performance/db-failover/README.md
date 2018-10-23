# DB Failover Testing Utilities

A set of scripts for testing DB failover scenarios.

The scripts expect to be run with relative execution prefix `./` from within the `db-failover` directory.

Provisioned services are defined in `../docker-compose-db-failover.yml` template.


## Set the size of DB cluster

Default size is 2 nodes. For a 3-node cluster run: `export NODES=3` before executing any of the scripts.

For more than 3 nodes more service definitions need to be added to the docker-compose template.

## Set up the environment

Run `./setup.sh`

This script will:
1. Start a bootstrap DB instance of MariaDB cluster
2. Start additional DB instances connected to the bootstrapped cluster
3. Stop the bootstrap DB instance
4. Optionally start Keycloak server

Parameterized by environment variables:
- `MARIADB_HA_MODE` See: [MariaDB HA parameters](https://mariadb.com/kb/en/library/failover-and-high-availability-with-mariadb-connector-j/#failover-high-availability-parameters)
   Defaults to `replication:`.
- `MARIADB_OPTIONS` See: [MariaDB HA options](https://mariadb.com/kb/en/library/failover-and-high-availability-with-mariadb-connector-j/#failover-high-availability-options).
   Use format: `?option1=value1[&option2=value2]...`. Default is an empty string.
- `START_KEYCLOAK` Default is `false`. Use `export START_KEYCLOAK=true` to enable.

More options relevant to MariaDB clustering can be found in `../db/mariadb/wsrep.cnf`.


## Test the failover

### Manual failover

To induce a failure of specific DB node run: `./kill-node.sh X` where `X ∈ {1..3}`

To reconnect the node back run: `./reconnect-node.sh X`


### Automated failover loop

Run `./loop.sh`

This script will run an infinite loop of failover/failback of DB nodes, switching to the next node in each loop.

Parameterized by environment variables:
- `TIME_BETWEEN_FAILURES` Default is `60` (seconds).
- `FAILURE_DURATION` Default is `60` (seconds).

To exit the script press `Ctrl+C`.


### Check number of table rows across the cluster

Run: `./check-rows.sh`


## Tear down the environment

Run `./teardown.sh`

This will stop all services and delete the database.
