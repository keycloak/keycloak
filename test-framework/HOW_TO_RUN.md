# Running tests

Tests can be run from your favourite IDE, or from the command-line using Maven. Simply run the tests and the framework
does the rest.

## Configuring the test framework

When running tests there are a few things than be configured:

* Server type
* Database type
* Browser type

There are a few options on how to configure the test framework, with the following ordinal:

* System properties
* Environment variables
* `.env.test` file in the project hierarchy
* A properties file specified with `kc.test.config` system property or `KC_TEST_CONFIG` environment variable

For more details about the test framework, see the [README](README.md).

### Using system properties

This is not the most convenient way as it is both cumbersome to set system properties when running tests from the IDE,
or when running tests using Maven.

For Maven see [Maven Surefire Plugin documentation](https://maven.apache.org/surefire/maven-surefire-plugin/examples/system-properties.html) on how to
set system properties when using the Surefire plugin to run tests. A brief example would look something like:

```xml
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <systemPropertyVariables>
            <kc.test.browser>firefox</kc.test.browser>
          </systemPropertyVariables>
        </configuration>
      </plugin>
    </plugins>
  </build>
```

### Using environment variables

When running tests from the CLI using environment variables is the recommended way. For example:

```shell
KC_TEST_BROWSER=firefox mvn test 
```

As with system properties, using environment variables within the IDE can be cumbersome.

### Using `.env.test` file

When running tests from an IDE using the `.env.test` file is very convenient, especially as this can be added to `.gitignore`
allowing developers to quickly have their own personal preference when running tests.

Example `.env.test` file:

```
KC_TEST_BROWSER=firefox
```

For multi-modal Maven projects the `.env.test` file can be located in the current module, or one of its parent modules.
This allows sharing configuration across multiple test modules.

### Using a properties file

Using a property file allows creating a set of configuration which can be committed to a Git repository to be shareable.

For example create the file `/path/mytestconfig.properties` with the following contents:

```
kc.test.browser=firefox
kc.test.server=remote
```

Then run tests with:

```shell
KC_TEST_CONFIG=/path/mytestconfig.properties mvn test 
```

## Config options

### Server

Option: `kc.test.server` / `KC_TEST_SERVER`

Valid values:

| Value        | Description                                                                                            |
|--------------|--------------------------------------------------------------------------------------------------------|
| distribution | Runs the full distribution of Keycloak in a separate JVM process                                       |
| embedded     | Runs a Keycloak server embedded in the same JVM process                                                |
| remote       | Connects to a remote Keycloak server. Requires manually configuring the server as needed for the test. |

Configuration:

| Value                                             | Description                                                            |
|---------------------------------------------------|------------------------------------------------------------------------|
| `kc.test.server.config` / `KC_TEST_SERVER_CONFIG` | The name of a KeycloakServerConfig class to use when running the tests |


### Database

Option: `kc.test.database` / `KC_TEST_DATABASE`

Valid values:

| Value    | Description                             |
|----------|-----------------------------------------|
| dev-file | H2 database with a file for persistence |
| dev-mem  | In-memory H2 database                   |
| mariadb  | MariaDB test container                  |
| mssql    | Microsoft SQL Server test container     |
| mysql    | MySQL test container                    |
| oracle   | Oracle test container                   |
| postgres | PostgreSQL test container               |
| tidb     | TiDb test container                     |

Configuration:

| Value                                               | Description                                                                                                                                                                 |
|-----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `kc.test.database.reuse` / `KC_TEST_DATABASE_REUSE` | Set to true to enable reuse of database. Requires [enabling reuse for Testcontainers](https://java.testcontainers.org/features/reuse/) (`TESTCONTAINERS_REUSE_ENABLE=true`) |

### Browser

Option: `kc.test.browser` / `KC_TEST_BROWSER`

Valid values:

| Value            | Description                  |
|------------------|------------------------------|
| chrome           | Chrome WebDriver             |
| chrome-headless  | Chrome WebDriver without UI  |
| firefox          | Firefox WebDriver            |
| firefox-headless | Firefox WebDriver without UI |

### Supplier configuration

#### Set the supplier

Option: `kc.test.<value type alias>` / `KC_TEST_<value type alias>`

#### Setting included suppliers

Option: `kc.test.<value type alias>.suppliers.included` / `KC_TEST_<value type alias>_SUPPLIERS_INCLUDED`

#### Setting excluded suppliers

Option: `kc.test.<value type alias>.suppliers.excluded` / `KC_TEST_<value type alias>_SUPPLIERS_EXCLUDED`
