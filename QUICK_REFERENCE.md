# Quick Reference: Old vs New Test Framework Patterns

## Class-Level Changes

| Aspect                   | Old Framework                                       | New Framework                                        |
| ------------------------ | --------------------------------------------------- | ---------------------------------------------------- |
| **Class Declaration**    | `public class Test extends AbstractRestServiceTest` | `@KeycloakIntegrationTest public class Test`         |
| **Base Class**           | AbstractTestRealmKeycloakTest                       | None (annotation-based)                              |
| **JUnit Version**        | JUnit 4 (@Rule, @Before, @After)                    | JUnit 5 (@Test, @TestSetup)                          |
| **Dependency Injection** | @Rule annotations                                   | @Inject\* annotations                                |
| **HTTP Client**          | `protected CloseableHttpClient httpClient`          | `@InjectSimpleHttp SimpleHttp simpleHttp`            |
| **OAuth Token**          | `@Rule TokenUtil tokenUtil`                         | `@InjectOAuthClient OAuthClient oAuthClient`         |
| **Realm Access**         | Inherited `managedRealm`                            | `@InjectRealm(config=...) ManagedRealm managedRealm` |
| **User Access**          | Created in configureTestRealm                       | `@InjectUser(config=...) ManagedUser testUser`       |

## Method-Level Changes

| Task                | Old Pattern                                                         | New Pattern                                                         |
| ------------------- | ------------------------------------------------------------------- | ------------------------------------------------------------------- |
| **Setup**           | `@Before public void setup()`                                       | `@TestSetup public void setup()`                                    |
| **Teardown**        | `@After public void cleanup()`                                      | Auto via @TestSetup and resource lifecycle                          |
| **Configure Realm** | `@Override public void configureTestRealm(RealmRepresentation rep)` | `implements RealmConfig` with `RealmBuilder.configure()`            |
| **Configure User**  | Create in configureTestRealm                                        | `implements UserConfig` with `UserBuilder.configure()`              |
| **Get Token**       | `tokenUtil.getToken()`                                              | `oAuthClient.doPasswordGrantRequest(user, pwd).getAccessToken()`    |
| **POST Request**    | `SimpleHttpDefault.doPost(url, httpClient).auth(token)`             | `simpleHttp.doPost(url).header("Authorization", "Bearer " + token)` |
| **GET Request**     | `SimpleHttpDefault.doGet(url, httpClient).auth(token)`              | `simpleHttp.doGet(url).header("Authorization", "Bearer " + token)`  |
| **Add Cleanup**     | `getCleanup().addCleanup(() -> {...})`                              | `managedRealm.cleanup().add(() -> {...})`                           |

## Import Statement Mapping

### Old Imports → New Imports

```
org.keycloak.testsuite.AbstractTestRealmKeycloakTest → (removed, use @KeycloakIntegrationTest)
org.keycloak.testsuite.AssertEvents → (removed, use @InjectEvents if needed)
org.keycloak.testsuite.util.TokenUtil → org.keycloak.testframework.oauth.OAuthClient
org.keycloak.testsuite.broker.util.SimpleHttpDefault → org.keycloak.http.simple.SimpleHttp
org.junit.Before → org.keycloak.testframework.annotations.TestSetup
org.junit.After → (removed, lifecycle managed automatically)
org.junit.Rule → org.keycloak.testframework.annotations.*

NEW CORE IMPORTS:
org.keycloak.testframework.annotations.KeycloakIntegrationTest
org.keycloak.testframework.annotations.InjectRealm
org.keycloak.testframework.annotations.InjectUser
org.keycloak.testframework.annotations.InjectSimpleHttp
org.keycloak.testframework.annotations.InjectOAuthClient
org.keycloak.testframework.realm.RealmConfig
org.keycloak.testframework.realm.UserConfig
org.junit.jupiter.api.Test (JUnit 5)
```

## Common Code Patterns

### Pattern 1: Getting Test User's Access Token

**OLD:**

```java
String token = tokenUtil.getToken();
```

**NEW:**

```java
@TestSetup
public void setup() {
    accessToken = oAuthClient.doPasswordGrantRequest(
        testUser.getUsername(),
        testUser.getPassword()
    ).getAccessToken();
}

// Then use in tests:
String token = accessToken;
```

### Pattern 2: Making HTTP Requests with Auth

**OLD:**

```java
UserRepresentation user = SimpleHttpDefault.doGet(url, httpClient)
    .auth(tokenUtil.getToken())
    .asJson(UserRepresentation.class);
```

**NEW:**

```java
UserRepresentation user = simpleHttp.doGet(url)
    .header("Authorization", "Bearer " + accessToken)
    .asJson(UserRepresentation.class);
```

### Pattern 3: Configuring Test Realm

**OLD:**

```java
@Override
public void configureTestRealm(RealmRepresentation testRealm) {
    testRealm.setDisplayName("My Realm");
    testRealm.getUsers().add(
        UserBuilder.create().username("user1").password("password").build()
    );
}
```

**NEW:**

```java
public static class MyRealmConfig implements RealmConfig {
    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        return realm.displayName("My Realm")
                    .user(UserBuilder.create()
                        .username("user1")
                        .password("password"));
    }
}

// In test class:
@InjectRealm(config = MyRealmConfig.class)
ManagedRealm realm;
```

### Pattern 4: Dynamic Cleanup During Test

**OLD:**

```java
@Test
public void testSomething() {
    SomeResource resource = createResource();
    getCleanup().addCleanup(() -> resource.delete());
    // Test code...
}
```

**NEW:**

```java
@Test
public void testSomething() {
    SomeResource resource = createResource();
    managedRealm.cleanup().add(() -> resource.delete());
    // Test code...
}
```

### Pattern 5: Assertion Framework

**OLD:**

```java
@Rule
public AssertEvents events = new AssertEvents(this);

// Usage:
events.expect(EventType.LOGIN).user(userId).assertEvent();
```

**NEW:**

```java
@InjectEvents
TestEvents events;

// Usage: (same API)
events.expect(EventType.LOGIN).user(userId).assertEvent();
```

## Dependency Injection Annotations Reference

| Annotation                 | Type   | Purpose                 | Old Equivalent                        |
| -------------------------- | ------ | ----------------------- | ------------------------------------- |
| `@KeycloakIntegrationTest` | Class  | Enable test framework   | extends AbstractTestRealmKeycloakTest |
| `@InjectRealm`             | Field  | Inject managed realm    | inherited managedRealm                |
| `@InjectUser`              | Field  | Inject managed user     | created in configureTestRealm         |
| `@InjectSimpleHttp`        | Field  | Inject HTTP client      | httpClient field + SimpleHttpDefault  |
| `@InjectOAuthClient`       | Field  | Inject OAuth client     | @Rule TokenUtil                       |
| `@InjectEvents`            | Field  | Inject event checker    | @Rule AssertEvents                    |
| `@InjectAdminClient`       | Field  | Inject admin API client | admin() method                        |
| `@InjectKeycloakUrls`      | Field  | Get server URLs         | getAuthServerContextRoot()            |
| `@TestSetup`               | Method | Setup before tests      | @Before                               |
| `@TestCleanup`             | Method | Cleanup after tests     | @After                                |

## Configuration Interfaces Reference

| Interface              | Purpose               | Method                                 | Returns                     |
| ---------------------- | --------------------- | -------------------------------------- | --------------------------- |
| `RealmConfig`          | Configure test realm  | configure(RealmBuilder)                | RealmBuilder                |
| `UserConfig`           | Configure test user   | configure(UserBuilder)                 | UserBuilder                 |
| `ClientConfig`         | Configure test client | configure(ClientBuilder)               | ClientBuilder               |
| `KeycloakServerConfig` | Configure server      | configure(KeycloakServerConfigBuilder) | KeycloakServerConfigBuilder |

## Common Migration Checklist

- [ ] Change class annotation to @KeycloakIntegrationTest
- [ ] Remove `extends AbstractRestServiceTest` (or parent class)
- [ ] Replace `@Rule TokenUtil` with `@InjectOAuthClient OAuthClient`
- [ ] Replace `@Rule AssertEvents` with `@InjectEvents TestEvents`
- [ ] Replace `protected CloseableHttpClient` with `@InjectSimpleHttp SimpleHttp`
- [ ] Replace `protected managedRealm` with `@InjectRealm ManagedRealm`
- [ ] Create realm configuration class implementing RealmConfig
- [ ] Create user configuration class implementing UserConfig
- [ ] Move realm setup from configureTestRealm() to RealmConfig
- [ ] Move user creation to UserConfig
- [ ] Change `@Before` to `@TestSetup`
- [ ] Change `@After` to auto-cleanup (or @TestCleanup if needed)
- [ ] Update HTTP request code: SimpleHttpDefault → SimpleHttp
- [ ] Update auth headers: .auth(token) → .header("Authorization", "Bearer " + token)
- [ ] Update token retrieval: tokenUtil.getToken() → oAuthClient.doPasswordGrantRequest(...)
- [ ] Update cleanup: getCleanup().addCleanup() → resource.cleanup().add()
- [ ] Update imports to use testframework packages
- [ ] Change @Test imports to JUnit 5
- [ ] Run tests to verify migration
- [ ] Check for any @Rule references and replace with @Inject\*

## Troubleshooting

| Issue                                              | Cause                    | Solution                                            |
| -------------------------------------------------- | ------------------------ | --------------------------------------------------- |
| "Cannot find symbol: class ManagedRealm"           | Wrong import             | Use `org.keycloak.testframework.realm.ManagedRealm` |
| "OAuthClient has no method doPasswordGrantRequest" | Wrong OAuthClient        | Use `org.keycloak.testframework.oauth.OAuthClient`  |
| "Cannot resolve symbol: @InjectRealm"              | Annotation not available | Check testframework module dependency               |
| "SimpleHttp.auth() not found"                      | Wrong SimpleHttp         | Use `org.keycloak.http.simple.SimpleHttp`           |
| "Tests fail with 'getCleanup() undefined'"         | Still using old cleanup  | Use `managedRealm.cleanup().add()` instead          |
| "configureTestRealm() not called"                  | Not using RealmConfig    | Implement RealmConfig interface                     |
