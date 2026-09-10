Test with various databases
===========================

MySQL
-----

The simplest way to test with MySQL is to use the official [MySQL docker image](https://registry.hub.docker.com/_/mysql/).

Start MySQL:

    docker run --name mysql -e MYSQL_DATABASE=keycloak -e MYSQL_USER=keycloak -e MYSQL_PASSWORD=keycloak -e MYSQL_ROOT_PASSWORD=keycloak -d mysql
   
Run tests:

    mvn install -Dkeycloak.connectionsJpa.url=jdbc:mysql://`docker inspect --format '{{ .NetworkSettings.IPAddress }}' mysql`/keycloak -Dkeycloak.connectionsJpa.driver=com.mysql.jdbc.Driver -Dkeycloak.connectionsJpa.user=keycloak -Dkeycloak.connectionsJpa.password=keycloak    
    
Stop MySQl:

    docker rm -f mysql
    
    
PostgreSQL
----------

The simplest way to test with PostgreSQL is to use the official [PostgreSQL docker image](https://registry.hub.docker.com/_/postgres/).

Start PostgreSQL:

    docker run --name postgres -e POSTGRES_DATABASE=keycloak -e POSTGRES_USER=keycloak -e POSTGRES_PASSWORD=keycloak -e POSTGRES_ROOT_PASSWORD=keycloak -d postgres
   
Run tests:

    mvn install -Dkeycloak.connectionsJpa.url=jdbc:postgresql://`docker inspect --format '{{ .NetworkSettings.IPAddress }}' postgres`:5432/keycloak -Dkeycloak.connectionsJpa.driver=org.postgresql.Driver -Dkeycloak.connectionsJpa.user=keycloak -Dkeycloak.connectionsJpa.password=keycloak    
    
Stop PostgreSQL:

    docker rm -f postgres
    
MariaDB
-------

The simplest way to test with MariaDB is to use the official [MariaDB docker image](https://registry.hub.docker.com/_/mariadb/).

Start MariaDB:

    docker run --name mariadb -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=keycloak -e MYSQL_USER=keycloak -e MYSQL_PASSWORD=keycloak -d mariadb:10.1
   
Run tests:

    mvn install -Dkeycloak.connectionsJpa.url=jdbc:mariadb://`docker inspect --format '{{ .NetworkSettings.IPAddress }}' mariadb`/keycloak -Dkeycloak.connectionsJpa.driver=org.mariadb.jdbc.Driver -Dkeycloak.connectionsJpa.user=keycloak -Dkeycloak.connectionsJpa.password=keycloak    
    
Stop MySQl:

    docker rm -f mariadb

TiDB
-----

The simplest way to test with TiDB is to use the official [TiDB docker image](https://hub.docker.com/r/pingcap/tidb).

Start TiDB:

    docker run --name tidb -p 4000:4000 -d pingcap/tidb:v8.5.2

Run tests:

    mvn install -Dkeycloak.connectionsJpa.url=jdbc:mysql://`docker inspect --format '{{ .NetworkSettings.IPAddress }}' tidb`:4000/test -Dkeycloak.connectionsJpa.driver=com.mysql.jdbc.Driver -Dkeycloak.connectionsJpa.user=root -Dkeycloak.connectionsJpa.password=    

Stop TiDB:

    docker rm -f tidb


Enabling SSL
============

Generate certificate
-------

To enable TLS connection, a private key and certificate to be provided to the database.
Let's create a directory named `certs` to store the files we need.

```bash
mkdir certs ; cd certs
```

```bash
openssl req -x509 -newkey rsa:4096 -keyout database-key.pem -out database-cert.pem -sha256 -days 3650 -nodes -subj "/CN=localhost
```

Private key permissions
-------

The primary key must belong to the user running in the container and only that user should be able to access.
PostgreSQL, MariaDB, and MySQL both use user with id 999 (at the time of writing).

```bash
chmod 0600 database-key.pem
chown 999:999 database-key.pem
```

Starting the database container
-------

Mount the `certs` directory in the container and configure the database as shown below.
The file `database-cert.pem` can be added to Keycloak truststore to perform the hostname verification.
By default, the JDBC drivers do not perform the hostname verification.

**PostgreSQL**

```bash
docker run -d --name postgres --network host --volume '${PWD}:/mnt/certs:ro' -e POSTGRES_USER=keycloak -e POSTGRES_PASSWORD=keycloak -e POSTGRES_DB=keycloak postgres:17 postgres -c ssl=on -c ssl_cert_file=/mnt/certs/database-cert.pem -c ssl_key_file=/mnt/certs/database-key.pem
```

**MariaDB**

```bash
docker run -d --name maridb --network host --volume '${PWD}:/mnt/certs:ro' -e MARIADB_ROOT_PASSWORD=keycloak -e MARIADB_USER=keycloak -e MARIADB_PASSWORD=keycloak -e MARIADB_DATABASE=keycloak mariadb:11 --ssl-cert=/mnt/certs/database-cert.pem --ssl-key=/mnt/certs/database-key.pem --require-secure-transport
```

The option `--require-secure-transport` ensures only TLS connections are accepted by the server.

**MySQL**

```bash
docker run -d --name mysql --network host --volume '${PWD}:/mnt/certs:ro' -e MYSQL_ROOT_PASSWORD=keycloak -e MYSQL_USER=keycloak -e MYSQL_PASSWORD=keycloak -e MYSQL_DATABASE=keycloak mysql:9 --ssl-cert=/mnt/certs/database-cert.pem --ssl-key=/mnt/certs/database-key.pem --require-secure-transport
```

The option `--require-secure-transport` ensures only TLS connections are accepted by the server.

Using built-in profiles to run database tests using docker containers
============

The project provides specific profiles to run database tests using containers. Below is a just a sample of implemented profiles. In order to get a full list, please invoke (`mvn help:all-profiles -pl testsuite/integration-arquillian | grep -- db-`):

* `db-mysql`
* `db-postgres`

As an example, to run tests using a MySQL docker container on Undertow auth-server:

    mvn -f testsuite/integration-arquillian clean verify -Pdb-mysql

If you want to run tests using a pre-configured Keycloak distribution (instead of Undertow):

    mvn -f testsuite/integration-arquillian clean verify -Pdb-mysql,jpa,auth-server-quarkus

Note that you must always activate the `jpa` profile when using auth-server-quarkus.

If the mvn command fails for any reason, it may also fail to remove the container which
must be then removed manually.

For Oracle databases, the images are not publicly available due to licensing restrictions. 

Build the Docker image per instructions at
https://github.com/oracle/docker-images/tree/main/OracleDatabase.
Update the property `docker.database.image` if you used a different
name or tag for the image.

Note that Docker containers may occupy some space even after termination, and
especially with databases that might be easily a gigabyte. It is thus
advisable to run `docker system prune` occasionally to reclaim that space.
