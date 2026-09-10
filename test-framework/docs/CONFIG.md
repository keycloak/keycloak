# Configuring

There are a few alternative approaches for configure the test framework, with the following ordinal:

* System properties
* Environment variables
* `.env.test` file in the project hierarchy
* `keycloak-test.properties` on the classpath
* A properties file specified with `KC_TEST_CONFIG` environment variable

The general syntax for configuration keys depend on the source. Keys in property files are `.` separated and lower-case,
while for environment variables they are `_` separated and upper-case. For example:

* Properties: `kc.test.myproperty`
* Environment: `KC_TEST_MYPROPERTY`

## Best practices

For a test module define default configuration in `src/test/resources/keycloak-test.properties`. This file should be
committed to version control systems and shared with other users.

When running tests in CI use environment variables, which are especially useful in matrix jobs.

When running tests locally use `.env.test`. This file should not be committed to version control systems. This makes
it easy to use the same settings when running from Maven or running from the IDE.

## Using system properties

This is not the most convenient way as it is both cumbersome to set system properties when running tests from the IDE,
and when running tests using Maven. Environment variables are recommended instead of system properties.

Example:
```shell
mvn test -Dkc.test.browser=firefox
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

```properties
KC_TEST_BROWSER=firefox
```

For multi-modal Maven projects the `.env.test` file can be located in the current module, or one of its parent modules.
This allows sharing configuration across multiple test modules.

### Using a properties file

Using a property file allows creating a set of configuration which can be committed to a Git repository to be shareable.

By default `keycloak-test.properties` is loaded from the classpath. To use a different config file use the 
`KC_TEST_CONFIG` option with the path to the file.

For example create the file `/tmp/mytestconfig.properties` with the following contents:
```properties
kc.test.browser=firefox
```

Then run tests with:
```shell
KC_TEST_CONFIG=/tmp/mytestconfig.properties mvn test 
```
