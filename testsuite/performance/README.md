Configuration of performance test
=================================

- src/test/jmeter/system.properties -- System properties including configuration of providers. Allow to specify:
-- Number of worker threads and loops to be used by JMeter performance test
-- which model to use
-- which test to run
-- configuration of individual tests. Properties for each test documented in the file

Running performance tests
=========================
cd KEYCLOAK_HOME/testsuite/performance
mvn clean verify -DskipTests=true -Pperformance-tests

Results:
- Log is in: testsuite/performance/target/jmeter/logs/keycloak_perf_test.jmx.log
- More charts and reports are inside: testsuite/performance/target/jmeter/results/


Example for running test
========================
1) Run:
mvn clean verify -DskipTests=true -Pperformance-tests
with OOTB configuration (Assumption is mongo running on 27017 as it's using mongo by default). This will create 10 new realms.

2) Then change src/test/jmeter/system.properties to have
 "Tkeycloak.jmeter.numThreads" to 10 and
 "keycloak.jmeter.loops" to 100
 "keycloak.perf.workerClass" to "org.keycloak.testsuite.performance.CreateUsersWorker"

Then run again:
mvn clean verify -DskipTests=true -Pperformance-tests
This will create 1000 new users (10 worker threads and each worker doing 100 iterations. Each worker is creating users in separate realm. So 100 users like "user1", "user2", ... "user100" in each realm)

3) Then change src/test/jmeter/system.properties to have
 "keycloak.perf.workerClass" to "org.keycloak.testsuite.performance.ReadUsersWorker"

Then run again:
mvn clean verify -DskipTests=true -Pperformance-tests
 This will read all 1000 previously created users and each user is read 5 times. There are 1000 iterations in total and each iteration is doing 5 read users.


TODO: Easier configuration without need to edit config files, more user friendly, easier to configure and run test

