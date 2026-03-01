# Keycloak Test Framework

The Keycloak JUnit test framework makes it easy to write tests for Keycloak and extensions. Behind the scenes the
framework handles the lifecycle of Keycloak, the database, and any injected resources such as realms and clients.

Tests simply declare what they want, including specific configuration, and the framework takes care of the rest.

# Chapters

* [Getting started](GETTING_STARTED.md) - Quick start for using the test framework
* [Configuring](CONFIG.md) - Overview of how to configure the test framework
* [Writing tests](WRITING_TESTS.md) - How to write tests
* [Running tests](RUNNING_TESTS.md) - How to run tests using different configuration options
* [Configuring logging](LOGGING.md) - How to configure logging when running tests
* [Using test suites](SUITES.md) - How to use tests suites with custom configuration
* [Best practices](BEST_PRACTICES.md) - Best practices and common pitfalls
* [Writing extensions](EXTENSIONS.md) - Writing test framework extensions
* [GitHub Actions](GITHUB.md) - Support for creating job summary on GitHub Actions