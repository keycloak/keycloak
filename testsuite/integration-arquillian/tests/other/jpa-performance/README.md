# Keycloak JPA Performance Tests

## How to run

1. Build the Arquilian Base Testsuite module: `/testsuite/integration-arquillian/base`
2. Run the test from this module using `mvn test` or `mvn clean test`.

Optional parameters:
```
-Dmany.users.count=10000
-Dmany.users.batch=1000
```

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
