# Quick Reference — Most Common Migration Patterns

One-page cheat sheet covering ~80% of migrations. For edge cases, see the full rule file linked in each section.

---

## 1. Class Structure

```java
// OLD
@RunWith(KcArquillian.class)
public class MyTest extends AbstractTestRealmKeycloakTest {

// NEW
@KeycloakIntegrationTest
public class MyTest {
    @InjectRealm(config = MyRealmConfig.class)
    ManagedRealm realm;
```

Remove: `@RunWith`, `@RunAsClient`, `extends AbstractKeycloakTest/AbstractTestRealmKeycloakTest`
→ Full details: `rules/core.md` §Base Class Resolution

---

## 2. JUnit 4 → 6

| Old | New |
|-----|-----|
| `import org.junit.Test` | `import org.junit.jupiter.api.Test` |
| `@Before` / `@After` | `@BeforeEach` / `@AfterEach` |
| `@BeforeClass` / `@AfterClass` | `@BeforeAll` / `@AfterAll` |
| `Assert.assertEquals(msg, expected, actual)` | `assertEquals(expected, actual, msg)` — message moves LAST |
| `@Ignore("reason")` | `@Disabled("reason")` |
| `@Test(expected = X.class)` | `assertThrows(X.class, () -> { ... })` |
| `public void testMethod()` | `void testMethod()` — remove `public` |
| `@FixMethodOrder(NAME_ASCENDING)` | `@TestMethodOrder(OrderAnnotation.class)` + `@Order(n)` |

---

## 3. Injection Replacements

| Old | New | Import |
|-----|-----|--------|
| `@ArquillianResource OAuthClient oauth` | `@InjectOAuthClient OAuthClient oauthClient` | `o.k.testframework.oauth.annotations.InjectOAuthClient` |
| `@Drone WebDriver driver` | `@InjectWebDriver ManagedWebDriver webDriver` | `o.k.testframework.ui.annotations.InjectWebDriver` |
| `@Page LoginPage loginPage` | `@InjectPage LoginPage loginPage` | `o.k.testframework.ui.annotations.InjectPage` |
| `@Rule AssertEvents events` | `@InjectEvents Events events` | `o.k.testframework.annotations.InjectEvents` |
| `@Rule AssertAdminEvents` | `@InjectAdminEvents AdminEvents adminEvents` | `o.k.testframework.annotations.InjectAdminEvents` |
| `@Rule GreenMailRule` | `@InjectMailServer MailServer mailServer` | `o.k.testframework.mail.annotations.InjectMailServer` |
| `adminClient` (inherited) | `@InjectAdminClient Keycloak adminClient` | `o.k.testframework.annotations.InjectAdminClient` |
| `testingClient.testing().setTimeOffset()` | `@InjectTimeOffSet TimeOffSet timeOffSet` → `timeOffSet.set(n)` | `o.k.testframework.remote.timeoffset.*` |
| `testingClient.server().run(session -> ...)` | `@InjectRunOnServer RunOnServerClient` → `runOnServer.run(session -> ...)` | `o.k.testframework.remote.runonserver.*` |
| `suiteContext.getAuthServerInfo().getContextRoot()` | `@InjectKeycloakUrls KeycloakUrls keycloakUrls` | `o.k.testframework.annotations.InjectKeycloakUrls` |

---

## 4. Admin Client Access

```java
// OLD
adminClient.realm("test").users().list()

// NEW
realm.admin().users().list()
```

---

## 5. Realm Configuration

```java
// OLD: addTestRealms() / configureTestRealm()
// NEW: RealmConfig class with only what the test needs

@InjectRealm(config = MyRealmConfig.class)
ManagedRealm realm;

public static class MyRealmConfig implements RealmConfig {
    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        realm.addUser("testuser").password("password").roles("admin");
        realm.addClient("my-client").secret("secret").redirectUris("*");
        realm.roles("admin");
        return realm;
    }
}
```

→ Full details: `rules/core.md` §Realm Configuration

---

## 6. Builder Replacements

| Old | New |
|-----|-----|
| `RealmBuilder` | `RealmConfigBuilder` |
| `ClientBuilder` | `ClientConfigBuilder` |
| `UserBuilder` | `UserConfigBuilder` |
| `RoleBuilder` | `RoleConfigBuilder` |
| `GroupBuilder` | `GroupConfigBuilder` |

**Chaining rule**: `realm.addUser("x").password("p");` — do NOT call `.build()` to chain back. Use separate statements.

---

## 7. Events

```java
// OLD
events.expectLogin().user(userId).session(sessionId).assertEvent();

// NEW
EventAssertion.assertSuccess(events.poll())
    .type(EventType.LOGIN)
    .userId(userId)
    .sessionId(sessionId);
```

→ Full details: `rules/events.md`

---

## 8. OAuthClient

```java
// do*() → returns response directly (NO .send())
AccessTokenResponse response = oauthClient.doAccessTokenRequest(code);

// Builder → returns request, call .send()
AccessTokenResponse response = oauthClient.accessTokenRequest(code).header("X-Custom", "v").send();
```

- Field rename: `oauth` → `oauthClient`
- Remove: `oauth.init()`, `oauth.setDriver()`, `oauth.clientId()` → `oauthClient.client()`
- Multiple clients: `@InjectOAuthClient(ref = "second")` — NOT `oauth.newConfig()`
→ Full details: `rules/oauth.md`

---

## 9. Login Page Pattern

```java
// OLD
loginPage.login("user", "password");

// NEW — fill + submit separately
loginPage.fillLogin("user", "password");
loginPage.submit();
```

Page navigation: `loginPage.open()` → `oauthClient.openLoginForm()`
Page assertion: `assertTrue(page.isCurrent())` → `page.assertCurrent()`
→ Full details: `rules/webdriver.md`

---

## 10. Server Features

```java
// OLD: @EnableFeature(Feature.AUTHORIZATION)
// NEW:
@KeycloakIntegrationTest(config = MyTest.ServerConfig.class)
public class MyTest {
    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.AUTHORIZATION);
        }
    }
}
```

→ Full details: `rules/server-and-registration.md`

---

## 11. Cleanup

```java
// OLD: getCleanup().addCleanup(() -> adminClient.realm("test").clients().get(id).remove());
// NEW:
realm.cleanup().add(r -> r.clients().get(id).remove());

// OLD: manual modify + reset in @After
// NEW:
realm.updateWithCleanup(r -> r.defaultSignatureAlgorithm("ES256"));
// Auto-resets after test
```

→ Full details: `rules/lifecycle.md`

---

## 12. Utility Classes

| Old | New |
|-----|-----|
| `o.k.testsuite.Assert` | `o.k.tests.utils.Assert` |
| `o.k.testsuite.admin.ApiUtil` | `o.k.tests.utils.admin.AdminApiUtil` (or `o.k.testframework.util.ApiUtil` for `getCreatedId()` only) |
| `SimpleHttpDefault.doGet(url, httpClient)` | `@InjectSimpleHttp SimpleHttp simpleHttp` → `simpleHttp.doGet(url)` |
| `TokenUtil.getToken()` | `oauthClient.client("direct-grant", "password").doPasswordGrantRequest(u, p).getAccessToken()` |
| `AccountHelper` / `FlowUtil` | Same import (in `utils-shared`) |

→ Full details: `rules/utilities.md`

---

## 13. Package Rename

`org.keycloak.testsuite.*` → `org.keycloak.tests.*` (except `utils-shared` classes which keep `org.keycloak.testsuite.util`)

---

## Decision Quick-Checks

**User needs roles?** → Use `RealmConfigBuilder.addUser()`, NOT `@InjectUser`

**Test uses `testrealm.json`?** → Prefer minimal `RealmConfig` unless test needs many users/OTP/complex setup

**OAuthClient + fromJson with `test-app` client?** → Add `ref` to `@InjectOAuthClient` to avoid 409

**Need second browser?** → `@InjectWebDriver(ref = "second")` + `@InjectOAuthClient(webDriverRef = "second", ref = "second")`

**Need multiple admin clients?** → `@InjectAdminClientFactory AdminClientFactory` → `.create().realm().username().password().build()`
