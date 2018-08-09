# Keycloak Server Config Migration Testsuite

## Test workflow
There are performed several steps before actual test is executed. The steps are divided into different maven lifecycle phases to make sure it goes in specified wanted order.

### `process-resources`
Unpack clean keycloak-server-dist 

### `compile`
Copy standalone/domain resources to `${jbossHome}/standalone/configuration` and `${jbossHome}/domain/configuration`

### `process-classes`
`maven-exec-plugin` is used to read current master configs and saves the output to `${project.build.directory}/master-${config.name}.txt`

### `generate-test-sources`
Files `${jbossHome}/standalone/configuration/keycloak-server.json` and `${jbossHome}/domain/configuration/keycloak-server.json` are created.

Configuration files (`standalone.xml`, `standalone-ha.xml`, `domain.xml`, `host-master.xml`) are overwritten by those from previous version.

### `process-test-sources`
Migration scripts are applied using **offline mode**. Temporary data are removed.

### `generate-test-resources`
`wildfly-maven-plugin` is used to start and shutdown container with different configs. It is done to let subsystems to upgrade themselves during first run.

### `process-test-resources`
`maven-exec-plugin` is used to read migrated configs and saves the output to `${project.build.directory}/migrated-${config.name}.txt`

### `default-test`
`org.keycloak.test.config.migration.ConfigMigrationTest` is executed. It compares generated outputs from ${project.build.directory}

If config outputs don't equal to each other, **by default** the test will compare outputs more deeply to get more readable output. It fails on first found difference.

This can be overwritten by adding property: `-Dget.simple.full.comparison=true` to the test command. In that case it'll perform assert on the two config outputs.

## Properties

### maven
* jbossHome
    * default: `${project.build.directory}/keycloak-${project.version}`
    * specifies path to jbossHome dir
* migrated.version
    * default: `1.8.1`
    * specifies version it is migrated from
* master.version
    * default: `${project.version}`
    * specifies version it is migrated to

## How to run tests

note: `keycloak-server-dist` module has to be build first (`mvn install -f keycloak/pom.xml -Pdistribution`)

* `mvn clean install` tests migration from 1.8.1.Final to current master. It goes thru whole test workflow. Deep comparison is done.
* `mvn clean install -Dget.simple.full.comparison=true` does assert on outputs instead of deep comparison.
* `mvn clean process-test-sources -Dskip.rename.configs=true` applies migration scripts to current master. It can be used to make sure the current keycloak version doesn't break the scripts.
* `mvn clean install -Dskip.rename.configs=true` applies scripts to current master **and** verifies the scripts doesn't actually change anything.
