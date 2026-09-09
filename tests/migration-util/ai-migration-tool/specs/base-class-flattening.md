# Base Class Flattening Recipes

When a legacy test extends a base class that has no equivalent in the new framework, you must "flatten" it — replace inheritance with explicit `@Inject*` field declarations and inline any needed helper methods.

**Always check `specs/keycloak-tests-module.md` Section 2 first** — if a new base class exists (e.g., `AbstractRealmTest`, `AbstractAuthenticationTest`), extend it instead of flattening.

---

## AbstractKeycloakTest (ROOT)

The root base class. All legacy tests inherit from this (directly or indirectly).

### Fields to inject

| Legacy field | Type | New injection |
|---|---|---|
| `adminClient` | `Keycloak` | `@InjectAdminClient Keycloak adminClient` |
| `testingClient` | `KeycloakTestingClient` | Remove — use `@InjectRunOnServer` / `@InjectTimeOffSet` / `@InjectEvents` instead |
| `oauth` | `OAuthClient` | `@InjectOAuthClient OAuthClient oauthClient` |
| `driver` | `WebDriver` | `@InjectWebDriver ManagedWebDriver webDriver` (only if test uses browser) |
| `loginPage` | `LoginPage` | `@InjectPage LoginPage loginPage` (only if test uses it) |
| `suiteContext` | `SuiteContext` | `@InjectKeycloakUrls KeycloakUrls keycloakUrls` (for server URLs) |
| `testContext` | `TestContext` | Remove — not needed |

### Methods to replace

| Legacy method | Replacement |
|---|---|
| `addTestRealms(List<RealmRepresentation>)` | `@InjectRealm(config = ...)` with `RealmConfig` class |
| `testRealm()` / `adminClient.realm("test")` | `realm.admin()` |
| `getAuthServerRoot()` | `keycloakUrls.getBaseUrl().toString()` |
| `getCleanup("realm").addCleanup(...)` | `realm.cleanup().add(r -> ...)` |
| `importRealm(rep)` | Not available — use `realm.dirty()` to force recreation |

### Constants to inline

| Constant | Value |
|---|---|
| `PROPERTY_LOGIN_THEME_DEFAULT` | check source |
| `TEST_REALM_NAME` | `"test"` |

---

## AbstractTestRealmKeycloakTest

Extends `AbstractKeycloakTest`. Loads `/testrealm.json` and provides `configureTestRealm()`.

### Additional flattening

Everything from `AbstractKeycloakTest` above, plus:

| Legacy method | Replacement |
|---|---|
| `configureTestRealm(RealmRepresentation)` | Merge into `RealmConfig.configure()` |
| `loadJson(stream, type)` | `@InjectRealm(fromJson = "/org/keycloak/tests/testrealm.json")` — but prefer minimal `RealmConfig` |
| `importTestRealm(Consumer<RealmRepresentation>)` | Not available — use `realm.dirty()` + fresh config |
| `testRealmReps` | Remove — realm managed by framework |

### Typical flattened result

```java
@KeycloakIntegrationTest
public class MyTest {
    @InjectRealm(config = MyRealmConfig.class)
    ManagedRealm realm;

    public static class MyRealmConfig implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addUser("test-user").password("password").roles("user");
            return realm;
        }
    }
}
```

---

## AbstractAdminTest

Extends `AbstractTestRealmKeycloakTest`. Adds `AssertAdminEvents`, event listeners, SMTP.

### Additional flattening

Everything from `AbstractTestRealmKeycloakTest` above, plus:

| Legacy field/method | Replacement |
|---|---|
| `@Rule AssertAdminEvents` | `@InjectAdminEvents AdminEvents adminEvents` |
| `configureTestRealm()` adds event listeners + SMTP | Add to `RealmConfig`: `realm.eventsEnabled(true).adminEventsEnabled(true).adminEventsDetailsEnabled(true)` |

### Check first

There IS a `AbstractRealmTest` in the new tests module. Check if it fits before flattening.

---

## AbstractAuthTest

Extends `AbstractKeycloakTest`. Adds test realm with pre-configured users (testUser, bburkeUser).

### Additional flattening

Everything from `AbstractKeycloakTest` above, plus:

| Legacy field | Replacement |
|---|---|
| `testUser` / `bburkeUser` | Create via `RealmConfig.addUser()` or `@InjectUser` |
| `testRealmResource()` | `realm.admin()` |
| `@Page AccountUpdateProfilePage` etc. | `@InjectPage` — create page if missing |

---

## AbstractAuthenticationTest

Extends `AbstractKeycloakTest`. Provides authentication flow management helpers.

### Additional flattening

| Legacy field/method | Replacement |
|---|---|
| `@Rule AssertAdminEvents` | `@InjectAdminEvents AdminEvents adminEvents` |
| `findFlowByAlias(alias, flows)` | Inline: iterate `realm.admin().flows().getFlows()` |
| `findExecutionByProvider(provider, executions)` | Inline: iterate `realm.admin().flows().getExecutions(flow)` |
| `newFlow(alias, desc, providerId, topLevel, builtIn)` | Create `AuthenticationFlowRepresentation` directly |
| `createFlow(flowRep)` | `realm.admin().flows().createFlow(flowRep)` |

### Check first

There IS an `AbstractAuthenticationTest` in the new tests module. Check if it fits before flattening.

---

## AbstractEventsTest

Extends `AbstractKeycloakTest`. Provides event testing infrastructure.

### Additional flattening

| Legacy field/method | Replacement |
|---|---|
| Events configuration in `addTestRealms()` | Add to `RealmConfig`: `realm.eventsEnabled(true)` |
| Event polling via `testingClient.testing()` | `@InjectEvents Events events` + `events.poll()` |

---

## AbstractChangeImportedUserPasswordsTest

Extends `AbstractTestRealmKeycloakTest`. Regenerates user passwords to random values so browsers (Chrome) don't reject weak passwords.

### Flattening recipe

```java
private final Map<String, String> userPasswords = new HashMap<>();

@BeforeEach
void setupPasswords() {
    if (userPasswords.isEmpty()) {
        for (UserRepresentation user : realm.admin().users().list()) {
            String password = "P" + UUID.randomUUID().toString().substring(0, 12) + "!aA1";
            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(PasswordCredentialModel.TYPE);
            cred.setTemporary(false);
            cred.setValue(password);
            realm.admin().users().get(user.getId()).resetPassword(cred);
            userPasswords.put(user.getUsername(), password);
        }
    }
}

private String getPassword(String username) {
    String password = userPasswords.get(username);
    Assertions.assertNotNull(password, "password not generated for user " + username);
    return password;
}
```

**Important**: If combined with `realm.dirty()` (for tests modifying browser flows), clear `userPasswords` too since the realm gets recreated.

---

## Decision Flowchart

```
Legacy test extends AbstractXxxTest
    │
    ├─ Does AbstractXxxTest exist in tests/base/?
    │   └─ YES → extend it, check what it provides
    │
    └─ NO → flatten:
        1. Read the ENTIRE base class chain
        2. List fields/methods the test ACTUALLY uses
        3. Replace each with @Inject* or inline
        4. Only bring what's needed — don't port the whole base class
```
