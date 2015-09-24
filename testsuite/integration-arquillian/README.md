# Keycloak Integration Testsuite with Arquillian

## Structure

```
integration-arquillian
│
├──servers  (submodules enabled via profiles)
│  ├──wildfly
│  └──eap6
│
└──tests
   ├──base
   └──adapters  (submodules enabled via profiles, all depend on base)
      ├──wildfly
      ├──wildfly-relative  (needs servers/wildfly)
      ├──wildfly8
      ├──as7
      ├──tomcat
      └──karaf

```

## General Concepts

The testsuite supports **multiple server runtimes** for the Keycloak server.
The **default is Undertow** which is the fastest and easiest option, and runs in the same JVM as the tests.

Other options are **Wildfly 9** and **EAP 6**. These have some additional requirements and limitations:
1. The selected server module must be built before any tests can be run. 
All server-side configuration is done during this build (e.g. datasource configuration).
Once server artifact is built the tests modules can unpack it via `maven-dependency-plugin` into their working directory before running.
2. Before the selected server module can be built the `keycloak/distribution` module also needs to be built.

### Server Runtimes

TODO: explain why separate module, list config options, note on migration modules

### Base Testsuite

login flows + account management

admin ui

abstract adapter tests

### Adapter Tests

test servlets: demo, session

examples

## Running the Tests

### Undertow

### Wildfly or EAP 6

### Adapters

### Supported Browsers