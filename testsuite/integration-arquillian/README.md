# Keycloak Arquillian Integration Testsuite

## Container Lifecycles

### Keycloak Auth Server

There is only one instance of Keycloak server running during a single test run.
It is automatically started by Arquillian on the `BeforeSuite` event and stopped `AfterSuite`.

The type of container can be determined by property `-Dauth.server.container`. Default value is `auth-server-undertow`, 
other options are: `auth-server-wildfly` and `auth-server-eap7`. The values correspond to Arquillian *container qualifiers* in `arquillian.xml` config file.

**Note 1:** For the non-default options it's necessary to build a corresponding server module prior to running any of the test modules.
This can be done by building the server module directly (from `servers/wildfly`/`servers/eap7`), 
or by activating `auth-server-wildfly`/`auth-server-eap7` profile when building from the top level module.

**Note 2:** Most server-side configurations are done during the build of the server module
and included in the output artifact - which is then consumed by the test modules( if a corresponding profile is activated).
To reflect a change in server config in the test (e.g. a datasource) it's necessary to rebuild the server module after each change.

#### Migration

Migration tests can be enabled by setting `-Dmigrated.auth.server.version` property. Supported versions can be found at the bottom of `tests/pom.xml`.
When enabled, the `AuthServerTestEnricher` class will start and stop the selected migrated instance 
*before* the current auth server instance is started.

#### Cluster Setup

Cluster setup can be enabled with profile `auth-server-wildfly-cluster`.
(It is also necessary to build the server modules with this profile before running the test. See *Notes 1 and 2* above.)

Clustering tests require MULTICAST to be enabled on machine's `loopback` network interface.
This can be done by running the following commands under root privileges:
```
route add -net 224.0.0.0 netmask 240.0.0.0 dev lo
ifconfig lo multicast
```

### App Servers

Lifecycle of application server is always tied to a particular TestClass.

Each *adapter* test class is annotated by `@AppServerContainer("app-server-*")` annotation 
that links it to a particular Arquillian container in `arquillian.xml`.
The `AppServerTestEnricher` then ensures the server is started before and stopped after all tests methods in the class.
In case the `@AppServerContainer` annotation has no value it's assumed that the application container 
is the same as the auth server container (a relative adapter test scenario).

Adapter tests are separated into submodules because different app containers require different configurations 
(installation of adapter libs, etc.).
Container entries of app servers are not present in the main `arquillian.xml` in the `base` module.
Each adapter submodule adds it's own entry before it runs the tests.

## SuiteContext and TestContext

These objects are injected into `AbstractKeycloakTest` class so they can be used everywhere.
They can be used to get information about the tested containers, and to store information that won't survive across test classes or test methods.
(Arquillian creates a new instance of test class for each test method run, so all data in the fields is always lost.)

## REST Testing

The `AbstractKeycloakTest` has an initialized instance of AdminClient. Subclasses can use it to access any REST subresources.

## UI Testing

### Page Objects

Page Objects are used by tests to access and operate on UI. 
They can be injected using annotation `@Page` provided by the *Arquillian Graphene* extension.

The base class for all page objects used throughout this Arquillian testsuite is `AbstractPage`, and it's subclass `AbstractPageWithInjectedUrl`.

For the page objects for the *adapter test apps* the URLs are injected automatically by Arquillian depending on actual URL of the deployed app/example.

For the pages under the `/auth` context the URL is only injected to the `AuthServerContextRoot` page object, 
and the URL hierarchy is modeled by the class inheritance hierarchy (subclasses/extending of `AuthServerContextRoot`).


### Browsers

The default browser for UI testing is `phantomjs` which is used for fast "headless" testing.
Other browsers can be selected with the `-Dbrowser` property, for example `firefox`.
See Arquillian Graphene documentation for more details.


## Test Modules

### Base Testsuite

The base testsuite contains custom Arquillian extensions and most functional tests.
The other test modules depend on this module.

### Admin Console UI Tests

Tests for Keycloak Admin Console are located in a separate module `tests/other/console` 
and are **disabled** by default. Can be enabled by `-Pconsole-ui-tests`.

### Adapter Tests

Adapter tests are located in submodules of the `tests/other/adapters` module.

They are **disabled** by default; they can be enabled by corresponding profiles.
Multiple profiles can be enabled for a single test execution.

#### Types of adapter tests

1. Using *custom test servlets*
2. Using *example demo apps* from `keycloak/examples` modules.

#### Relative vs Non-relative scenario

The test suite can handle both types.
It automatically modifies imported test realms and deployments' adapter configs based on scenario type.

| Scenario | Description | Realm config (server side) | Adapter config (client side) |
| --- | --- | --- | --- |
| **Relative** | auth server == app server | client `baseUrl`, `adminUrl` and `redirect-uris` can be relative | `auth-server-url` can be relative |
| **Non-relative** | auth server != app server  | client `baseUrl`, `adminUrl` and `redirect-uris` need to include FQDN of the app server | `auth-server-url` needs to include FQDN of the auth server|



#### Adapter Libs Mode

1. **Provided** - By container, e.g. as a subsystem. **Default.**
2. **Bundled** - In the deployed war in `/WEB-INF/libs`. Enable with `-Dadapter.libs.bundled`. *Wildfly only*.

#### Adapter Config Mode

1. ~~**Provided** - In `standalone.xml` using `secure-deployment`. *Wildfly only.*~~ WIP
2. **Bundled** - In the deployed war in `/WEB-INF/keycloak.json`. **Default.**


## Maven Modules Structure

```
integration-arquillian
│
├──servers
│  ├──wildfly   (activated by -Pauth-server-wildfly)
│  └──eap7      (activated by -Pauth-server-eap7)
│
└──tests   (common settings for all test modules)
   ├──base    (custom ARQ extensions + base functional tests)
   └──other   (common settings for all modules dependent on base)
      │
      ├──adapters         (common settings for all adapter submodules)
      │  ├──wildfly           (activated by -Papp-server-wildfly)
      │  ├──wildfly-relative  (activated by -Papp-server-wildfly-relative,auth-server-wildfly)
      │  ├──...
      │  ├──tomcat            (activated by -Papp-server-tomcat)
      │  └──karaf             (activated by -Papp-server-karaf)
      │
      ├──console          (activated by -Pconsole-ui-tests)
      ├──mod_auth_mellon  (activated by -Pmod_auth_mellon)
      ├──console_no_users (activated by -Pconsole-ui-no-users-tests)
      └──...
```

## Custom Arquillian Extensions

Custom extensions are registered in `META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension`.

### MultipleContainersExtension
 * Replaces Arquillian's default container handling.
 * Allows to manage multiple container instances of different types within a single test run.
 * Allows to skip loading disabled containers based on `enabled` config property in `arquillian.xml`.

### KeycloakArquillianExtension
 * `AuthServerTestEnricher` - Handles lifecycle of auth server and migrated auth server.
 * `AppServerTestEnricher` - Handles lifecycles of app servers.
 * `CustomUndertowContainer` - Custom container controller for JAX-RS-enabled Undertow with Keycloak Server.
 * `DeploymentArchiveProcessor` - Modifies adapter configs before test apps are deployed.
 * `DeploymentTargetModifier` - Ensures all test app deployments are targeted at app server containers.
 * `URLProvider` - Fixes URLs injected by Arquillian Graphene which contain value `127.0.0.1` instead of required `localhost`.
 
### CustomKarafContainerExtension

Extension for executing karaf commands after container is started. Used for installation of bundles (test apps and adapter libs).