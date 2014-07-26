Keycloak Web Performance Testsuite
==================================
To run web performance testsuite, you need to:
1) Run Keycloak server
2) Add some users into your Keycloak
3) Run JMeter performance tests

Keycloak server
---------------
In this project you can run:

```shell
mvn exec:java -Pkeycloak-perf-server
````

which will execute embedded Undertow server with:
 * Keycloak server
 * Performance-tools for mass adding of new users 
 * Simple web application for testing performance  
It will also automatically import realm "perf-realm" into Keycloak from file src/main/resources/perfrealm.json 

Note that by default it will use in-memory H2 database, which means that all changes (for example all added users) are discarded after server restart. For performance testing, it's recommended to use some database like PostgreSQL or MySQL

To run server with PostgreSQL you may use command like this (change host,port,dbName and credentials according your DB configuration):
```shell
mvn exec:java -Pkeycloak-perf-server -Dhibernate.connection.url=jdbc:postgresql://localhost:5432/keycloak_perf -Dhibernate.connection.driver_class=org.postgresql.Driver -Dhibernate.connection.username=postgres -Dhibernate.connection.password=postgres -Dhibernate.connection.autocommit=false
````

To run server with MySQL you may use command like this (change host,port,dbName and credentials according your DB configuration):
```shell
mvn exec:java -Pkeycloak-perf-server -Dhibernate.connection.url=jdbc:mysql://localhost/keycloak_perf -Dhibernate.connection.driver_class=com.mysql.jdbc.Driver -Dhibernate.connection.username=portal -Dhibernate.connection.password=portal -Dhibernate.connection.autocommit=false
````

To run server with Mongo you may use command like this (change host,port,dbName and credentials according your DB configuration):
```shell
mvn exec:java -Pkeycloak-perf-server -Dkeycloak.model.provider=mongo -Dkeycloak.model.mongo.db=keycloak-perf
````

To enable cache, you can add additional property:
```shell
-Dkeycloak.model.cache.provider=simple
````

Adding users
-----------------

Performance test is using users with prefix "user" (so users like "user-0", "user-1", ... , "user-123456" etc). So you first need to add some of these users into your database.

For checking users count, you can open this URL: 
```shell
http://localhost:8081/keycloak-tools/perf/perf-realm/get-users-count?prefix=user
````

For adding 10000 new users into your database (will start from last added user, so you don't need to explicitly check how many users to create are needed:
```shell 
http://localhost:8081/keycloak-tools/perf/perf-realm/create-available-users?prefix=user&count=10000&batch=100&async=true&roles=role-0,role-1
````

For update role mappings of all users:
```shell 
http://localhost:8081/keycloak-tools/perf/perf-realm/update-all-users?prefix=user&async=true&roles=role-3,perf-app:approle-3,perf-app:approle-4
````

For deleting all users:
```shell 
http://localhost:8081/keycloak-tools/perf/perf-realm/delete-all-users?prefix=user
````

Seeing progress of job for creating/updating/deleting users
```shell 
http://localhost:8081/keycloak-tools/perf/jobs
````

Note that with default H2 are all data automatically cleared after server restart. So it's recommended to use different DB like PostgreSQL or Mongo.


Execute performance test
------------------------

When server is started and some users are created, you can run performance test. It's possible to run it from Command line with:

```shell 
mvn verify -Pperformance-test
````

By default, test is using Keycloak on localhost:8081 and 50 concurrent clients (threads) and each client doing 50 test iterations. Each iterations is:
- Login user into KC and retrieve code
- Exchange code for accessToken
- Refresh token 2 times
- Logout user

Each client is using separate username, so actually you need at least 50 users created into your DB (users "user-0", "user-1", ... "user-49" . See above)

ATM it's possible to adjust behaviour with properties:
* host --- Keycloak host ("localhost" by default)
* port --- Keycloak port ("8081" by default)
* userPrefix --- prefix of users ("user" by default)
* concurrentUsers --- Number of concurrent clients (50 by default). RampUp time is configured to start 5 new users each second, so with 50 users are all clients started in like 10 seconds.
* iterationsPerUser --- Number of iterations per each client (50 by default). So by default we have 50*50 = 2500 iterations in total per whole test
* refreshTokenRequestsPerIteration --- Number of refresh token iterations (2 by default)

You can change configuration by adding some properties into command line for example to start just with 10 concurrent clients and 10 iterations per client (so just 100 test iterations) you can use:
```shell 
mvn verify -Pperformance-test -DconcurrentUsers=10 -DiterationsPerUser=10 
````
 
After triggering test are results in file target/jmeter/results/aggregatedRequests-durations-<TIMESTAMP>-keycloak_web_perf_test.html

Execute performance test from JMeter GUI
----------------------------------------
You can run
```shell 
mvn jmeter:gui
````

and then open file src/test/jmeter/keycloak_web_perf_test.jmx and trigger test from JMeter GUI. It may be good as you can see the progress of whole test during execution.
