# Introduction

The Keycloak JUnit 5 test framework makes it easy to write tests for Keycloak and extensions. Behind the scenes the
framework handles the lifecycle of Keycloak, the database, and any injected resources such as realms and clients.

Tests simply declare what they want, including specific configuration, and the framework takes care of the rest. 


# Writing tests

An example is better than a lot of words, so here is a very basic test:

```java
@KeycloakIntegrationTest
public class BasicTest {

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void test() {
        Assertions.assertEquals("default", realm.getName());
        Assertions.assertEquals(0, realm.admin().users().list().size());
    }

}
```

## Resource lifecycle

Managed resources can have the following life-cycles:

* Global - Shared across multiple test classes
* Class - Shared across multiple test methods within the same test class
* Method - Only used for a single test method

The framework handles the lifecycle accordingly to how it is configured in the annotation, or the default lifecycle
for a given resource.

For example the default lifecycle for a realm is Class, but it can be changed through the annotation:

```java
@InjectRealm(lifecycle = LifeCycle.METHOD)
ManagedRealm realm;

@Test
public void test() {
    realm.admin().users().create(...);
}

@Test
public void test2() {
    Assertions.assertEquals(0, realm.admin().users().list().size());
}
```

When the lifecycle is set to Method the realm is automatically destroyed and re-created for each test method, as seen in 
the above example where one test method adds a user to the realm, but the user is not present in the next test.

The general recommendation is to use the Class lifecycle for realms, clients, and users. Making sure that individual test
methods leave the resource in a way that can be re-used. Realms for example with global lifecycle can be harder to
maintain as individual test classes can break other tests, but at the same time using global resources can be useful 
as it will be more performant.

## Configuring resources

Resources are configured by declaring the required configuration through a Java class. This Java class can be an inner-class
if it's only used for a single test class, or can be a proper class when multiple tests share the same configuration.

For example to create a realm with a specific configuration:

```java
@InjectRealm(config = MyRealmConfig.class)
ManagedRealm realm;

static class MyRealmConfig implements RealmConfig {

    @Override
    public RealmRepresentation getRepresentation() {
        return builder()
                .name("myrealm")
                .groups("group-a", "group-b")
                .build();
    }
}
```

The framework will automatically re-create global resources if they don't match the required configuration. For example:

```java
@KeycloakIntegrationTest
public class Test1 {

    @InjectRealm(lifecycle = LifeCycle.GLOBAL, config = MyRealmConfig.class)
    ManagedRealm realm;

}

@KeycloakIntegrationTest
public class Test2 {

    @InjectRealm(lifecycle = LifeCycle.GLOBAL, config = MyOtherRealm.class)
    ManagedRealm realm;

}
```

In this example the realm from `Test1` would be destroyed and a new realm created for `Test2` since different 
configuration is requested.

## Multiple instances

By default, a resource does not have a reference, and child-resources are created within parent the resource without a 
reference. For example in the following example `userA` will be created within `realmA`:

```java
@InjectRealm
ManagedRealm realmA;

@InjectUser
ManagedUser userA;
```

If you need for instance multiple realms within a test you need to set a reference on it, and use this reference for
child resources:

```java
@InjectRealm
ManagedRealm realmA;

@InjectUser
ManagedUser userA;

@InjectRealm(ref = "realmB")
ManagedRealm realmB;

@InjectUser(realmRef = "realmB")
ManagedUser userB;
```

As with resources without a reference if a resource is re-used in another test class compatibility will be checked. 
For example:

```java
@KeycloakIntegrationTest
public class Test1 {
    @InjectRealm(lifecycle = LifeCycle.GLOBAL, ref = "realmA")
    ManagedRealm realmA;

    @InjectRealm(lifecycle = LifeCycle.GLOBAL, ref="realmB", config = MyRealmConfig.class)
    ManagedRealm realmB;
}

@KeycloakIntegrationTest
public class Test2 {
    @InjectRealm(lifecycle = LifeCycle.GLOBAL, ref = "realmA")
    ManagedRealm realmA;
    
    @InjectRealm(lifecycle = LifeCycle.GLOBAL, ref="realmB", config = MyOtherRealm.class)
    ManagedRealm realmB;
}
```

In the above example `realmA` will be reused both for `Test1` and `Test2`, while `realmB` will be re-created between the 
two test classes since the required configuration differs. 

## Using the Keycloak admin client

The Keycloak admin client can be injected directly, which is automatically connected to the test server:

```java
@InjectAdminClient
org.keycloak.admin.client.Keycloak keycloak;

@Test
public void testAdminClient() {
    keycloak.realms().findAll();
}
```

It is also available directly for a managed resource:

```java
@InjectRealm
ManagedRealm realm;

@Test
public void testRealmAdmin() {
    realm.admin().users().list();
}
```

## Using Selenium

Frequently when testing Keycloak it is required to interact with login pages, required actions, etc. through the 
browser. This can be done in two ways, where the most convenient way is to inject a Java Page representation:

```java
@InjectPage
LoginPage loginPage;

@Test
public void testLogin() {
    // Do something to open the login page
    loginPage.fillLogin(..);
    loginPage.submit();
}
```

An alternative approach is to inject the `WebDriver` directly:

```java
@InjectWebDriver
WebDriver webDriver;

@Test
public void test() {
    webDriver.switchTo().newWindow(WindowType.TAB);
}
```


## OAuth Client

A convenient way to test OAuth flows are with the OAuth Client. This provides convenient methods to perform different
OAuth flows, and it even automatically creates its own client within the realm. For example:

```java
@InjectOAuthClient
OAuthClient oAuthClient;

@Test
public void testClientCredentials() throws Exception {
    TokenResponse tokenResponse = oAuthClient.clientCredentialGrant();
    Assertions.assertTrue(tokenResponse.indicatesSuccess());
    Assertions.assertNotNull(tokenResponse.toSuccessResponse().getTokens().getAccessToken());
}
```


# Test Suites

A `@Suite` can supply configuration to be used when running tests from the suite. For example:

```java
@Suite
@SelectClasses(MyTest.class)
public class MyTestSuite {

    @BeforeSuite
    public static void beforeSuite() {
        SuiteSupport.startSuite()
                .registerServerConfig(MyTestSuiteServerConfig.class)
                .includedSuppliers("server", "remote");
    }

    @AfterSuite
    public static void afterSuite() {
        SuiteSupport.stopSuite();
    }
}
```

The above example adds some additional Keycloak server configuration, as well as limiting what server suppliers can be used for the suite.


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
