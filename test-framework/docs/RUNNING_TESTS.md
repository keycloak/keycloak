# Running tests

Tests can be run from your favourite IDE, or from the command-line using Maven. Simply run the tests and the framework
does the rest.

When running tests there are a few things than be configured, including:

* Server type
* Database type
* Browser type

## Server type

The Keycloak server can be started by the test framework in different modes, including:

* `distribution` - Installs Keycloak from a `zip` file and starts as an external process (default mode)
* `embedded` - Executes Keycloak within the JVM used to execute tests
* `remote` - Connects to a remote Keycloak server running on `http://localhost:8080`

Currently, Keycloak will always run on `localhost` using the ports `8080`, `8443` and `9001`. These ports need to be
free to run tests using the test framework.

To run tests using a different mode use the `KC_TEST_SERVER` option, for example:
```shell
KC_TEST_SERVER=embedded mvn test 
```

The distribution mode will re-use the previous installation if the `zip` last modified date has not changed. If it has
changed it will re-install. Keycloak will be installed to `<SYSTEM TEMP DIRECTORY>/kc-test-framework/`.

By default, the Keycloak server is started for the first test, then left running until all tests are completed. If a 
test requires different server configuration the server is automatically restarted.

### Re-use

When using `distribution` mode it is possible to leave the Keycloak server running in the background allowing running
tests quickly without waiting for the server to start again. This will also restart the server if the test requires
different server config. To enable re-use use the `KC_TEST_SERVER_REUSE=true` option.

### `kcw`

When using the `remote` mode if Keycloak is not already running the test framework will output the required server
configuration to make it easy to start a correctly configured Keycloak server.

If the `KC_TEST_SERVER_KCW` option is specified the test framework will show a complete command to start Keycloak
using `kcw`. For example setting `KC_TEST_SERVER_KCW=dev-build` will show the command to use `kcw` to build Keycloak
from the local checkout of Keycloak sources and then start Keycloak.

## Database

The test framework supports using a range of different databases when running tests:

* `dev-mem`: In-memory H2 database (default)
* `dev-file`: H2 database with a file for persistence
* `mariadb`: MariaDB test container
* `mssql`: Microsoft SQL Server test container
* `mysql`: MySQL test container
* `oracle`: Oracle test container
* `postgres`: PostgreSQL test container
* `tidb`: TiDb test container
* `remote`: Connect to a remotely running database

The test framework core (`keycloak-test-framework-core`) contains support for `dev-mem`, `dev-file`, and `remote`. To
use other databases add a dependency on the corresponding Maven module (`keycloak-test-framework-db-<db name>`).

To run tests using a different database use the `KC_TEST_DATABASE` option, for example:
```shell
KC_TEST_DATABASE=postgres mvn test 
```

### Re-use

`dev-file` and container based databases supports re-using the database, leaving the database running to enable running
tests faster.

To enable re-use use the `KC_TEST_DATABASE_REUSE=true` option. 

For containers, you also have to [enable reuse for Testcontainers](https://java.testcontainers.org/features/reuse/) (`TESTCONTAINERS_REUSE_ENABLE=true`). 

### Remote Database

If connecting to a remotely running database is desired, the following options need to be specified:

Option prefix: `kc.test.database.` / `KC_TEST_DATABASE_`

| Option                              | Description                                                         | Required? |
|-------------------------------------|---------------------------------------------------------------------|-----------|
| `vendor`/`VENDOR`                   | Database vendor (valid values mentioned above)                      | yes       |
| `user`/`USER`                       | Username of the database user                                       | yes       |
| `password`/`PASSWORD`               | Password of the database user                                       | yes       |
| `url`/`URL`                         | Full database JDBC URL                                              | yes       |
| `driver`/`DRIVER`                   | Fully qualified class name of the JDBC driver                       | no        |
| `driver.artifact`/`DRIVER_ARTIFACT` | Maven artifact containing the driver in format `groupId:artifactId` | no        |

Because some databases may require a special driver, you can specify it to the Keycloak server. To use it, Keycloak needs
to add it as a dependency, which is why you need to specify the Maven artifact coordinates.

For example, you want to run tests with an Oracle database which needs a specific jdbc driver. You would run:
```shell
KC_TEST_DATABASE=remote \
  KC_TEST_DATABASE_VENDOR=oracle \
  KC_TEST_DATABASE_USER=testUser \
  KC_TEST_DATABASE_PASSWORD=password \
  KC_TEST_DATABASE_URL=jdbc:oracle:thin:@localhost:1521/keycloak \
  KC_TEST_DATABASE_DRIVER=oracle.jdbc.OracleDriver \
  KC_TEST_DATABASE_DRIVER_ARTIFACT=com.oracle.database.jdbc:ojdbc17 \
  mvn test
```

## Browser 

The test framework supports using a range of different browsers when running tests:

* `htmlunit`: HtmlUnit WebDriver (default)
* `chrome`: Chrome WebDriver
* `chrome-headless`: Chrome WebDriver without UI
* `firefox`: Firefox WebDriver
* `firefox-headless`: Firefox WebDriver without UI


To run tests using a different mode use the `KC_TEST_BROWSER` option, for example:
```shell
KC_TEST_BROWSER=chrome mvn test 
```

Resolving the web driver is done either automatically by Selenium, or the binary can be specified directly either
through using `CHROMEWEBDRIVER` and `GECKOWEBDRIVER` environment variables. It can also be configured by setting 
`KC_TEST_BROWSER_DRIVER` to the path of the driver.
