# Keycloak Performance Testsuite

## Requirements:
- Bash 2.05+
- Maven 3.5.4+
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
mvn verify -Pgenerate-data -Ddataset=1r_10c_100u -DnumOfWorkers=10
mvn verify -Ptest -Ddataset=1r_10c_100u -DusersPerSec=2 -DrampUpPeriod=10 -DuserThinkTime=0 -DbadLoginAttempts=1 -DrefreshTokenCount=1 -DmeasurementPeriod=60 -DfilterResults=true
```

Now open the generated report in a browser - the link to .html file is displayed at the end of the test.

After the test run you may want to tear down the docker instances for the next run to be able to import data:
```
mvn verify -Pteardown
```

You can perform all phases in a single run:
```
mvn verify -Pprovision,generate-data,test,teardown -Ddataset=1r_10c_100u -DnumOfWorkers=10 -DusersPerSec=4 -DrampUpPeriod=10
```
Note: The order in which maven profiles are listed does not determine the order in which profile related plugins are executed. `teardown` profile always executes last.

Keep reading for more information.


## Provisioning

### Provision

#### Provisioners

Depending on the target environment different provisioners may be used.
Provisioner can be selected via property `-Dprovisioner=PROVISIONER`. 

Default value is `docker-compose` which is intended for testing on a local docker host.
This is currently the only implemented option. See [`README.docker-compose.md`](README.docker-compose.md) for more details.

#### Deployment Types

Different types of deployment can be provisioned.
The default deployment is `singlenode` with only a single instance of Keycloak server and a database.
Additional options are `cluster` and `crossdc` which can be enabled with a profile (see below).

#### Usage

Usage: `mvn verify -P provision[,DEPLOYMENT_PROFILE] [-Dprovisioning.properties=NAMED_PROPERTY_SET]`.

The properties are loaded from `tests/parameters/provisioning/${provisioning.properties}.properties` file.
Individual parameters can be overriden from command line via `-D` params.

Default property set is `docker-compose/4cpus/singlenode`.

To load a custom properties file specify `-Dprovisioning.properties.file=ABSOLUTE_PATH_TO_FILE` instead of `-Dprovisioning.properties`.
This file needs to contain all properties required by the specific combination of provisioner and deployment type.
See examples in folder `tests/parameters/provisioning/docker-compose/4cpus`.

Available parameters are described in [`README.provisioning-parameters.md`](README.provisioning-parameters.md).

#### Examples:
- Provision a single-node deployment with docker-compose: `mvn verify -P provision`
- Provision a cluster deployment with docker-compose: `mvn verify -P provision,cluster`
- Provision a cluster deployment with docker-compose, overriding some properties: `mvn verify -P provision,cluster -Dkeycloak.scale=2 -Dlb.worker.task-max-threads=32`
- Provision a cross-DC deployment with docker-compose: `mvn verify -P provision,crossdc`
- Provision a cross-DC deployment with docker-compose using a custom properties file: `mvn verify -P provision,crossdc -Dprovisioning.properties.file=/tmp/custom-crossdc.properties`


#### Provisioned System

The `provision` operation will produce a `provisioned-system.properties` inside the `tests/target` directory 
with information about the provisioned system such as the type of deployment and URLs of Keycloak servers and load balancers.
This information is then used by operations `generate-data`, `import-dump`, `test`, `teardown`.

Provisioning operation is idempotent for a specific combination of provisioner+deployment. 
When running multiple times the system will be simply updated based on the new parameters.
However when switching between different provisioiners or deployment types it is **always necessary** 
to tear down the currently running system.

**Note:** When switching deployment type from `singlenode` or `cluster` to `crossdc` (or the other way around) 
it is necessary to update the generated Keycloak server configuration (inside `keycloak/target` directory) by 
adding a `clean` goal to the provisioning command like so: `mvn clean verify -Pprovision …`. It is *not* necessary to update this configuration 
when switching between `singlenode` and `cluster` deployments.

#### Manual Provisioning

If you want to generate data or run the test against an already running instance of Keycloak server
you need to provide information about the system in a properties file.

Create file: `tests/target/provisioned-system.properties` with the following properties:
```
keycloak.frontend.servers=http://localhost:8080/auth
keycloak.admin.user=admin
keycloak.admin.password=admin
```
and replace the values with your actual information. Then it will be possible to run tasks: `generate-data` and `test`.

The tasks: `export-dump`, `import-dump` and `collect` (see below) are only available with the automated provisioning
because they require direct access to the provisioned services.


### Collect Artifacts

Usage: `mvn verify -Pcollect`

Collects artifacts such as logs from the provisioned system and stores them in `tests/target/collected-artifacts/${deployment}-TIMESTAMP/`.
When used in combination with teardown (see below) the artifacts are collected just before the system is torn down.

### Teardown

Usage: `mvn verify -Pteardown [-Dprovisioner=<PROVISIONER>]`

**Note:** Unless the provisioned system has been properly torn down the maven build will not allow a cleanup of the `tests/target` directory
because it contains the `provisioned-system.properties` with information about the still-running system.


## Testing

### Generate Test Data

Usage: `mvn verify -P generate-data [-Ddataset=NAMED_PROPERTY_SET] [-DnumOfWorkers=N]`. Workers default to `1`.

The parameters are loaded from `tests/src/test/resources/dataset/${dataset}.properties` file with `${dataset}` defaulting to `default`.

To use a custom properties file specify `-Ddataset.properties.file=ABSOLUTE_PATH_TO_FILE` instead of `-Ddataset`.

To generate data using a different version of Keycloak Admin Client set property `-Dserver.version=SERVER_VERSION` to match the version of the provisioned server.

To delete the generated dataset add `-Ddelete=true` to the above command. Dataset is deleted by deleting individual realms.

#### Examples:
- Generate the default dataset. `mvn verify -P generate-data`
- Generate the `1r_10c_100u` dataset. `mvn verify -P generate-data -Ddataset=1r_10c_100u`

#### Export Database

To export the generated data to a data-dump file enable profile `-P export-dump`. This will create a `${DATASET}.sql.gz` file next to the dataset properties file.

Example: `mvn verify -P generate-data,export-dump -Ddataset=1r_10c_100u`

#### Import Database

To import data from an existing data-dump file use profile `-P import-dump`.

Example: `mvn verify -P import-dump -Ddataset=1r_10c_100u`

If the dump file doesn't exist locally the script will attempt to download it from `${db.dump.download.site}` which defaults to `https://downloads.jboss.org/keycloak-qe/${server.version}` 
with `server.version` defaulting to `${project.version}` from `pom.xml`.

**Warning:** Don't override dataset parameters (with `-Dparam=value`) when running export/import because then the contents of dump file might not match the properties file.


### Run Tests

Usage: `mvn verify -P test [-Dtest.properties=NAMED_PROPERTY_SET]`. Default property set is `oidc-login-logout`.

The parameters are loaded from `tests/parameters/test/${test.properties}.properties` file.
Individual properties can be overriden from command line via `-D` params.

To use a custom properties file specify `-Dtest.properties.file=ABSOLUTE_PATH_TO_FILE` instead of `-Dtest.properties`.

#### Dataset

When running the tests it is necessary to define the dataset to be used.

| Parameter | Description | Default Value |
| --- | --- | --- | 
| `dataset` | Name of the dataset to use. Individual parameters can be overriden from CLI. For details see the section above. | `default` |
| `sequentialRealmsFrom` | Use sequential realm iteration starting from specific index. Must be lower than `numOfRealms` parameter from dataset properties. Useful for user registration scenario. | `-1` random iteration |
| `sequentialUsersFrom` | Use sequential user iteration starting from specific index. Must be lower than `usersPerRealm` parameter from dataset properties. Useful for user registration scenario. | `-1` random iteration |

#### Common Test Run Parameters

| Parameter | Description | Default Value |
| --- | --- | --- | 
| `gatling.simulationClass` | Classname of the simulation to be run. | `keycloak.OIDCLoginAndLogoutSimulation`  |
| `usersPerSec` | Arrival rate of new users per second. Can be a floating point number. | `1.0` for OIDCLoginAndLogoutSimulation, `0.2` for AdminConsoleSimulation |
| `rampUpPeriod` | Period during which the users will be ramped up. (seconds) | `15` |
| `warmUpPeriod` | Period with steady number of users intended for the system under test to warm up. (seconds) | `15` |
| `measurementPeriod` | A measurement period after the system is warmed up. (seconds) | `30` |
| `filterResults` | Whether to filter out requests which are outside of the `measurementPeriod`. | `false` |
| `userThinkTime` | Pause between individual scenario steps. | `5` |
| `refreshTokenPeriod`| Period after which token should be refreshed. | `10` |
| `logoutPct`| Percentage of users who should log out at the end of scenario. | `100` |

| Test Assertion | Description | Default Value |
| --- | --- | --- | 
| `maxFailedRequests`| Maximum number of failed requests. | `0` |
| `maxMeanReponseTime`| Maximum mean response time of all requests. | `300` |

#### Test Run Parameters specific to `OIDCLoginAndLogoutSimulation`

| Parameter | Description | Default Value |
| --- | --- | --- | 
| `badLoginAttempts` | | `0`  |
| `refreshTokenCount` | | `0` |

#### Examples:

- Run test with default test and dataset parameters:

`mvn verify -P test`

- Run test specific test and dataset parameters:

`mvn verify -P test -Dtest.properties=oidc-login-logout -Ddataset=1r_10c_100u`

- Run test with specific test and dataset parameters, overriding some from command line:

`mvn verify -P test -Dtest.properties=admin-console -Ddataset=1r_10c_100u -DrampUpPeriod=30 -DwarmUpPeriod=60 -DusersPerSec=0.3`

#### Running `OIDCRegisterAndLogoutSimulation`

Running the user registration simulation requires a different approach to dataset and how it's iterated.
- It requires sequential iteration instead of the default random one.
- In case some users are already registered it requires starting the iteration from a specific index .

##### Example A: 
1. Generate dataset with 0 users: `mvn verify -P generate-data -DusersPerRealm=0`
2. Run the registration test:

`mvn verify -P test -D test.properties=oidc-register-logout -DsequentialUsersFrom=0 -DusersPerRealm=<MAX_EXPECTED_REGISTRATIONS>`

##### Example B:
1. Generate or import dataset with 100 users: `mvn verify -P generate-data -Ddataset=1r_10c_100u`. This will create 1 realm and users 0-99.
2. Run the registration test starting from user 100:

`mvn verify -P test -D test.properties=oidc-register-logout -DsequentialUsersFrom=100 -DusersPerRealm=<MAX_EXPECTED_REGISTRATIONS>`


### Testing with HTTPS

If the provisioned server is secured with HTTPS it is possible to set the truststore which contains the server certificate.
The truststore is used in phases `generate-data` and `test`.

Usage: `mvn verify -P generate-data,test -DtrustStore=<PATH_TO_TRUSTSTORE> -DtrustStorePassword=<TRUSTSTORE_PASSWORD>`

To automatically generate the truststore file run a utility script `tests/create-truststore.sh HOST:PORT [TRUSTSTORE_PASSWORD]`.
The script requires `openssl` and `keytool` (included in JDK).

Example: `tests/create-truststore.sh localhost:8443 truststorepass`


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

### JStat - JVM memory statistics

To enable jstat monitoring use `-Pjstat` option.
This will start a `jstat` process in each container with Wildfly-based service (Keycloak, Infinispan, Load balancer)
and record the statistics in the `standalone/log/jstat-gc.log` file. These can be then collected by running the `mvn verify -Pcollect` operation.

To enable creation of PNG charts based on the jstat output use `-Pgnuplot`.

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

#### Run OIDCLoginAndLogoutSimulation from IntelliJ

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

