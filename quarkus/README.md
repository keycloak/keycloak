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

The module isn't enabled by default. To enable it please activate the `quarkus` profile. 

## Building

To build the module and produce the artifacts to run a server:

    mvn -f ../pom.xml clean install -DskipTestsuite -DskipExamples -DskipTests -Pquarkus

### Building the Distribution
    
To build the module as well as the distribution packages:

    mvn -f ../pom.xml clean install -DskipTestsuite -DskipExamples -DskipTests -Pquarkus,distribution

The distribution packages (ZIP and TAR) should be available at [../distribution/server-x](../distribution/server-x/target).

## Running

By default, the HTTP port is disabled and you need to provide the key material to configure HTTPS. If you want to enable
the HTTP port, run the server in development mode as follows:

    java -jar server/target/lib/quarkus-run.jar --profile=dev

## Contributing

### Development Mode

To run the server in development mode:

    cd server
    mvn compile quarkus:dev
    
You should be able to attach your debugger to port `5005`.

Changes to files such as `server/src/main/resources` or `server/src/main/resources/META-INF/keycloak.properties` should
be recognized automatically when running in development mode.

However, considering that there is no real code in the `server` module (but from `runtime` and its dependencies), changes you make to
dependencies (e.g: services, model, etc) won't be reflected into the running server. However, you can still leverage the
hot reload capabilities from your IDE to make changes at runtime.

NOTE: We need to improve DevX and figure out why changes to dependencies are not being recognized when running tests or running 
Quarkus Dev Mode. 
