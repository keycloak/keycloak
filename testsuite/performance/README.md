# Keycloak Performance Testsuite

## Requirements:
- Bash 2.05+
- Maven 3.1.1+
- Keycloak server distribution installed in the local Maven repository. To do this run `mvn install -Pdistribution` from the root of the Keycloak project.

### Docker Compose Provisioner
- Docker 1.13+
- Docker Compose 1.14+

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
mvn verify -Pgenerate-data -Ddataset=100u -DnumOfWorkers=10 -DhashIterations=100
mvn verify -Ptest -Ddataset=100u -DrunUsers=200 -DrampUpPeriod=10 -DuserThinkTime=0 -DbadLoginAttempts=1 -DrefreshTokenCount=1 -DsteadyLoadPeriod=10

```

Now open the generated report in a browser - the link to .html file is displayed at the end of the test.

After the test run you may want to tear down the docker instances for the next run to be able to import data:
```
mvn verify -Pteardown
```

You can perform all phases in a single run:
```
mvn verify -Pprovision,generate-data,test,teardown -Ddataset=100u -DnumOfWorkers=10 -DhashIterations=100 -DrunUsers=200 -DrampUpPeriod=10
```
Note: The order in which maven profiles are listed does not determine the order in which profile related plugins are executed. `teardown` profile always executes last.

Keep reading for more information.


## Provisioning

### Available provisioners:

- `docker-compose` **Default.** See [`README.docker-compose.md`](README.docker-compose.md) for more details.

### Provision

Usage: `mvn verify -Pprovision [-Dprovisioner=<PROVISIONER>] [-D<PARAMETER>=<VALUE>] …`. 

#### Deployment Types

- Single node: `mvn verify -Pprovision`
- Cluster: `mvn verify -Pprovision,cluster [-Dkeycloak.scale=N] [-Dkeycloak.cpusets="cpuset1 cpuset2 … cpusetM"]`. `N ∈ {1 .. M}`.
- Cross-DC: `mvn verify -Pprovision,crossdc [-Dkeycloak.dc1.scale=K] [-Dkeycloak.dc2.scale=L] [-Dkeycloak.dc1.cpusets=…] [-Dkeycloak.dc2.cpusets=…]`

All available parameters are described in [`README.provisioning-parameters.md`](README.provisioning-parameters.md).

#### Provisioned System

The `provision` operation will produce a `provisioned-system.properties` inside the `tests/target` directory 
with information about the provisioned system such as the type of deployment and URLs of Keycloak servers and load balancers.
This information is then used by operations `generate-data`, `import-dump`, `test`, `teardown`.

Provisioning can be run multiple times with different parameters. The system will be updated/reprovisioned based on the new parameters.
However when switching between different deployment types (e.g. from `singlenode` to `cluster`) it is always necessary 
to tear down the currently running system.

**Note:** When switching deployment type from `singlenode` or `cluster` to `crossdc` (or the other way around) 
it is necessary to update the generated Keycloak server configuration (inside `keycloak/target` directory) by 
adding a `clean` goal to the provisioning command like so: `mvn clean verify -Pprovision …`. It is *not* necessary to update this configuration 
when switching between `singlenode` and `cluster` deployments.

### Teardown

Usage: `mvn verify -Pteardown [-Dprovisioner=<PROVISIONER>]`

**Note:** Unless the provisioned system has been properly torn down the maven build will not allow a cleanup of the `tests/target` directory
because it contains the `provisioned-system.properties` with information about the still-running system.


## Testing

### Generate Test Data

Usage: `mvn verify -Pgenerate-data [-Ddataset=DATASET] [-D<dataset.property>=<value>]`.

Dataset properties are loaded from `datasets/${dataset}.properties` file. Individual properties can be overriden by specifying `-D` params.

Dataset data is first generated as a .json file, and then imported into Keycloak via Admin Client REST API.

#### Dataset Properties

| Property | Description | Value in the Default Dataset |
| --- | --- | --- | 
| `numOfRealms` | Number of realms to be created. | `1`  |
| `usersPerRealm` | Number of users per realm. | `100`  |
| `clientsPerRealm` | Number of clients per realm. | `2`  |
| `realmRoles` | Number of realm-roles per realm. | `2`  |
| `realmRolesPerUser` | Number of realm-roles assigned to a created user. Has to be less than or equal to `realmRoles`. | `2`  |
| `clientRolesPerUser` | Number of client-roles assigned to a created user. Has to be less than or equal to `clientsPerRealm * clientRolesPerClient`. | `2`  |
| `clientRolesPerClient` | Number of client-roles per created client. | `2`  |
| `hashIterations` | Number of password hashing iterations. | `27500`  |


#### Examples:
- `mvn verify -Pgenerate-data` - generate default dataset
- `mvn verify -Pgenerate-data -DusersPerRealm=5` - generate default dataset, override the `usersPerRealm` property
- `mvn verify -Pgenerate-data -Ddataset=100u` - generate `100u` dataset
- `mvn verify -Pgenerate-data -Ddataset=100r/default` - generate dataset based on `datasets/100r/default.properties`

#### Export / Import Database Dump

To speed up dataset initialization part, it is possible to pass `-Dexport-dump` option to have the generated dataset
exported right after it has been generated. Then, if there is a data dump file available then `-Pimport-dump` 
can be used to import the data directly into the database, bypassing Keycloak server completely.

**Usage:** `mvn verify -Pimport-dump [-Ddataset=DATASET]`

**For example:**
- `mvn verify -Pgenerate-data -Ddataset=100u -Dexport-dump` will generate data based on `datasets/100u.properties` and export a database dump to a file: `datasets/100u.sql.gz`.
- `mvn verify -Pimport-dump -Ddataset=100u` will import the database dump from a file: `datasets/100u.sql.gz`, and reboot the server(s)


### Run Tests

Usage: `mvn verify -Ptest[,cluster] [-DtestParameter=value]`.

#### Common Parameters

| Parameter | Description | Default Value |
| --- | --- | --- | 
| `gatling.simulationClass` | Classname of the simulation to be run. | `keycloak.BasicOIDCSimulation`  |
| `dataset` | Name of the dataset to use. (Individual dataset properties can be overridden with `-Ddataset.property=value`.) | `default` |
| `runUsers` | Number of users for the simulation run. | `1` |
| `rampUpPeriod` | Period during which the users will be ramped up. (seconds) | `0` |
| `steadyLoadPeriod` | A period of steady load. (seconds) | `30` |
| `rampDownASAP` | When `true` the test will be checking for ramp-down condition after each *scenario step*. When `false` the check will be done only at the end of a *scenario iteration*. | `false` |
| `pace` | A dynamic pause after each *scenario iteration*. For example if the pace is 30s and one scenario iteration takes only 20s, the simulation will wait additional 10s before continuing to the next iteration. | `0` |
| `userThinkTime` | Pause between individual scenario steps. | `5` |
| `refreshTokenPeriod`| Period after which token should be refreshed. | `10` |

#### Addtional Parameters of `keycloak.BasicOIDCSimulation`

| Parameter | Description | Default Value |
| --- | --- | --- | 
| `badLoginAttempts` | | `0`  |
| `refreshTokenCount` | | `0` |


Example:

`mvn verify -Ptest -Dgatling.simulationClass=keycloak.AdminConsoleSimulation -Ddataset=100u -DrunUsers=1 -DsteadyLoadPeriod=30 -DuserThinkTime=0 -DrefreshTokenPeriod=15`


## Monitoring

### JMX

To enable access to JMX on the WildFly-backed services set properties `management.user` and `management.user.password` during the provisioning phase.

#### JVisualVM

- Set `JBOSS_HOME` variable to point to a valid WildFly 10+ installation.
- Start JVisualVM with `jboss-client.jar` on classpath: `./jvisualvm --cp:a $JBOSS_HOME/bin/client/jboss-client.jar`.
- Add a local JMX connection: `service:jmx:remote+http://localhost:9990`. <sup>**[*]**</sup>
- Check "Use security credentials" and set `admin:admin`. (The default credentials can be overriden by providing env. variables `DEBUG_USER` and `DEBUG_USER_PASSWORD` to the container.)
- Open the added connection.

**[*]** For `singlenode` this points to the JMX console of the Keycloak server.
To get the connection URLs for `cluster` or `crossdc` deployments see the JMX section in the generated `provisioned-system.properties` file.
- Property `keycloak.frontend.servers.jmx` contains JMX URLs of the Load Balancers.
- Property `keycloak.backend.servers.jmx` contains JMX URLs of the clustered Keycloak servers.
- Property `infinispan.servers.jmx` contains JMX URLs of the Infinispan servers, in Cross-DC deployment.

### Docker Monitoring

There is a docker-based solution for monitoring CPU, memory and network usage per container. 
It uses CAdvisor service to export container metrics into InfluxDB time series database, and Grafana web app to query the DB and present results as graphs.

- To enable run: `mvn verify -Pmonitoring`
- To disable run: `mvn verify -Pmonitoring-off[,delete-monitoring-data]`.
By default the monitoring history is preserved. If you wish to delete it enable the `delete-monitoring-data` profile when turning monitoring off.

To view monitoring dashboard open Grafana UI at: `http://localhost:3000/dashboard/file/resource-usage-combined.json`.

### Sysstat metrics

To enable recording of sysstat metrics use `-Psar`.
This will run the `sar` command during the test and process its binary output to produce textual and CSV files with CPU utilisation stats.
To also enable creation of PNG charts use `-Psar,gnuplot`. For this to work Gnuplot needs to be installed on the machine.
To compress the binary output with bzip add `-Dbzip=true` to the commandline.

Results will be stored in folder: `tests/target/sar`.

## Examples

### Single-node

- Provision single node of KC + DB, generate data, run test, and tear down the provisioned system:

    `mvn verify -Pprovision,generate-data,test,teardown -Ddataset=100u -DrunUsers=100`

- Provision single node of KC + DB, generate data, no test, no teardown:

    `mvn verify -Pprovision,generate-data -Ddataset=100u`

- Run test against provisioned system using 100 concurrent users ramped up over 10 seconds, then tear it down:

    `mvn verify -Ptest,teardown -Ddataset=100u -DrunUsers=100 -DrampUpPeriod=10`

### Cluster

- Provision a 1-node KC cluster + DB, generate data, run test against the provisioned system, then tear it down:

    `mvn verify -Pprovision,cluster,generate-data,test,teardown -Ddataset=100u -DrunUsers=100`

- Provision a 2-node KC cluster + DB, generate data, run test against the provisioned system, then tear it down:

    `mvn verify -Pprovision,cluster,generate-data,test,teardown -Dkeycloak.scale=2 -DusersPerRealm=200 -DrunUsers=200`


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

#### Run BasicOIDCSimulation from IntelliJ

Make sure that `performance` maven profile is enabled for IDEA to treat `performance` directory as a project module. 

You may also need to rebuild the module in IDEA for scala objects to become available.

Then find Engine object In ProjectExplorer (you can use ctrl-N / cmd-O). Right click on class name and select Run or Debug as if it was
a JUnit tests.

You'll have to edit a test configuration, and set 'VM options' to a list of -Dkey=value pairs to override default configuration values in TestConfig class.

Make sure to set 'Use classpath of module' to 'performance-test'. 

When tests are executed via maven, the Engine object is not used. It exists only for running tests in IDE.

If test startup fails due to not being able to find the test classes try reimporting the 'performance' module from pom.xml (right click on 'performance' directory, select 'Maven' at the bottom of context menu, then 'Reimport')

If you want to run a different simulation - not DefaultSimulation - you can edit Engine object source, or create another Engine object for a different simulation.

## Troubleshoot

### Verbose logging
You can find `logback-test.xml` file in `tests/src/test/resources` directory. This files contains logging information in log4j xml format.
Root logger is by default set to WARN, but if you want to increase verbosity you can change it to DEBUG or INFO.

