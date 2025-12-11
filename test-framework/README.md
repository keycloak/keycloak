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

For more information on how to run tests, see the [How to run tests](HOW_TO_RUN.md) guide.