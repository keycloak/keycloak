#!/bin/bash

echo "docker-entrypoint-wsrep.sh: $1"

if [ "$1" == "--wsrep-new-cluster" ]; then

    echo "docker-entrypoint-wsrep.sh: Cluster 'bootstrap' node."

else

    echo "docker-entrypoint-wsrep.sh: Cluster 'member' node."

    if [ ! -d "$DATADIR/mysql" ]; then
        echo "docker-entrypoint-wsrep.sh: Creating empty datadir to be populated from the cluster 'seed' node."
        mkdir -p "$DATADIR/mysql"
	chown -R mysql:mysql "$DATADIR"
    fi

fi

echo "docker-entrypoint-wsrep.sh: Delegating to original mariadb docker-entrypoint.sh"
docker-entrypoint.sh "$@";
