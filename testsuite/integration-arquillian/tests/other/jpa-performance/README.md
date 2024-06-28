# Keycloak JPA Performance Tests

## Test Phases

1. Create individual users
2. Delete realm **Optional** 
3. Re-import realm **Optional** 
4. Delete individual users

Phases 2 and 3 are activated by property `many.users.reimport=true|false`.


## How to run

1. Build the Arquilian Base Testsuite module: `/testsuite/integration-arquillian/base`
2. Run the test from this module using `mvn test` or `mvn clean test`.

Optional parameters:
* `many.users.count` - Number of users to add/delete. Default: *10000*.
* `many.users.batch` - Measurement batch size. Default: *1000*.
* `many.users.reimport` - Switch for phases 2 and 3. Default: *false*.
* `many.users.minTokenValidity` - Minimum validity of admin-client's access token. Default: *10000*. (ms)
* `many.users.read.after.create` - If true, then additional request to read user is send always after the user is created. Default: *false*
* `many.users.create.objects` - If true, then some additional objects will be added to each user (2 attributes, password credential, 2 group mappings, 1 required action) Default: *false*
* `many.users.create.social.links` - If true, then one social (identityProvider) link will be added to each user and then later read. Default: *false*
* `keycloak.connectionsJpa.globalStatsInterval` - Interval in seconds to log Hibernate statistics into log. Default: *-1* (which means statistics are disabled) 


### With MySQL

Start dockerized MySQL:
```
docker run --name mysql-keycloak -e MYSQL_ROOT_PASSWORD=keycloak -e MYSQL_DATABASE=keycloak -e MYSQL_USER=keycloak -e MYSQL_PASSWORD=keycloak -d -p 3306:3306 mysql
```

Additional test parameters:
```
-Pclean-jpa
-Dkeycloak.connectionsJpa.url=jdbc:mysql://localhost/keycloak
-Dkeycloak.connectionsJpa.driver=com.mysql.jdbc.Driver
-Dkeycloak.connectionsJpa.user=keycloak
-Dkeycloak.connectionsJpa.password=keycloak
```

### With PostgreSQL

Start dockerized PostgreSQL:
```
docker run --name postgres-keycloak -e POSTGRES_PASSWORD=keycloak -d -p 5432:5432 postgres
```

Additional test parameters:
```
-Pclean-jpa
-Dkeycloak.connectionsJpa.url=jdbc:postgresql://localhost/postgres
-Dkeycloak.connectionsJpa.driver=org.postgresql.Driver
-Dkeycloak.connectionsJpa.user=postgres
-Dkeycloak.connectionsJpa.password=keycloak
```

## Reports

Test creates reports in `target/stats`.
