# Configuring logging

The test framework provides built-in support for logging, including the following features:

* Log filtering - automatically filter out log output for successful tests
* Test information - log details on test execution, including setup and cleanup
* Configure Keycloak logging - automatically configure Keycloak server logging
* Log forwarding - forward output from the managed Keycloak test server, and managed database containers

## Configuring logging

To configure default logging for a test module add `src/main/resources/keycloak-test.properties` with the default 
configuration. For example:

```properties
kc.test.log.level=WARN
kc.test.log.filter=true
kc.test.log.category."testinfo".level=INFO
```

Developers that are running tests locally can override the defaults with either `.env.test` file at the root of the test 
module, or using environment variables when running tests from the command line. For example:

```properties
KC_TEST_LOG_FILTER=FALSE KC_TEST_LOG_LEVEL=INFO mvn test
```

### Configure log levels

Supported log levels include `TRACE`, `DEBUG`, `INFO`, `WARN`, and `ERROR`.

The default/root log level is configured with `KC_TEST_LOG_LEVEL` / `kc.test.log.level`. To prevent a lot of noise in 
logs you may want to set this to `WARN` by default.

To configure log levels for a specific category use 
`kc.test.log."<category>".level` / `KC_TEST_LOG_CATEGORY__<CATEGORY>__LEVEL`, for example:

```properties
kc.test.log.category."org.keycloak".level=INFO
KC_TEST_LOG_CATEGORY__ORG_KEYCLOAK__LEVEL=INFO
```

Some useful categories include:

* `testinfo` - Test lifecycle information. This serves two purposes, firstly it tells you when a test class and test 
  method starts, and if the test succeeded or not. Secondly, it breaks up the log output so you can see exactly what is 
  causing logs for instance from the Keycloak server.
* `org.keycloak.testframework` - Logging from the test framework itself. Setting this to `DEBUG` can be helpful to debug 
  any issues with the test framework itself, or custom suppliers.
* `org.keycloak` - Logging from the Keycloak server. If you set this to `DEBUG` for example, but don't want debug from 
  the test framework, also explicitly set `org.keycloak.testframework` to for example `INFO`
* `managed.keycloak` - Log output from the managed Keycloak server if you are running the server in `distribution` mode 
  (which is the default)
* `managed.db` - Output from database containers are included in this category. Standard out is logged with `DEBUG` 
  level, while standard error is logged with `WARN` level
* `managed.infinispan` - Output from the external Infinispan container is included in this category. Standard out is 
  logged with `DEBUG` level, while standard error is logged with `WARN` level

### Enable log filtering

Log filtering suppresses log output for successful tests, while providing output for failing tests. This can be useful 
to reduce the amount of logging, while at the same time provide log output to debug failing tests.

To enable the log filter capability set `KC_TEST_LOG_FILTER` / `kc.test.log.filter` to `true`.

### Enable colors

To enable colors to log output set `KC_TEST_CONSOLE_COLOR` / `kc.test.console.color` to `true`. For CI or anywhere log 
files are being parsed this is usually not a good idea.

## Examples

### Example default log configuration

This provides a more complete example of a `src/test/resource/keycloak-test.properties` file:

```properties
kc.test.log.level=WARN

kc.test.log.filter=true

kc.test.log.category."testinfo".level=INFO
kc.test.log.category."org.keycloak.testframework".level=INFO
kc.test.log.category."org.keycloak".level=WARN
kc.test.log.category."managed.keycloak".level=WARN
kc.test.log.category."managed.db".level=WARN
kc.test.log.category."managed.infinispan".level=WARN
```

This should serve as a good starting point balancing the need of log information with not producing too much noise.

### Example local `.env.test` file

For developers wanting to override the defaults for a more personal configuration, add a `.env.test` file to the root of 
the test module. This file should not be committed to the git repository, as it should be personal for each developer.

Here's an example `.env.test` file that is enabling debug logging for some specific categories:

```properties
KC_TEST_CONSOLE_COLOR=true
KC_TEST_LOG_LEVEL=WARN

KC_TEST_LOG_CATEGORY__TESTINFO__LEVEL=DEBUG
KC_TEST_LOG_CATEGORY__ORG_KEYCLOAK___LEVEL=DEBUG
KC_TEST_LOG_CATEGORY__ORG_KEYCLOAK_TEST__LEVEL=DEBUG
KC_TEST_LOG_CATEGORY__MANAGED_KEYCLOAK__LEVEL=DEBUG
KC_TEST_LOG_CATEGORY__MANAGED_DB__LEVEL=DEBUG
KC_TEST_LOG_CATEGORY__MANAGED_INFINISPAN__LEVEL=DEBUG
```

### Examples using environment variables

Disabling console colors if enabled in default test configuration:
```properties
KC_TEST_CONSOLE_COLOR=false mvn test
```

Enabling the log filter if not enabled by default, and for example you want to enable this only in CI:
```properties
KC_TEST_LOG_FILTER=true mvn test
```

Quickly increasing log levels for a specific category when running a specific test:
```properties
KC_TEST_LOG_CATEGORY__ORG_KEYCLOAK__LEVEL=DEBUG mvn test -Dtest=ProblematicTest
```
