# New Tests

The new tests parent module incorporate:
* tests written in _Keycloak Test Framework_: _base_, _clustering_, _webauthn_ 
* support modules for utilities and extensions: _utils_, _utils-shared_, _custom-providers_, _custom-scripts_
* migration utility (from _Arquillian_ to _Keycloak Test Framework_)

## Tests modules

Keep attention where a particular test belong, most of the tests are placed in the **_base_** module, but some require very specific configuration, like **_clusterting_** or **_webauthn_**.

Support for test suites:
* Based on _JUnit Platform Suite_
* All currently available suites are in the [suites package](../base/src/test/java/org/keycloak/tests/suites)
* Part of the _GitHub Actions_ [CI worklow](../../.github/workflows/ci.yml)

Useful guides:
* [_Keycloak Test Framework_](../../test-framework/docs/README.md)
* [_FIPS_ tests execution](FIPS_104-2.md) - how to set up a local machine to successfully execute the _FIPS_ tests.

## Utilities

### _utils_
Some test classes need complex executions and use a logic, which can be reused over and over.
To remove a boilerplate code, this is the place - the _utils_ module, where some static utilities can be placed.

Are you thinking of adding a new util class?
1. Always check if this functionality is part of the test framework.
2. Check the existing util classes.
3. If none of them are sufficient, then create a new one, but:
   1. Be cautious, since it creates a new complexity.
   2. Be as specific with the names as possible => clear package and class name.

### _utils-shared_
The _utils-shared_ module is for migration purpose only and will be removed once the migration is over! Meaningful utilities will be refactored and moved to _utils_.

It prevents the new tests to be dependent on the old _Arquillian_ testsuite and in the same time, it simplifies the overall migration. Is a util class (in the old testsuite) shared by multiple test cases?
  * Migrate util class into this module first.
  * Then migrate your test.

## Extensions

### _custom-providers_
The _custom-providers_ module is used within tests to specify a custom extension, built on top of the _Keycloak SPI_.
Every custom extension with particular factory must be stored in this module!

### _custom-scripts_
Same applies for the _custom-scripts_ module, it deploys custom authenticators (feature _scripts_) written in _JavaScript_.
Every such script must be stored in this separate module!

All of them can be deployed as a dependency within the _KeycloakServerConfig_ implementation, please check the _Keycloak Test Framework_ docs.

## Migration

The _migration-util_ is a tool, written in Java, to simplify the overall migration. It can be executed via _migrate.sh_ script.
Its purpose is to copy/paste selected test case, and parse and replace patterns to follow the _Keycloak Test Framework_.

Pay attention! It doesn't support every pattern just speed up the initial phase, before digging into a manual migration.

Example of usage: `./migrate.sh AttributesTest`

Please, consider using this script for making a commit, because it allows to preserve a history tree for a specific test case:
`commit-migration.sh`

Available guides, which cover the required knowledge to successfully migrate a single test case:
* [Migrating tests](MIGRATING_TESTS.md) - Quick start for a single test migration (includes the migration utility best practice)
* [Manual migration](MANUAL_MIGRATION.md) - Guide with recommendations on how to fully migrate a test

Once the migration is over the _migration-util_ module will be removed!
