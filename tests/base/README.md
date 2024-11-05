
# Running tests

Run all tests:

```
mvn test
```

## Runtime configuration

When running tests from the command-line use environment variables to configure the test framework. As this is not
very convenient when running from an IDE add environment variables to the file `tests/base/.env`. Bear in mind this
environment file is picked-up when you run tests from the command-line as well.


## Server run-mode

By default, the tests run with a standalone distribution of Keycloak. To run with a different mode run with:

```
KC_TEST_SERVER=<server mode>
```

Replace `<server mode>` with one of `distribution`, `embedded`, or `remote`.

Embedded mode can be useful when debugging tests through the IDE as it allows debugging both test and Keycloak code
directly without the need to attach a remote debugger.

Remote mode can be useful when developing tests as the tests run faster since the Keycloak server is not started and
stopped when running tests. One caveat here is that some tests require a specific configuration. The required configuration
will be logged by the tests when running it, to make it easier to start Keycloak manually with the required configuration.


## Database testsuite

By default, the tests run with H2 in-memory database. To run with a different database run with:

```
KC_TEST_DATABASE=<database> test -Dtest=DatabaseTestsuite
```

Replace `<database>` with one of `mariadb`, `mssql`, `mysql`, `oracle`, or `postgres`.


## Using Chrome or Firefox

By default, the tests run with HtmlUnit. To run with a different browser run with: 

```
KC_TEST_BROWSER=<browser>
```

Replace `<browser>` with one of `chrome`, `chrome-headless`, `firefox`, or `firefox-headless`.

