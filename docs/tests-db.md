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

The project provides specific profiles to run database tests using containers. Below is a just a sample of implemented profiles. In order to get a full list, please invoke (`mvn help:all-profiles -pl testsuite/integration-arquillian | grep -- db- | grep -v allocator`):

* `db-mysql`
* `db-postgres`

As an example, to run tests using a MySQL docker container on Undertow auth-server:

    mvn -f testsuite/integration-arquillian clean verify -Pdb-mysql

If you want to run tests using a pre-configured Keycloak distribution (instead of Undertow):

    mvn -f testsuite/integration-arquillian clean verify -Pdb-mysql,jpa,auth-server-wildfly

Note that you must always activate the `jpa` profile when using auth-server-wildfly.

If the mvn command fails for any reason, it may also fail to remove the container which
must be then removed manually.

For Oracle databases, neither JDBC driver nor the image are publicly available
due to licensing restrictions and require preparation of the environment. You
first need to download the JDBC driver and install it to your local maven repo
(feel free to specify GAV and file according to the one you would download):

    mvn install:install-file -DgroupId=com.oracle -DartifactId=ojdbc7 -Dversion=12.1.0 -Dpackaging=jar -Dfile=ojdbc7.jar -DgeneratePom=true

Then build the Docker image per instructions at
https://github.com/oracle/docker-images/tree/master/OracleDatabase. The last
step is running which might require updating the `jdbc.mvn.groupId`,
`jdbc.mvn.artifactId`, and `jdbc.mvn.version` according to the parameters you
used in the command above, and `docker.database.image` if you used a different
name or tag for the image.

Note that Docker containers may occupy some space even after termination, and
especially with databases that might be easily a gigabyte. It is thus
advisable to run `docker system prune` occasionally to reclaim that space.


Using DB Allocator Service
-------

The testsuite can use the DB Allocator Service to allocate and release desired database automatically.
Since some of the database properties (such as JDBC URL, Username or Password) need to be used when building the Auth Server,
the allocation and deallocation need to happen when building the `integration-arquillian` project (instead of `tests/base` as
it happens in other cases).

In order to use the DB Allocator Service, you must use the `jpa` profile with one of the `db-allocator-*`. Here's a full example to
run JPA with Auth Server Wildfly and MSSQL 2016:

```
mvn -f testsuite/integration-arquillian/pom.xml clean verify \
    -Pjpa,auth-server-wildfly,db-allocator-db-mssql2016 \
    -Ddballocator.uri=<<db-allocator-servlet-url>> \
    -Ddballocator.user=<<db-allocator-user>> \
    -Dmaven.test.failure.ignore=true
```

Using `-Dmaven.test.failure.ignore=true` is not strictly required but highly recommended. After running the tests,
the DB Allocator Plugin should release the allocated database.

**NOTE**: If you killed the maven surefire test preliminary (for example with CTRL-C or `kill -9` command), it will be
good to manually release the allocated database. Please check `dballocator.uri` and add `?operation=report` to the end of the URL.
Find your DB in the GUI and release it manually.

Below is a just a sample of implemented profiles. In order to get a full list, please invoke (`mvn help:all-profiles -pl testsuite/integration-arquillian | grep -- db-allocator-db-`):

* `db-allocator-db-postgres` - for testing with Postgres 9.6.x
* `db-allocator-db-mysql` - for testing with MySQL 5.7