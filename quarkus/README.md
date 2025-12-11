# Keycloak on Quarkus

The module holds the codebase to run Keycloak on top of [Quarkus](https://quarkus.io/):

```
├── container
│   ├── Dockerfile, e.g. used by the Testsuite
│
├── deployment
│   ├── Build-time codebase with all the necessary steps to build and configure the server
│
├── dist
│   ├── Packaging the quarkus distribution
│
├── runtime
│   ├── Runtime codebase with all the runtime code
│
├── server
│   ├── The server itself, only responsible for generating the server artifacts
│
└── tests
    ├── Integration tests for the quarkus distribution
``` 

## Prerequisites
Ensure you have at least JDK 11 installed.
Your shell is located at the `quarkus` submodule. (`cd quarkus`)

### Activating the Module from the root directory
When a build from the project root directory is started, this module is only enabled if your installed JDK is 11 or newer. 

## Building the project the first time

To build this module and produce the artifacts to run a server, you first need to build the main codebase once. This step will put required modules of keycloak into your local maven cache in package `org.keycloak`:

    ../mvnw -f ../pom.xml clean install -DskipTestsuite -DskipExamples -DskipTests

This build can take some time, usually around two to four minutes depending on your hardware, and even longer depending on the maven packages that need to be downloaded and installed to the cache.

## Building the Keycloak Quarkus distribution

After the main codebase is built, you can build the quarkus distribution, including the zip and tar.gz files, by invoking the following command:
    
    ../mvnw clean install -DskipTests

This command produces the distribution artifacts as ZIP and TAR file. The artifacts for the quarkus distribution will be available at the `/dist/target` subdirectory afterwards.

As an alternative, you can build the distribution artifacts directly without a rebuild of the code by running the following command:

    ../mvnw -f dist/pom.xml clean install

## Running in Keycloak development mode
When you start Keycloak in production mode, the HTTP port is disabled by default, and you need to provide the key material to configure HTTPS, a hostname and other configuration suitable for production. 

For development purposes, you can run the server in development mode instead using `start-dev`.

### Running from your IDE
Alternatively, you can run the server in development mode from your IDE. For that, run the `org.keycloak.Keycloak` main class in the `tests/junit5` directory.

If you have problems starting the server in debug mode, add following environment variables to your run config:

    -Djava.util.logging.manager=org.jboss.logmanager.LogManager
    -Djava.util.concurrent.ForkJoinPool.common.threadFactory=io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThreadFactory

Set the `kc.home.dir` environment variable for keeping state between startups:
    
    -Dkc.home.dir=.kc 

### Running from the command line

To run the server in Quarkus' development mode, invoke the following command:

    ../mvnw -f server/pom.xml compile quarkus:dev -Dkc.config.built=true -Dquarkus.args="start-dev"

You will be able to attach your debugger to port `5005`.

For debugging the build steps right after start, you can suspend the JVM by running:

    ../mvnw -f server/pom.xml -Dsuspend=true compile quarkus:dev -Dkc.config.built=true -Dquarkus.args="start-dev"

When running using `quarkus:dev` you are able to do live coding whenever you change / add code in the `server` module, for example when creating a new custom provider.

There are currently limitations when running in development mode that block us to use all capabilities the Quarkus development mode has to offer. For instance, hot-reload of transient dependencies from keycloak (e.g.: keycloak-* dependencies) do not work. Expect more improvements in this area, and feel free to reach out if you want to help, using our [discussions](https://github.com/keycloak/keycloak/discussions/categories/keycloak-x-quarkus-distribution) or the development mailing list.

## Contributing
Please make sure to read our [Contribution Guidelines](../CONTRIBUTING.md) before contributing.

## Running tests
Keycloaks Quarkus distribution module uses a new testsuite more integrated into the quarkus platform.

### Running tests from your IDE
The tests can also be run from an IDE GUI such as Intellij IDEA. There are different kinds of tests:
* Unit tests: Located in the respective module (`deployment`, `runtime`)
* Integration tests:
  * `@CLITest` annotated: These tests have no prerequisites and are whitebox tests, so you can easily debug them.
  * `@DistributionTest` annotated: These tests need a build of the distribution artifacts first to run. These are blackbox tests, so not as easily debuggable as `@CLITest` annotated tests. Mostly used for scenarios when a `build` is involved or build options need to change, as this invocation happens in a different JVM.

### Running container-based tests
The `@DistributionTest` annotated tests can use different runtimes, e.g. plain JVM or a docker container. Per default, they use the plain JVM mode. 

To run them from a container image instead, you need to build the distribution first. Then you can use the flag `-Dkc.quarkus.tests.dist=docker`. This builds a docker image from the provided distribution archives and runs the `@DistributionTest` annotated tests for them.

There are some tests annotated `@RawDistOnly` which prevents them from running in docker. You'll find a short reason in the respective annotation.

The container based tests are using Testcontainers to spin up the container image and can be considered tech preview.

### Running database tests
There are also some container based tests to check if Keycloak starts using one of the supported database vendors. They are annotated with `@WithDatabase`. 

These tests are disabled by default. They using Quarkus development mode predefined database containers by default and can be run in the `tests` subdirectory by using e.g. 

    ../mvnw clean install -Ptest-database -Dtest=MariaDBDistTest

to spin up a MariaDB container and start Keycloak with it.

To use a specific database container image, use the option `-Dkc.db.postgresql.container.image` to specify the image tag of the postgres image to use or `-Dkc.db.mariadb.container.image=<name:tag>` for mariadb.

Example:

    ../mvnw clean install -Ptest-database -Dtest=PostgreSQLDistTest -Dkc.db.postgresql.container.image=postgres:alpine
    
### Updating Expectations

Changing to the help output will cause HelpCommandDistTest to fail. You may use:

    KEYCLOAK_REPLACE_EXPECTED=true ../mvnw clean install -Dtest=HelpCommandDistTest 

to replace the expected output, then use a diff to ensure the changes look good.
