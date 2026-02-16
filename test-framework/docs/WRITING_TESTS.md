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
    public RealmConfigBuilder configure(RealmConfigBuilder builder) {
        return builder
                .name("myrealm")
                .groups("group-a", "group-b");
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

### Injection support in config classes

`RealmConfig`, `ClientConfig` and `UserConfig` supports injecting dependencies into the config. This can be useful 
when the configuration depends on how other resources are configured. For example:

```java
public static class MyClient implements ClientConfig {

    @InjectDependency
    KeycloakUrls keycloakUrls;

    @Override
    public ClientConfigBuilder configure(ClientConfigBuilder client) {
        return client.redirectUris(keycloakUrls.getAdmin());
    }
}
```

Only dependencies (including transitive dependencies) defined by the suppliers can be injected into config classes. 

## Realm cleanup

The test framework aims to re-use as much as possible to reduce execution time. This is especially relevant to 
managed realms. By default, a managed realm has its lifecycle set to `CLASS`, which means the same realm will be
re-used for all tests methods within the same test class.

It's also possible to change the lifecycle to `GLOBAL` where the realm will be shared for all test classes. This can
be beneficial for large and complex realms, but bear in mind that tests will need to carefully clean after 
themselves.

In the end choosing the lifecycle of the realm depends on how much (if any) cleanup tests have to perform.

To help with cleanup `ManagedRealm` provides some convenience methods to help test clean-up after themselves. In general 
the above methods should be called at the start of the test method before any changes are made.

### `dirty()`

If a limited number of tests require a lot of cleanup it can be expensive to do so, and result in larger and more 
complex test methods. Marking the realm as dirty within a test method will cause it to be re-created after the test
method has executed:

```java
@Test
public void testSomething() {
    managedRealm.dirty();
    
    // Make loads of changes to the realm
}
```

If most or all test methods are using `dirty()` consider using lifecycle `CLASS` instead for the managed realm.

### `updateWithCleanup(...)`

If a limited number of test methods require changes to the realm configuration the `updateWithCleanup(...)` method
can be used:

```java
@Test
public void testSomethingThatRequiresRegistration() {
    managedRealm.updateWithCleanup(r -> r.registrationAllowed(true));
    
    // Test registration
}
```

The changes will then be reverted after the test method has executed.

### `update` and `add` methods

There are a number of utilities that allow adding or updating resources within a realm, with cleanup after the test 
method has executed. This allows for example adding a user that is only required for a single test method:

```java
@Test
public void testUser() {
    managedRealm.addUser(UserConfigBuilder.create().username("myuser"));
}
```

There are a limit number of supported resources at the moment, and more will be added as needed, eventually 
supporting the majority of resources within a realm.

### `cleanup().add(...)`

Adding cleanup to the realm will allow cleaning up anything within the realm:

```java
@Test
public void testWithCleanup() {
    managedRealm.cleanup().add(r -> r.roles().get("foo").remove());
}
```

## Setup and Cleanup

Typically, for a JUnit test `beforeAll` and `afterAll` are used to setup the environment for tests, but these are not
very useful when using the test framework since these need to be `static` and does not have access to any injected 
resources.

Instead, the test framework allows annotating `non-static` methods with no parameters using `@TestSetup` and 
`@TestCleanup`. Methods annotated with `@TestSetup` will be executed before all tests, and methods annotated with
`@TestCleanup` after all test methods have completed. For example:

```java
@InjectRealm(lifecycle = LifeCycle.CLASS)
ManagedRealm realm;

@TestSetup
public void setupRealms() {
    RealmRepresentation rep = realm.admin().toRepresentation();
    Assertions.assertNull(rep.getAttributes().get("test.setup"));
    rep.getAttributes().put("test.setup", "myvalue");
    realm.admin().update(rep);
}

@TestCleanup
public void cleanupRealms() {
    RealmRepresentation rep = realm.admin().toRepresentation();
    Assertions.assertEquals("myvalue", rep.getAttributes().get("test.setup"));
    rep.getAttributes().remove("test.setup");
    realm.admin().update(rep);
}
```

One thing to bear in mind when using `@TestSetup` and `@TestCleanup` is any injected resources with lifecycle `METHOD`.
As these will be re-created for each test method, any changes done in `@TestSetup` will to those resources will be
reverted after the first test has executed.

Avoid using `@TestSetup` for anything that can be configured using `config`.

## Multiple instances

By default, all resources are granted the default reference, and other resources that depend on them don't need to
explicitly reference a given instance. For example if there is a single realm and a single user:

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

The Keycloak admin client can be used to access the Keycloak Admin API to view the status of a realm and it's resources
as well as creating additional resources needed by tests:

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
    loginPage.fillLogin("myuser", "mypassword");
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

One thing to bear in mind when using `OAuthClient` is if a test changes the configuration (for example changing the
client using `client()`) it is not reset automatically for the next test. It may be better in this case to use
`lifecycle=method`, which will result in a new `OAuthClient` being created for each test method.

## Asserting events on the server

Admin and login events can be checked by using `@InjectAdminEvents AdminEvents adminEvents` or `@InjectEvents Events`.
This allows pulling events that have occurred in the duration of a test method.

`AssertAdminEvents` and `AssertEvents` provides a convenient way to assert the events. 

## Run-on-Server

The `keycloak-test-framework-remote` extensions installs a custom provider on the Keycloak server that enables executing
code on the Keycloak server.

This can be used to retrieve or assert values directly in the Keycloak server that is not available through REST APIs.
It can also be useful to execute methods directly on providers.

The example below shows retrieving a value from the server, and another test that retrieves a provider:
```java
@KeycloakIntegrationTest
public class RunOnServerTest {

    @InjectRunOnServer(permittedPackages = "org.keycloak.test.examples")
    RunOnServerClient runOnServer;

    @Test
    public void testFetchValuesFromTheServer() {
        String string = runOnServer.fetch(session -> "Hello world!", String.class);
        Assertions.assertEquals("Hello world!", string);
    }

    @Test
    public void testCallingProvider() {
        runOnServer.run(session -> {
            RealmResourceProvider myprovider = session.getProvider(RealmResourceProvider.class, "myprovider");
        });
    }
}
```

It is also possible to run a test method remotely:

```java
@KeycloakIntegrationTest
public class RunOnServerTest {

    @TestOnServer
    public void fromModel_mapsRoles(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        Assertions.assertNotNull(realm);
    }
}
```

## Managed resources

Complete list of resources that can be injected into tests:

| Annotation                  | Java class                                               | Description                                       |
|-----------------------------|----------------------------------------------------------|---------------------------------------------------|
| `@InjectAdminClient`        | `org.keycloak.admin.client.Keycloak`                     | Admin client                                      |
| `@InjectAdminClientFactory` | `org.keycloak.testframework.admin.AdminClientFactory`    | Factory to create admin clients                   |
| `@InjectAdminEvents`        | `org.keycloak.testframework.events.AdminEvents`          | Retrieve admin events                             |
| `@InjectClient`             | `org.keycloak.testframework.realm.ManagedClient`         | Managed clients                                   |
| `@InjectCryptoHelper`       | `org.keycloak.testframework.crypto.CryptoHelper`         | Crypto utilities                                  |
| `@InjectEvents`             | `org.keycloak.testframework.events.Events`               | Retrieve login events                             |
| `@InjectHttpClient`         | `org.apache.http.client.HttpClient`                      | Apache HTTP client                                |
| `@InjectHttpServer`         | `com.sun.net.httpserver.HttpServer`                      | Mock HTTP server                                  |
| `@InjectInfinispanServer`   | `org.keycloak.testframework.infinispan.InfinispanServer` | Infinispan server                                 |
| `@InjectKeycloakUrls`       | `org.keycloak.testframework.server.KeycloakUrls`         | Keycloak server URLs                              |
| `@InjectRealm`              | `org.keycloak.testframework.realm.ManagedRealm`          | Managed realm                                     |
| `@InjectSimpleHttp`         | `org.keycloak.http.simple.SimpleHttp`                    | Simple HTTP client                                |
| `@InjectSysLogServer`       | `org.keycloak.testframework.events.SysLogServer`         | Add/remove listener for logs from Keycloak server |
| `@InjectTestDatabase`       | `org.keycloak.testframework.database.TestDatabase`       | Database                                          |
| `@InjectUser`               | `org.keycloak.testframework.realm.ManagedUser`           | Managed user                                      |

## `keycloak-test-framework-email-server` extension

| Annotation          | Java class                                   | Description     |
|---------------------|----------------------------------------------|-----------------|
| `@InjectMailServer` | `org.keycloak.testframework.mail.MailServer` | Retrieve emails |

## `keycloak-test-framework-remote` extension

| Annotation           | Java class                                                        | Description                                    |
|----------------------|-------------------------------------------------------------------|------------------------------------------------|
| `@InjectRunOnServer` | `org.keycloak.testframework.remote.runonserver.RunOnServerClient` | Execute code on the Keycloak server            |
| `@InjectTimeOffSet`  | `org.keycloak.testframework.remote.timeoffset.TimeOffSet`         | Set the timeoffset used on the Keycloak server |

## `keycloak-test-framework-oauth` extension

| Annotation                     | Java class                                               | Description                  |
|--------------------------------|----------------------------------------------------------|------------------------------|
| `@InjectOAuthClient`           | `org.keycloak.testframework.oauth.OAuthClient`           | OAuth client                 |
| `@InjectOAuthIdentityProvider` | `org.keycloak.testframework.oauth.OAuthIdentityProvider` | Mock OAuth identity provider |
| `@InjectTestApp`               | `org.keycloak.testframework.oauth.TestApp`               | Mock OAuth client            |

## `keycloak-test-framework-ui` extension

| Annotation         | Java class                                                 | Description                                   |
|--------------------|------------------------------------------------------------|-----------------------------------------------|
| `@InjectPage`      | `org.keycloak.testframework.ui.page.AbstractPage`          | Inject pages extending `AbstractPage`         |
| `@InjectWebDriver` | `org.keycloak.testframework.ui.webdriver.ManagedWebDriver` | Utilities for interracting with the WebDriver |
