# Utility Class Migration Rules

Read this file when the legacy test imports utility classes from `org.keycloak.testsuite.*`.

---

## Quick Reference: Utility Class Mapping

| Legacy Class | New Import | Status |
|---|---|---|
| `org.keycloak.testsuite.broker.util.SimpleHttpDefault` | `@InjectSimpleHttp` or `@InjectHttpClient` | **Replaced** — see SimpleHttpDefault section below |
| `org.keycloak.testsuite.util.TokenUtil` | `OAuthClient.doPasswordGrantRequest()` | **Replaced** — use `@InjectOAuthClient` for token acquisition |
| `org.keycloak.testsuite.util.AccountHelper` | `org.keycloak.testsuite.util.AccountHelper` | **Same** (in utils-shared) |
| `org.keycloak.testsuite.util.FlowUtil` | `org.keycloak.testsuite.util.FlowUtil` | **Same** (in utils-shared) |
| `org.keycloak.testsuite.Assert` | `org.keycloak.tests.utils.Assert` | **Moved** |
| `org.keycloak.testsuite.admin.ApiUtil` | `org.keycloak.tests.utils.admin.AdminApiUtil` | **Renamed + moved** |
| `org.keycloak.testframework.util.ApiUtil` | `org.keycloak.testframework.util.ApiUtil` | **Framework** (only `getCreatedId()`) |
| `org.keycloak.testsuite.util.AdminClientUtil` | — | **Obsolete** → `@InjectAdminClient` |
| `org.keycloak.testsuite.util.ClientManager` | — | **Not migrated** → use direct API |
| `org.keycloak.testsuite.ProfileAssume` | — | **Replaced** → `@DisabledForDatabases` / `Assumptions` |
| `org.keycloak.testsuite.util.WaitUtils` | `org.keycloak.testframework.ui.webdriver.WaitUtils` | **Rewritten** (see webdriver.md) |
| `org.keycloak.testsuite.util.TokenSignatureUtil` | — | **Not migrated** → inline (see server-and-registration.md) |
| `org.keycloak.testsuite.util.RealmBuilder` | `org.keycloak.testframework.realm.RealmConfigBuilder` | **Replaced** (see core.md Builder → Config) |
| `org.keycloak.testsuite.util.ClientBuilder` | `org.keycloak.testframework.realm.ClientConfigBuilder` | **Replaced** (see core.md Builder → Config) |
| `org.keycloak.testsuite.util.UserBuilder` | `org.keycloak.testframework.realm.UserConfigBuilder` | **Replaced** (see core.md Builder → Config) |
| `org.keycloak.testsuite.util.RoleBuilder` | `org.keycloak.testframework.realm.RoleConfigBuilder` | **Replaced** (see core.md Builder → Config) |
| `org.keycloak.testsuite.util.RolesBuilder` | — | **Obsolete** → use `RealmConfigBuilder.roles()` or `addRole()` |
| `org.keycloak.testsuite.util.GroupBuilder` | `org.keycloak.testframework.realm.GroupConfigBuilder` | **Replaced** (see core.md Builder → Config) |
| `org.keycloak.testsuite.util.FlowBuilder` | `org.keycloak.testframework.realm.AuthenticationFlowConfigBuilder` | **Replaced** |
| `org.keycloak.testsuite.util.ExecutionBuilder` | `org.keycloak.testframework.realm.AuthenticationExecutionExportConfigBuilder` | **Replaced** |

---

## Detailed Mappings

### AccountHelper — No change needed
```java
// OLD and NEW — same import (class is in utils-shared)
import org.keycloak.testsuite.util.AccountHelper;

// Usage stays the same
AccountHelper.logout(realm.admin(), "username");
AccountHelper.updatePassword(realm.admin(), "user", "newpass");
```

### SimpleHttpDefault — Replace with @InjectSimpleHttp or @InjectHttpClient

`SimpleHttpDefault` is a legacy static helper that wraps `SimpleHttp` with an explicit `HttpClient` parameter. Replace it with framework-managed injection. Choose based on context:

- **`@InjectSimpleHttp`** — when the test uses `SimpleHttpDefault.doGet()`, `doPost()`, `doPut()`, `doDelete()` for JSON API calls (account REST API, well-known endpoints, etc.). This is the preferred replacement.
- **`@InjectHttpClient`** — when the test needs low-level `HttpClient` directly (e.g., `HttpOptions`, custom request headers, raw HTTP operations).

Check already-migrated tests for examples:
```bash
grep -rln "InjectSimpleHttp" tests/base/src/test/java/
grep -rln "InjectHttpClient" tests/base/src/test/java/
```

```java
// OLD — SimpleHttpDefault with manual HttpClient
CloseableHttpClient httpClient;

@Before
public void before() {
    httpClient = HttpClientBuilder.create().build();
}

@After
public void after() {
    httpClient.close();
}

// In test:
SimpleHttpDefault.doGet(url, httpClient).auth(token).asJson(Type.class);
SimpleHttpDefault.doPost(url, httpClient).auth(token).json(body).asResponse();

// NEW Option A — @InjectSimpleHttp (preferred for JSON API calls)
@InjectSimpleHttp
SimpleHttp simpleHttp;

// In test:
simpleHttp.doGet(url).auth(token).asJson(Type.class);
simpleHttp.doPost(url).auth(token).json(body).asResponse();
// Note: no HttpClient parameter — the framework manages it

// NEW Option B — @InjectHttpClient (for low-level HTTP operations)
@InjectHttpClient
HttpClient httpClient;

// In test: use httpClient directly with HttpGet, HttpPost, etc.
```

**Imports:**
```
org.keycloak.testframework.annotations.InjectSimpleHttp
org.keycloak.http.simple.SimpleHttp
org.keycloak.testframework.annotations.InjectHttpClient
```

**Key differences from SimpleHttpDefault:**
- No `HttpClient` parameter in method calls — `simpleHttp.doGet(url)` not `SimpleHttpDefault.doGet(url, httpClient)`
- No manual `@Before`/`@After` to create/close `HttpClient` — the framework manages the lifecycle
- The `SimpleHttp` instance is `org.keycloak.http.simple.SimpleHttp`, NOT `SimpleHttpDefault`

### TokenUtil — Replace with OAuthClient.doPasswordGrantRequest()

`TokenUtil` is a JUnit 4 `@Rule` that acquires an access token via password grant using the `direct-grant` client. `OAuthClient` fully replaces it — no need to port `TokenUtil` as a utility class.

**What TokenUtil does internally:**
1. Creates an `OAuthClient` with client `direct-grant` / secret `password`
2. `getToken()` calls `doPasswordGrantRequest(username, password)`, caches the token, auto-refreshes on expiry
3. Returns a raw access token `String`

**How to replace:**

```java
// OLD — TokenUtil as @Rule
@Rule
public TokenUtil tokenUtil = new TokenUtil("test-user@localhost", "password");

// In test:
simpleHttp.doGet(url).auth(tokenUtil.getToken()).asJson(Type.class);

// OLD — TokenUtil created inline for different users
TokenUtil viewToken = new TokenUtil("view-account-access", "password");
simpleHttp.doGet(url).auth(viewToken.getToken()).asJson(Type.class);


// NEW — use @InjectOAuthClient + helper method
@InjectOAuthClient
OAuthClient oAuthClient;

private String getAccessToken(String username, String password) {
    return oAuthClient.client("direct-grant", "password")
        .doPasswordGrantRequest(username, password)
        .getAccessToken();
}

// In test:
simpleHttp.doGet(url).auth(getAccessToken("test-user@localhost", "password")).asJson(Type.class);

// For different users — same helper:
simpleHttp.doGet(url).auth(getAccessToken("view-account-access", "password")).asJson(Type.class);
```

**Notes:**
- The `direct-grant` client must exist in the realm. It's in `testrealm.json` (clientId: `direct-grant`, secret: `password`). If using minimal `RealmConfig`, add it: `realm.addClient("direct-grant").secret("password").directAccessGrantsEnabled(true);`
- No token caching — each call does a fresh password grant. Tests run fast enough that this doesn't matter.
- If the test only uses a single user's token repeatedly, store the result: `String token = getAccessToken("user", "pass");`
- Remove all `@Rule public TokenUtil` declarations and `new TokenUtil(...)` inline calls.

**Already-migrated examples:**
- `AbstractFineGrainedAdminTest`: `oauth.doPasswordGrantRequest("admin", "admin").getAccessToken()`
- `AdminConsoleWhoAmILocaleTest`: helper `accessToken(oAuth, clientId, secret, user, pass)`
- `AbstractGroupTest`: `oAuth.client(clientId, clientSecret).doPasswordGrantRequest(username, "password")`

### FlowUtil — No change needed
```java
// OLD and NEW — same import (class is in utils-shared)
import org.keycloak.testsuite.util.FlowUtil;

// Usage stays the same
FlowUtil.inCurrentRealm(realm.admin()).copyBrowserFlow("custom-flow");
```

### Assert — Package changed
```java
// OLD
import org.keycloak.testsuite.Assert;
Assert.assertNames(list, "name1", "name2");

// NEW
import org.keycloak.tests.utils.Assert;
Assert.assertNames(list, "name1", "name2");  // Same API, JUnit 6 based
```

Methods: `assertNames()`, `assertMap()`, `assertProviderConfigProperty()`, `assertExpiration()`, `assertRoleAttributes()`

### ApiUtil — Renamed and moved
```java
// OLD
import org.keycloak.testsuite.admin.ApiUtil;
String id = ApiUtil.getCreatedId(response);
ClientResource client = ApiUtil.findClientResourceByName(realm, "name");

// NEW — use AdminApiUtil for full API
import org.keycloak.tests.utils.admin.AdminApiUtil;
ClientResource client = AdminApiUtil.findClientResourceByName(realm.admin(), "name");

// NEW — or use framework ApiUtil for just getCreatedId()
import org.keycloak.testframework.util.ApiUtil;
String id = ApiUtil.getCreatedId(response);
```

`AdminApiUtil` methods: `findClientResourceById()`, `findClientResourceByName()`, `findClientByClientId()`, `findClientRoleByName()`, `findProtocolMapperByName()`, `findClientScopeByName()`, `findRealmRoleByName()`, `findUserByUsername()`, `createUserWithAdminClient()`, `resetUserPassword()`, `assignRealmRoles()`, `assignClientRoles()`

### AdminClientUtil — Obsolete
```java
// OLD
import org.keycloak.testsuite.util.AdminClientUtil;
Keycloak adminClient = AdminClientUtil.createAdminClient();

// NEW — use injection instead
@InjectAdminClient
Keycloak adminClient;
```

### ClientManager — Not migrated
```java
// OLD
import org.keycloak.testsuite.util.ClientManager;
ClientManager.realm(adminClient.realm("test")).clientId("my-client").addRedirectUris("http://...");

// NEW — use direct API calls
ClientRepresentation client = realm.admin().clients().get(clientId).toRepresentation();
client.getRedirectUris().add("http://...");
realm.admin().clients().get(clientId).update(client);
```

### ProfileAssume — Replaced by framework conditions
```java
// OLD
import org.keycloak.testsuite.ProfileAssume;
ProfileAssume.assumeFeatureEnabled(Feature.ADMIN_FINE_GRAINED_AUTHZ);

// NEW — use JUnit 5 conditional annotations
import org.junit.jupiter.api.Assumptions;
Assumptions.assumeTrue(condition, "reason");

// Or use framework-provided conditions:
@DisabledForDatabases({"mssql"})    // Skip for specific databases
@DisabledForServers({"remote"})     // Skip for specific server types
```

**Imports for conditions:**
```
org.keycloak.testframework.conditions.DisabledForDatabases
org.keycloak.testframework.conditions.DisabledForServers
```

---

## testingClient Replacement

The legacy `testingClient` (`KeycloakTestingClient`) is NOT available in the new framework. Replace with specific injections:

| Legacy Pattern | New Replacement |
|---|---|
| `testingClient.testing().setTimeOffset(...)` | `@InjectTimeOffSet TimeOffSet timeOffSet` → `timeOffSet.set(n)` |
| `testingClient.testing().pollEvent()` | `@InjectEvents Events events` → `events.poll()` |
| `testingClient.testing().pollAdminEvent()` | `@InjectAdminEvents AdminEvents adminEvents` → `adminEvents.poll()` |
| `testingClient.testing().clearEventQueue()` | `events.clear()` |
| `testingClient.testing().clearAdminEventQueue()` | `adminEvents.clear()` |
| `testingClient.testing().removeUserSessions(realm)` | Use `RunOnServerClient` or admin API (see below) |
| `testingClient.server().run(session -> ...)` | `@InjectRunOnServer RunOnServerClient runOnServer` → `runOnServer.run(session -> ...)` |
| `testingClient.server().fetch(session -> ..., Type.class)` | `runOnServer.fetch(session -> ..., Type.class)` |
| `testingClient.testApp()` | `@InjectTestApp TestApp testApp` |

### RunOnServerClient examples

```java
@InjectRunOnServer
RunOnServerClient runOnServer;

// Execute code on server (no return value)
runOnServer.run(session -> {
    RealmModel realm = session.realms().getRealm(realmId);
    session.sessions().removeUserSessions(realm);
});

// Fetch data from server (with return value)
List<String> aliases = runOnServer.fetch(session -> {
    return session.identityProviders().getAllStream()
        .map(IdentityProviderModel::getAlias)
        .toList();
}, List.class);
```

**Imports:**
```
org.keycloak.testframework.remote.runonserver.InjectRunOnServer
org.keycloak.testframework.remote.runonserver.RunOnServerClient
```

Use `@InjectRunOnServer(realmRef = "myRealm")` to bind to a specific realm.

---

## CryptoInitRule → @InjectCryptoHelper

```java
// OLD
@ClassRule
public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

// NEW
@InjectCryptoHelper
CryptoHelper cryptoHelper;
```

---

## getAuthServerRoot / getAuthUrl → @InjectKeycloakUrls

```java
// OLD
String authUrl = getAuthServerRoot();
String contextRoot = suiteContext.getAuthServerInfo().getContextRoot().toString();

// NEW
@InjectKeycloakUrls
KeycloakUrls keycloakUrls;

String baseUrl = keycloakUrls.getBaseUrl().toString();
```

---

## Email Testing — GreenMailRule → @InjectMailServer

```java
// OLD
@Rule public GreenMailRule greenMail = new GreenMailRule();
MimeMessage message = greenMail.getLastEmail();

// NEW — SMTP auto-configured when @InjectMailServer is present
@InjectMailServer
MailServer mailServer;

MimeMessage message = mailServer.getLastReceivedMessage();
List<MimeMessage> messages = mailServer.getReceivedMessages();
```

**Import:** `org.keycloak.testframework.mail.annotations.InjectMailServer` and `org.keycloak.testframework.mail.MailServer`

---

## Missing Util Classes — Finding, Reusing, or Creating Utils

When a migrated test imports a utility class from `org.keycloak.testsuite.*` that doesn't exist in the new modules, follow this resolution order:

### Step 1: Search for an equivalent in the new modules

Search both new util modules for a class with the same or similar functionality (it may have a different name):
- `tests/utils/src/main/java/org/keycloak/tests/utils/` — utilities used only by new tests
- `tests/utils-shared/src/main/java/org/keycloak/testsuite/util/` — shared between old and new tests

Use Grep to search by method names the test actually calls, not just by class name. For example, if the test calls `SomeUtil.findById(...)`, search for `findById` across both modules.

If found → update the import to the new location.

### Step 2: Check if the logic can be inlined

If the utility usage is simple (1-3 lines, single method call), inline the logic directly:
```java
// OLD: TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.ES256)
// Inline it:
managedRealm.updateWithCleanup(r -> r.defaultSignatureAlgorithm(Algorithm.ES256));
```

### Step 3: Create the utility class

If no equivalent exists and the logic is too complex to inline, **create the utility class**. This is not a blocker — missing utils must be created as part of the migration.

**Determine the target module:**

1. Search the legacy `testsuite/` for other tests that import this utility class:
   ```
   Grep for the import across testsuite/integration-arquillian/tests/base/src/test/java/
   ```

2. **If other non-migrated tests also use it** → place in `tests/utils-shared/`
   - Package: `org.keycloak.testsuite.util` (keeps the old import path, so both old and new tests can use it)
   - Location: `tests/utils-shared/src/main/java/org/keycloak/testsuite/util/`

3. **If only the migrated test uses it** (or all users are already migrated) → place in `tests/utils/`
   - Package: `org.keycloak.tests.utils`
   - Location: `tests/utils/src/main/java/org/keycloak/tests/utils/`

**When creating the utility class:**
- Copy only the methods the migrated test actually uses — don't port the entire class
- Remove any dependencies on legacy infrastructure (`testingClient`, `AbstractKeycloakTest`, etc.)
- Adapt method signatures to accept new-framework types where appropriate (e.g., `RealmResource` from `realm.admin()` instead of `adminClient.realm("test")`)
- If the util uses `testingClient.server().run()`, convert to accept `RunOnServerClient` as a parameter instead

**Compile the util module after creating:**
```bash
# For utils-shared:
./mvnw install -pl tests/utils-shared -DskipTests

# For utils:
./mvnw install -pl tests/utils -DskipTests
```
