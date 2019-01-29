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

Using built-in profiles to run database tests using docker containers
-------

The project provides specific profiles to run database tests using containers. The supported databases and their respective profiles are:

* `db-mysql`
* `db-postgres`
* `db-mariadb`

As an example, to run tests using a MySQL docker container:

    mvn -f testsuite/integration-arquillian clean verify -Pdb-mysql,jpa

If you want to run tests using a pre-configured Keycloak distribution (instead of Undertow):

    mvn -f testsuite/integration-arquillian clean verify -Pdb-mysql,jpa,auth-server-wildfly
    
Note that you must always activate the `jpa` profile.
    
