#! /bin/bash
set -euo pipefail

PGSETUP_INITDB_OPTIONS="-E UTF-8" ./initdb -A md5 -U $PGUSER --pwfile=<(echo "$PGPASSWORD")
echo "host all all 0.0.0.0/0 md5" >> "$PGDATA/pg_hba.conf";

(while ! (sleep 1 && ./createdb $PGDATABASE > /dev/null 2>&1); do echo "Retrying database creation..."; done; echo "Database $PGDATABASE created.") &

exec ./edb-postgres -c port=$PGPORT -c logging_collector=off -c listen_addresses=*