# Keycloak on Quarkus

The module holds the codebase to run Keycloak on top of [Quarkus](https://quarkus.io/):

```
├── deployment
│   ├── Build-time codebase with all the necessary steps to build and configure the server
│
├── runtime
│   ├── Runtime codebase with all the runtime code
│
└── server
    ├── The server itself, only responsible for generating the server artifacts
``` 

## Activating the Module

When build from the project root directory, this module is only enabled if the installed JDK is 11 or newer. 

## Building

Ensure you have JDK 11 (or newer) installed.

To build the module and produce the artifacts to run a server:

    mvn -f ../pom.xml clean install -DskipTestsuite -DskipExamples -DskipTests

### Building the Distribution
    
To build the module as well as the distribution packages:

    mvn -f ../pom.xml clean install -DskipTestsuite -DskipExamples -DskipTests -Pdistribution

The distribution packages (ZIP and TAR) should be available at [../distribution/server-x-dist](../distribution/server-x-dist/target).

Alternatively, you can also build the distribution directly by running the following command:

    mvn -f ../distribution/server-x-dist/pom.xml clean install

## Running

By default, the HTTP port is disabled and you need to provide the key material to configure HTTPS. If you want to enable
the HTTP port, run the server in development mode as follows:

    java -jar server/target/lib/quarkus-run.jar start-dev

## Contributing

### Development Mode

To run the server in development mode:

    mvn -f server/pom.xml compile quarkus:dev -Dquarkus.args="start-dev"

You should be able to attach your debugger to port `5005`.

For debugging the build steps, you can suspend the JVM by running:

    mvn -f server/pom.xml -Dsuspend=true compile quarkus:dev -Dquarkus.args="start-dev"

When running using `quarkus:dev` you should be able to do live coding whenever code changes within the `server` module. Changes you make to transient dependencies from the server extension (e.g: services, model, etc) won't be reflected into the running server. However, you can still leverage the hot swapping capabilities from your IDE to make changes at runtime.

NOTE: Although still very handy during development, there are some limitations when running in dev mode that
blocks us to leverage all the capabilities from Quarkus dev mode. For instance, hot-reload of transient dependencies from the server extension (e.g.: keycloak-* dependencies) does not work. More improvements should be expected to improve the experience.

NOTE: When developing custom providers, you should be able to benefit from live coding as long as you keep changes within the `server` module. 

Alternatively, you can run the server in development mode from your IDE. For that, run the `org.keycloak.quarkus._private.IDELauncher` main class.
