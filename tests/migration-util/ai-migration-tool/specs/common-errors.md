# Common Migration Errors & Fixes

Errors encountered during actual migrations, organized by when they appear. Check this file BEFORE debugging — the answer is likely here.

---

## Compilation Errors

### `cannot find symbol: class AuthzClient` (or other Keycloak class)
**Cause**: The Keycloak module isn't a dependency of the test module.
**Fix**: Add the dependency to `tests/base/pom.xml` with `<scope>test</scope>`. Find the module: `find . -name "AuthzClient.java" -path "*/main/*" -not -path "*/target/*"`, then check its `pom.xml` for coordinates.
**Don't**: Rewrite working code to avoid the import.

### `cannot find symbol: method setXxx()` on `RealmConfigBuilder`
**Cause**: `RealmConfigBuilder` uses builder method names, NOT `RealmRepresentation` setter names.
**Fix**: Check `rules/core.md` §RealmConfigBuilder Method Reference. Example: `setDefaultSignatureAlgorithm()` → `defaultSignatureAlgorithm()`.
**Source**: `test-framework/core/src/main/java/org/keycloak/testframework/realm/RealmConfigBuilder.java`

### `package org.keycloak.testframework.annotations does not exist` for `@InjectOAuthClient`
**Cause**: Submodule annotations are NOT in `o.k.testframework.annotations.*`.
**Fix**: Use the module-specific package:
- `@InjectOAuthClient` → `org.keycloak.testframework.oauth.annotations.InjectOAuthClient`
- `@InjectWebDriver` → `org.keycloak.testframework.ui.annotations.InjectWebDriver`
- `@InjectPage` → `org.keycloak.testframework.ui.annotations.InjectPage`
- `@InjectMailServer` → `org.keycloak.testframework.mail.annotations.InjectMailServer`
- `@InjectTimeOffSet` → `org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet`
- `@InjectRunOnServer` → `org.keycloak.testframework.remote.runonserver.InjectRunOnServer`

### `cannot find symbol: class OAuthClient` (wrong import)
**Cause**: Old import `org.keycloak.testsuite.util.oauth.OAuthClient` still present.
**Fix**: `org.keycloak.testframework.oauth.OAuthClient`

### `cannot find symbol: class Assert` (Keycloak Assert, not JUnit)
**Cause**: Old import `org.keycloak.testsuite.Assert`.
**Fix**: `org.keycloak.tests.utils.Assert`

### `cannot find symbol: class ApiUtil`
**Cause**: Old import `org.keycloak.testsuite.admin.ApiUtil`.
**Fix**: For `getCreatedId()` only → `org.keycloak.testframework.util.ApiUtil`. For full API → `org.keycloak.tests.utils.admin.AdminApiUtil`.

### `incompatible types: UserRepresentation cannot be converted to String` in `.userId()`
**Cause**: `EventAssertion.userId()` only accepts `String`, not `UserRepresentation`.
**Fix**: Change `.user(userRep)` → `.userId(userRep.getId())`

### `method build() cannot return RealmConfigBuilder` in chained addUser/addClient
**Cause**: `.build()` returns the raw representation (e.g., `UserRepresentation`), NOT back to `RealmConfigBuilder`.
**Fix**: Use separate statements: `realm.addUser("a").password("p"); realm.addClient("b").secret("s"); return realm;`

### Compilation succeeds with `./mvnw compile` but test class has errors
**Cause**: Used `compile` instead of `test-compile`. Test classes are in `src/test/java/`.
**Fix**: Always use `./mvnw test-compile -pl tests/base -DskipTests`

---

## Runtime Errors

### `409 Conflict` when creating OAuth client
**Cause**: `@InjectOAuthClient` creates a `test-app` client, but the realm (loaded from JSON or RealmConfig) already has one.
**Fix**: Add `ref` to `@InjectOAuthClient`: `@InjectOAuthClient(ref = "browser")` → creates `test-app-browser`.
**Don't**: Use custom `ClientConfig` to change the clientId — it breaks the TestApp redirect URI wiring.

### `UnsupportedOperationException: Creating user with roles or client roles is not supported!`
**Cause**: Called `.roles()` or `.clientRoles()` on a `UserConfigBuilder` used with `@InjectUser`.
**Fix**: Use `RealmConfigBuilder.addUser()` instead — realm import supports roles. Or use `realm.addUser(UserConfigBuilder.create().username("u").roles("r"))` for dynamic users.

### `page 'null'` / `AssertionError: Expected page-id 'login-login' but got 'null'`
**Cause**: Browser state from a previous test is carrying over. The browser is not on a login page.
**Fix**: Add `@BeforeEach void setup() { webDriver.cookies().deleteAll(); }`. If tests modify browser flows, also add `realm.dirty()`.

### Login error message not found (empty string or wrong element)
**Cause**: In the current Keycloak theme, "Invalid username or password" is shown as an input-level error (`#input-error-username`), not a page-level alert.
**Fix**: Use `loginPage.getUsernameInputError()` instead of `loginPage.getError()` for credential errors.

### `NoSuchElementException` on page element after navigation
**Cause**: Page hasn't finished loading or redirecting.
**Fix**: Use `page.assertCurrent()` before interacting with elements — it waits for the page to load.

### Test passes locally but fails in CI with Chrome
**Cause**: Chrome rejects weak passwords (like "password") and shows a password strength warning that blocks the login flow.
**Fix**: Use strong passwords: `"P" + UUID.randomUUID().toString().substring(0, 12) + "!aA1"`. See `specs/base-class-flattening.md` §AbstractChangeImportedUserPasswordsTest.

### `realm.admin()` returns stale state after `updateWithCleanup()`
**Cause**: `updateWithCleanup()` sends the update to the server but doesn't refresh local state.
**Fix**: Re-fetch if needed: `realm.admin().toRepresentation()`. The auto-reset after test works correctly.

### `FlowUtil.inCurrentRealm(session)` fails with NPE
**Cause**: `RunOnServerClient` doesn't automatically bind to the test's managed realm.
**Fix**: Use `@InjectRunOnServer(realmRef = "myRealm")` if the realm has a ref, or pass the realm name explicitly.

---

## Anti-Patterns (Things That Look Right But Are Wrong)

### Custom `ClientConfig` to change OAuthClient's clientId
```java
// WRONG — breaks TestApp redirect URI connection
@InjectOAuthClient(config = MyConfig.class)
OAuthClient oauthClient;
public static class MyConfig implements ClientConfig {
    public ClientConfigBuilder configure(ClientConfigBuilder c) {
        return c.clientId("custom-id"); // DON'T
    }
}
```
**Use instead**: `@InjectOAuthClient(ref = "custom")` → creates `test-app-custom`

### Calling `.send()` on `do*()` methods
```java
// WRONG — doAccessTokenRequest already returns the response
oauthClient.doAccessTokenRequest(code).send();  // .send() doesn't exist on AccessTokenResponse!
```
**Use instead**: `oauthClient.doAccessTokenRequest(code)` — returns `AccessTokenResponse` directly.

### `loginPage.login("user", "pass")` on new framework
```java
// WRONG — login() doesn't exist on new LoginPage
loginPage.login("user", "password");
```
**Use instead**: `loginPage.fillLogin("user", "password"); loginPage.submit();`

### Loading `testrealm.json` when test only uses 1-2 resources
```java
// WRONG — loads 10 users, 9 clients, complex role hierarchy for a test that needs one user
@InjectRealm(fromJson = "/org/keycloak/tests/testrealm.json")
```
**Use instead**: Minimal `RealmConfig` with only what the test needs.

### Porting `TokenUtil` as a utility class
```java
// WRONG — TokenUtil is tightly coupled to legacy infrastructure
private TokenUtil tokenUtil = new TokenUtil("user", "pass");
String token = tokenUtil.getToken();
```
**Use instead**: `oauthClient.client("direct-grant", "password").doPasswordGrantRequest("user", "pass").getAccessToken()`

### Manual `HttpClient` creation for `SimpleHttp` calls
```java
// WRONG — manual lifecycle management
CloseableHttpClient client = HttpClientBuilder.create().build();
SimpleHttpDefault.doGet(url, client).asJson(Foo.class);
client.close();
```
**Use instead**: `@InjectSimpleHttp SimpleHttp simpleHttp` → `simpleHttp.doGet(url).asJson(Foo.class)`

### `@InjectUser` with roles
```java
// WRONG — throws UnsupportedOperationException at runtime
@InjectUser(config = RoledUserConfig.class) ManagedUser user;
public static class RoledUserConfig implements UserConfig {
    public UserConfigBuilder configure(UserConfigBuilder u) {
        return u.username("admin").roles("admin");  // BOOM
    }
}
```
**Use instead**: `RealmConfigBuilder.addUser("admin").password("pass").roles("admin")` in a `RealmConfig`

### Forgetting `test-app` → `direct-grant` when replacing TokenUtil
```java
// WRONG — test-app is confidential and doesn't have direct access grants
oauthClient.doPasswordGrantRequest("user", "pass");  // fails if oauthClient uses test-app
```
**Use instead**: `oauthClient.client("direct-grant", "password").doPasswordGrantRequest("user", "pass")` — switch to the direct-grant client first.
