# Keycloak Performance Testsuite

## Requirements:
- Maven 3.1.1+
- Keycloak server distribution installed in the local Maven repository. To do this run `mvn install -Pdistribution` from the root of the Keycloak project.
- Docker 1.13+
- Docker Compose 1.14+
- Bash

## Getting started for the impatient

Here's how to perform a simple tests run:

```
# Clone keycloak repository if you don't have it yet
# git clone https://github.com/keycloak/keycloak.git

# Build Keycloak distribution - needed to build docker image with latest Keycloak server
mvn clean install -DskipTests -Pdistribution

# Now build, provision and run the test
cd testsuite/performance
mvn clean install

# Make sure your Docker daemon is running THEN
mvn verify -Pprovision
mvn verify -Pimport-data -Ddataset=100u -DnumOfWorkers=10 -DhashIterations=100
mvn verify -Ptest -Ddataset=100u -DrunUsers=200 -DrampUpPeriod=10 -DuserThinkTime=0 -DbadLoginAttempts=1 -DrefreshTokenCount=1 -DnumOfIterations=3

```

Now open the generated report in a browser - the link to .html file is displayed at the end of the test.

After the test run you may want to tear down the docker instances for the next run to be able to import data:
```
mvn verify -Pteardown
```

You can perform all phases in a single run:
```
mvn verify -Pprovision,import-data,test,teardown -Ddataset=100u -DnumOfWorkers=10 -DhashIterations=100 -DrunUsers=200 -DrampUpPeriod=10
```
Note: The order in which maven profiles are listed does not determine the order in which profile related plugins are executed. `teardown` profile always executes last.

Keep reading for more information.


## Provisioning

### Provision

Usage: `mvn verify -Pprovision[,cluster] [-D<PARAM>=<VALUE> ...]`. 

- Single node deployment: `mvn verify -Pprovision`
- Cluster deployment: `mvn verify -Pprovision,cluster [-Dkeycloak.scale=N]`. Default `N=1`.

Available parameters are described in [README.provisioning-parameters](README.provisioning-parameters.md).

### Teardown

Usage: `mvn verify -Pteardown[,cluster]`

- Single node deployment: `mvn verify -Pteardown`
- Cluster deployment: `mvn verify -Pteardown,cluster`

Provisioning/teardown is performed via `docker-compose` tool. More details in [README.docker-compose](README.docker-compose.md).


## Testing

### Import Data

Usage: `mvn verify -Pimport-data[,cluster] [-Ddataset=DATASET] [-D<dataset.property>=<value>]`.

Dataset properties are loaded from `datasets/${dataset}.properties` file. Individual properties can be overriden by specifying `-D` params.

Dataset data is first generated as a .json file, and then imported into Keycloak via Admin Client REST API.

#### Examples:
- `mvn verify -Pimport-data` - import default dataset
- `mvn verify -Pimport-data -DusersPerRealm=5` - import default dataset, override the `usersPerRealm` property
- `mvn verify -Pimport-data -Ddataset=100u` - import `100u` dataset
- `mvn verify -Pimport-data -Ddataset=100r/default` - import dataset from `datasets/100r/default.properties`

The data can also be exported from the database, and stored locally as `datasets/${dataset}.sql.gz`
`DATASET=100u ./prepare-dump.sh`

If there is a data dump file available then -Pimport-dump can be used to import the data directly into the database, 
by-passing Keycloak server completely.

Usage: `mvn verify -Pimport-dump [-Ddataset=DATASET]`

#### Example:
- `mvn verify -Pimport-dump -Ddataset=100u` - import `datasets/100u.sql.gz` dump file created using `prepare-dump.sh`.


### Run Tests

Usage: `mvn verify -Ptest[,cluster] [-DrunUsers=N] [-DrampUpPeriod=SECONDS] [-DnumOfIterations=N] [-Ddataset=DATASET] [-D<dataset.property>=<value>]* [-D<test.property>=<value>]* `.

_*Note:* The same dataset properties which were used for data import should be supplied to the `test` phase._

The default test `keycloak.DefaultSimulation` takes the following additional properties:

`[-DuserThinkTime=SECONDS] [-DbadLoginAttempts=N] [-DrefreshTokenCount=N] [-DrefreshTokenPeriod=SECONDS]`


If you want to run a different test you need to specify the test class name using `[-Dgatling.simulationClass=CLASSNAME]`.

For example:

`mvn verify -Ptest -DrunUsers=1 -DnumOfIterations=10 -DuserThinkTime=0 -Ddataset=100u -DrefreshTokenPeriod=10 -Dgatling.simulationClass=keycloak.AdminSimulation`


## Debugging & Profiling

Keycloak docker container exposes JMX management interface on port `9990`.

### JVisualVM

- Start JVisualVM with `jboss-client.jar` on classpath: `./jvisualvm --cp:a $JBOSS_HOME/bin/client/jboss-client.jar`.
- Add a local JMX connection: `service:jmx:remote+http://localhost:9990`.
- Check "Use security credentials" and set `admin:admin`. (The default credentials can be overriden by providing env. variables `DEBUG_USER` and `DEBUG_USER_PASSWORD` to the container.)
- Open the added connection.

_Note: The above applies for the singlenode deployment.
In cluster/crossdc deployments there are multiple KC containers running at the same time so their exposed ports are mapped to random available ports on `0.0.0.0`.
To find the actual mapped ports run command: `docker ps | grep performance_keycloak`._


## Monitoring

There is a docker-based solution for monitoring of CPU, memory and network usage per container. 
(It uses CAdvisor service to export container metrics into InfluxDB time series database, and Grafana web app to query the DB and present results as graphs.)

- To enable run: `mvn verify -Pmonitoring`
- To disable run: `mvn verify -Pmonitoring-off[,delete-monitoring-data]`.
By default the monitoring history is preserved. If you wish to delete it enable the `delete-monitoring-data` profile when turning monitoring off.

To view monitoring dashboard open Grafana UI at: `http://localhost:3000/dashboard/file/resource-usage-combined.json`.



## Examples

### Single-node

- Provision single node of KC + DB, import data, run test, and tear down the provisioned system:

    `mvn verify -Pprovision,import-data,test,teardown -Ddataset=100u -DrunUsers=100`

- Provision single node of KC + DB, import data, no test, no teardown:

    `mvn verify -Pprovision,import-data -Ddataset=100u`

- Run test against provisioned system using 100 concurrent users ramped up over 10 seconds, then tear it down:

    `mvn verify -Ptest,teardown -Ddataset=100u -DrunUsers=100 -DrampUpPeriod=10`

### Cluster

- Provision a 1-node KC cluster + DB, import data, run test against the provisioned system, then tear it down:

    `mvn verify -Pprovision,cluster,import-data,test,teardown -Ddataset=100u -DrunUsers=100`

- Provision a 2-node KC cluster + DB, import data, run test against the provisioned system, then tear it down:

    `mvn verify -Pprovision,cluster,import-data,test,teardown -Dkeycloak.scale=2 -DusersPerRealm=200 -DrunUsers=200`


## Developing tests in IntelliJ IDEA

### Add scala support to IDEA

#### Install the correct Scala SDK

First you need to install Scala SDK. In Scala land it's very important that all libraries used are compatible with specific version of Scala.
Gatling version that we use uses Scala version 2.11.7. In order to avoid conflicts between Scala used by IDEA, and Scala dependencies in pom.xml
it's very important to use that same version of Scala SDK for development.

Thus, it's best to download and install [this SDK version](http://scala-lang.org/download/2.11.7.html)

#### Install IntelliJ's official Scala plugin

Open Preferences in IntelliJ. Type 'plugins' in the search box. In the right pane click on 'Install JetBrains plugin'.
Type 'scala' in the search box, and click Install button of the Scala plugin.

#### Run DefaultSimulation from IntelliJ

In ProjectExplorer find Engine object (you can use ctrl-N / cmd-O). Right click on class name and select Run or Debug like for
JUnit tests.

You'll have to create a test profile, and set 'VM options' with -Dkey=value to override default configuration values in TestConfig class.

Make sure to set 'Use classpath of module' to 'performance-test'. 

When tests are executed via maven, the Engine object is not used. It exists only for running tests in IDE.

If test startup fails due to not being able to find the test classes try reimporting the 'performance' module from pom.xml (right click on 'performance' directory, select 'Maven' at the bottom of context menu, then 'Reimport')
