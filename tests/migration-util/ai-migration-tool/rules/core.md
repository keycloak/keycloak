# Core Migration Rules

These rules apply to ALL test migrations regardless of complexity.

## Table of Contents

1. [Target Module Routing](#rule-target-module-routing) — which test module to target
2. [Base Class Resolution](#rule-base-class-resolution) — extend or flatten legacy base classes
3. [Realm Configuration](#rule-realm-configuration) — prefer minimal RealmConfig over fromJson
4. [Builder → Config Transformation](#rule-builder--config-transformation) — RealmBuilder/ClientBuilder/etc. replacements
5. [Resource Injection](#rule-resource-injection) — @ArquillianResource/@Drone/@Page/@Rule → @Inject*
6. [Admin Client Access](#rule-admin-client-access) — adminClient.realm() → realm.admin()
7. [JUnit 4 → JUnit 6](#rule-junit-4--junit-6) — annotations, imports
8. [Package Names](#rule-package-names) — org.keycloak.testsuite → org.keycloak.tests
9. [Imports Cleanup](#rule-imports-cleanup) — remove/add imports
10. [Missing Maven Dependencies](#rule-missing-maven-dependencies) — add Keycloak deps to pom.xml
11. [Dead Code Removal](#rule-dead-code-removal) — remove unused imports/fields/methods
12. [Hamcrest Assertions](#rule-hamcrest-assertions--no-migration-needed) — keep as-is
13. [Annotations to Remove → KeycloakServerConfig](#rule-annotations-to-remove--keycloakserverconfig) — @EnableFeature/@EnableVault/etc.
14. [Admin Client Factory](#rule-admin-client-factory-multiple-admin-clients) — multiple Keycloak instances
15. [User Configuration](#rule-user-configuration) — @InjectUser limitations, addUser()
16. [Client Configuration](#rule-client-configuration) — @InjectClient, RealmConfig inline
17. [JUnit 4 Assertion Message Order](#rule-junit-4-assertion-message-order) — message-first → message-last
18. [RealmConfigBuilder Method Reference](#rule-realmconfigbuilder-method-reference) — setter → builder mapping table
19. [Unrecognized Patterns](#rule-unrecognized-patterns) — TODO MIGRATION comments

---

## Rule: Target Module Routing

Not all tests go to `tests/base`. Determine the target module by the legacy test's package:

| Legacy package | Target module | Target package |
|---|---|---|
| `org.keycloak.testsuite.webauthn.*` | `tests/webauthn` | `org.keycloak.tests.webauthn` |
| Everything else | `tests/base` | `org.keycloak.tests.*` |

For webauthn tests, `migrate.sh` outputs to `tests/base/` — you must manually move the file to `tests/webauthn/src/test/java/org/keycloak/tests/webauthn/` and update the package declaration.

---

## Rule: Base Class Resolution

**See `specs/base-class-flattening.md`** for detailed flattening recipes per base class.

**If the legacy test extends a legacy base class:**

| Legacy Base Class | Action |
|---|---|
| `AbstractKeycloakTest` | Remove `extends`. Add `@InjectRealm ManagedRealm realm` and `@InjectAdminClient Keycloak adminClient` fields. |
| `AbstractTestRealmKeycloakTest` | Same as above. Convert `configureTestRealm()` to a `RealmConfig` class. |
| `AbstractAdminTest` | Check if `AbstractRealmTest` exists in `tests/`. If yes, extend it. If no, flatten. |
| `AbstractAuthenticationTest` | Check if it exists in `tests/`. If yes, extend it. If no, flatten with `@InjectRealm`, auth management resource field. |
| Any other `Abstract*Test` | Search `tests/base/src/test/java/` for equivalent. Extend if found, flatten if not. |

**When flattening:**
1. Read the ENTIRE legacy base class chain (e.g., `MyTest` → `AbstractClientRegistrationTest` → `AbstractKeycloakTest`). You must understand all inherited fields and methods.
2. Identify which inherited fields/methods are **actually used** by the specific test being migrated. Only bring over what's needed — don't migrate helper methods used only by sibling tests.
3. Replace inherited fields with `@Inject*` field declarations (e.g., `adminClient` → `@InjectAdminClient Keycloak adminClient`).
4. Inline any `@Before`/`@After` logic from the base class that the test depends on into `@BeforeEach`/`@AfterEach` methods.
5. If the base class provides utility methods the test uses (e.g., `getToken()`, `createClient()`), either inline them as private methods or find the new-framework equivalent.
6. **Inherited constants**: If the test references constants defined in the base class (e.g., `PROPERTY_LOGIN_THEME_DEFAULT` from `AbstractKeycloakTest`), inline their values as `private static final` fields in the migrated test. Search the base class chain with Grep to find the constant's definition.

---

## Rule: Realm Configuration

**Prefer minimal `RealmConfig` over `fromJson`.**

Most legacy tests extend `AbstractTestRealmKeycloakTest` which loads `/testrealm.json` — a large file with many users, clients, and roles. Do NOT blindly use `@InjectRealm(fromJson = "testrealm.json")`. Instead, analyze which realm resources the test actually uses and create a minimal `RealmConfig` with only those resources. This approach:

- Avoids conflicts (e.g., `testrealm.json` contains a `test-app` client that conflicts with `@InjectOAuthClient`'s default client)
- Makes test dependencies explicit and readable
- Is faster (smaller realm to create/destroy)
- Avoids inheriting unnecessary configuration that can cause unexpected behavior

**Only use `fromJson` when** the test genuinely depends on the full testrealm structure (many users with specific credentials/OTP configurations, complex client setups, etc.) and recreating it programmatically would be impractical. See `specs/testrealm-json.md` for the complete user/client/role summary and a decision table.

**If the test has `addTestRealms()`:**

Transform into `@InjectRealm` with a config class containing only what the test needs:

```java
// OLD: addTestRealms() with programmatic setup
@Override
public void addTestRealms(List<RealmRepresentation> testRealms) {
    RealmRepresentation realm = new RealmRepresentation();
    realm.setRealm("my-realm");
    realm.setEnabled(true);
    testRealms.add(realm);
}

// NEW: Config class + injection
@InjectRealm(config = MyRealmConfig.class)
ManagedRealm realm;

public static class MyRealmConfig implements RealmConfig {
    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        return realm.name("my-realm");
    }
}
```

**If the legacy test loads JSON but only uses a few resources from it:**

Don't load the JSON — build the needed resources programmatically:

```java
// OLD: loads entire testrealm.json just to get a realm with one user
extends AbstractTestRealmKeycloakTest  // loads /testrealm.json

// NEW: create only what's needed
@InjectRealm(config = TestRealmConfig.class)
ManagedRealm realm;

public static class TestRealmConfig implements RealmConfig {
    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        realm.addUser("test-user").password("password").roles("user");
        realm.addClient("test-client").secret("secret").redirectUris("*");
        return realm;
    }
}
```

**If the test truly requires the full JSON realm** (e.g., many users with OTP credentials, complex role hierarchies):

```java
// Only when the full realm structure is genuinely needed
@InjectRealm(fromJson = "/org/keycloak/tests/testrealm.json")
ManagedRealm realm;
```

Note: use the absolute classpath path with leading `/`. Be aware that `testrealm.json` contains a `test-app` client — if you also use `@InjectOAuthClient`, add `ref` to make the clientId unique (e.g., `@InjectOAuthClient(ref = "browser")` creates `test-app-browser`). See `oauth.md` for details. Do NOT use a custom `ClientConfig` to change the clientId — that breaks the `TestApp` redirect endpoint wiring.

**If the test uses `configureTestRealm()`:** Extract the actual configuration applied and express it as a `RealmConfig`. Do not load JSON just to apply a few overrides on top.

**If the test creates multiple realms:** Use multiple `@InjectRealm` fields with `ref`:

```java
@InjectRealm(ref = "realm1", config = Realm1Config.class)
ManagedRealm realm1;

@InjectRealm(ref = "realm2", config = Realm2Config.class)
ManagedRealm realm2;
```

---

## Rule: Builder → Config Transformation

Every legacy `*Builder` class in `org.keycloak.testsuite.util` has a new equivalent `*ConfigBuilder` in `org.keycloak.testframework.realm`:

| Legacy Builder | New ConfigBuilder | Import |
|---|---|---|
| `RealmBuilder` | `RealmConfigBuilder` | `o.k.testframework.realm.RealmConfigBuilder` |
| `ClientBuilder` | `ClientConfigBuilder` | `o.k.testframework.realm.ClientConfigBuilder` |
| `UserBuilder` | `UserConfigBuilder` | `o.k.testframework.realm.UserConfigBuilder` |
| `RoleBuilder` / `RolesBuilder` | `RoleConfigBuilder` | `o.k.testframework.realm.RoleConfigBuilder` |
| `GroupBuilder` | `GroupConfigBuilder` | `o.k.testframework.realm.GroupConfigBuilder` |
| `FlowBuilder` / `ExecutionBuilder` | `AuthenticationFlowConfigBuilder` / `AuthenticationExecutionExportConfigBuilder` | `o.k.testframework.realm.*` |

The new builders follow the same pattern: `XxxConfigBuilder.create().name("x").build()` returns a representation.

**Chaining with `addClient()`, `addUser()`, `addGroup()`, `addRole()` on `RealmConfigBuilder`:**

These methods immediately add the object to the realm's internal list and return the nested builder (e.g., `UserConfigBuilder`). The nested builder operates on the already-added object. You do NOT need to call `.build()` to "commit" it — it's already part of the realm. `.build()` returns the raw representation (e.g., `UserRepresentation`), NOT back to `RealmConfigBuilder`.

```java
// CORRECT — each addX() call adds to the realm; no .build() needed for chaining
public RealmConfigBuilder configure(RealmConfigBuilder realm) {
    realm.addUser("marta").password("password");
    realm.addUser("kolo").password("password");
    realm.addClient("my-client").secret("secret").redirectUris("*");
    realm.addRole("admin").description("Admin role");
    return realm;
}

// WRONG — .build() returns UserRepresentation, not RealmConfigBuilder
realm.addUser("marta").password("password").build()
     .addUser("kolo").password("password").build();  // COMPILE ERROR
```

**RealmBuilder:**
```java
// OLD
RealmBuilder.create().name("test").roles(RolesBuilder.create()
    .realmRole(RoleBuilder.create().name("admin").build())).build()

// NEW (inside RealmConfig.configure())
return realm.name("test").roles("admin");
```

**ClientBuilder:**
```java
// OLD
ClientBuilder.create().clientId("my-client").secret("secret").redirectUris("*").build()

// NEW (inside ClientConfig.configure())
return client.clientId("my-client").secret("secret").redirectUris("*");
```

**UserBuilder:**
```java
// OLD
UserBuilder.create().username("testuser").password("pass").role("realm", "admin").build()

// NEW (inside UserConfig.configure())
return user.username("testuser").password("pass").roles("admin");
```

**RoleBuilder:**
```java
// OLD
RoleBuilder.create().name("admin").build()

// NEW — Option A: inside RealmConfigBuilder (no .build() needed — already added)
realm.addRole("admin").description("Admin role");

// NEW — Option B: standalone (e.g., for admin API calls like realm.admin().roles().create())
RoleConfigBuilder.create().name("admin").build()  // returns RoleRepresentation
```

**GroupBuilder:**
```java
// OLD
GroupBuilder.create().name("mygroup").build()

// NEW — Option A: inside RealmConfigBuilder (no .build() needed)
realm.addGroup("mygroup");

// NEW — Option B: standalone
GroupConfigBuilder.create().name("mygroup").build()  // returns GroupRepresentation
```

**RolesBuilder (composite roles):**
```java
// OLD
RolesBuilder.create()
    .realmRole(RoleBuilder.create().name("admin").build())
    .realmRole(RoleBuilder.create().name("user").build())

// NEW
realm.roles("admin", "user")
// or for client roles:
realm.clientRoles("my-client", "role1", "role2")
```

---

## Rule: Resource Injection

| Old Pattern | New Pattern |
|---|---|
| `@ArquillianResource OAuthClient oauth` | `@InjectOAuthClient OAuthClient oauthClient` |
| `@Drone WebDriver driver` | `@InjectWebDriver ManagedWebDriver webDriver` |
| `@SecondBrowser @Drone WebDriver driver2` | `@InjectWebDriver(ref = "second") ManagedWebDriver webDriver2` — see `webdriver.md` for full pattern |
| `@Page LoginPage loginPage` | `@InjectPage LoginPage loginPage` |
| `@Rule AssertEvents events = new AssertEvents(this)` | `@InjectEvents Events events` |
| `@Rule AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this)` | `@InjectAdminEvents AdminEvents adminEvents` |
| `@Rule GreenMailRule greenMail = new GreenMailRule()` | `@InjectMailServer MailServer mailServer` |
| `adminClient` (inherited field) | `@InjectAdminClient Keycloak adminClient` |
| `testingClient` (inherited field) | Remove — use `@InjectRunOnServer` or `@InjectTimeOffSet` instead |
| `@ArquillianResource SuiteContext` | Remove — see server-and-registration.md for server URL access |
| `@ArquillianResource TestContext` | Remove — not needed |
| `suiteContext.getAuthServerInfo().getContextRoot()` | `@InjectKeycloakUrls KeycloakUrls keycloakUrls` + `keycloakUrls.getBaseUrl().toString()` |

---

## Rule: Admin Client Access

```java
// OLD (via inherited field or realm name string)
adminClient.realm("test").users().list()
realm.users().create(user)  // where realm is a RealmResource

// NEW (via injected ManagedRealm)
realm.admin().users().list()
```

If the legacy test uses `adminClient.realm(realmName)` with a variable realm name, check if `realm.admin()` is equivalent. If the test operates on the master realm, use:

```java
@InjectRealm(attachTo = "master", ref = "master")
ManagedRealm masterRealm;
```

---

## Rule: JUnit 4 → JUnit 6

| Old | New |
|---|---|
| `import org.junit.Test` | `import org.junit.jupiter.api.Test` |
| `import org.junit.Before` | `import org.junit.jupiter.api.BeforeEach` |
| `import org.junit.After` | `import org.junit.jupiter.api.AfterEach` |
| `import org.junit.BeforeClass` | `import org.junit.jupiter.api.BeforeAll` |
| `import org.junit.AfterClass` | `import org.junit.jupiter.api.AfterAll` |
| `import org.junit.Assert` | `import org.junit.jupiter.api.Assertions` |
| `import static org.junit.Assert.*` | `import static org.junit.jupiter.api.Assertions.*` |
| `import org.junit.Ignore` | `import org.junit.jupiter.api.Disabled` |
| `@Ignore("reason")` | `@Disabled("reason")` |
| `@Test(expected = X.class)` | `assertThrows(X.class, () -> { ... })` |
| `@Test(timeout = 5000)` | `@Timeout(5)` |
| `public void testMethod()` | `void testMethod()` (remove `public` from test methods) |

---

## Rule: Package Names

All imports and package declarations:
- `org.keycloak.testsuite.` → `org.keycloak.tests.`
- `org.keycloak.testsuite.util.*` → see `rules/utilities.md` for the specific mapping (some stay, some move, some are renamed)

---

## Rule: Imports Cleanup

Remove these imports entirely (no replacement needed):
```
org.jboss.arquillian.container.test.api.RunAsClient
org.jboss.arquillian.drone.api.annotation.Drone
org.jboss.arquillian.graphene.page.Page
org.jboss.arquillian.test.api.ArquillianResource
org.junit.runner.RunWith
org.junit.runners.MethodSorters
org.junit.FixMethodOrder
org.keycloak.testsuite.arquillian.*
```

Add these imports as needed:
```
# Annotations (org.keycloak.testframework.annotations.*)
org.keycloak.testframework.annotations.KeycloakIntegrationTest
org.keycloak.testframework.annotations.InjectRealm
org.keycloak.testframework.annotations.InjectUser
org.keycloak.testframework.annotations.InjectClient
org.keycloak.testframework.annotations.InjectAdminClient
org.keycloak.testframework.annotations.InjectAdminClientFactory
org.keycloak.testframework.annotations.InjectEvents
org.keycloak.testframework.annotations.InjectAdminEvents
org.keycloak.testframework.annotations.InjectKeycloakUrls
org.keycloak.testframework.annotations.InjectHttpClient
org.keycloak.testframework.annotations.InjectSimpleHttp
org.keycloak.testframework.annotations.InjectDependency

# Realm/Client/User (org.keycloak.testframework.realm.*)
org.keycloak.testframework.realm.ManagedRealm
org.keycloak.testframework.realm.ManagedClient
org.keycloak.testframework.realm.ManagedUser
org.keycloak.testframework.realm.RealmConfig
org.keycloak.testframework.realm.RealmConfigBuilder
org.keycloak.testframework.realm.ClientConfig
org.keycloak.testframework.realm.ClientConfigBuilder
org.keycloak.testframework.realm.UserConfig
org.keycloak.testframework.realm.UserConfigBuilder

# Server (org.keycloak.testframework.server.*)
org.keycloak.testframework.server.KeycloakUrls
org.keycloak.testframework.server.KeycloakServerConfig
org.keycloak.testframework.server.KeycloakServerConfigBuilder

# OAuth (org.keycloak.testframework.oauth.*)
org.keycloak.testframework.oauth.OAuthClient
org.keycloak.testframework.oauth.annotations.InjectOAuthClient

# UI (org.keycloak.testframework.ui.*)
org.keycloak.testframework.ui.annotations.InjectWebDriver
org.keycloak.testframework.ui.annotations.InjectPage
org.keycloak.testframework.ui.webdriver.ManagedWebDriver

# Remote (org.keycloak.testframework.remote.*)
org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet
org.keycloak.testframework.remote.timeoffset.TimeOffSet
org.keycloak.testframework.remote.annotations.TestOnServer
org.keycloak.testframework.remote.runonserver.InjectRunOnServer
org.keycloak.testframework.remote.runonserver.RunOnServerClient

# Email (org.keycloak.testframework.mail.*)
org.keycloak.testframework.mail.annotations.InjectMailServer
org.keycloak.testframework.mail.MailServer

# Events (org.keycloak.testframework.events.*)
org.keycloak.testframework.events.Events
org.keycloak.testframework.events.AdminEvents
org.keycloak.testframework.events.EventAssertion
org.keycloak.testframework.events.AdminEventAssertion

# Utilities (org.keycloak.testframework.util.*)
org.keycloak.testframework.util.ApiUtil
```

**IMPORTANT**: Some annotations like `@InjectOAuthClient`, `@InjectWebDriver`, `@InjectPage`, `@InjectMailServer`, `@InjectTimeOffSet`, and `@InjectRunOnServer` are NOT in `org.keycloak.testframework.annotations.*` — they are in their respective module packages. When unsure, search the `test-framework/` directory for the annotation class to find the correct import.

---

## Rule: Missing Maven Dependencies

If the migrated test uses a Keycloak module that is not a dependency of the target test module (`tests/base`, `tests/webauthn`, or `tests/clustering`), **add the missing dependency to the target module's `pom.xml`**.

This commonly happens with:
- `keycloak-authz-client` — for tests using `AuthzClient` (authorization protection/permission API)
- Other Keycloak modules the legacy test had access to via the Arquillian classpath

**Steps:**

1. **Identify the missing dependency** from the compilation error (e.g., `cannot find symbol: class AuthzClient`)
2. **Find which module provides it**: `find . -name "AuthzClient.java" -path "*/main/*" -not -path "*/target/*"` — this shows the module path (e.g., `authz/client/`)
3. **Find its Maven coordinates**: check the `pom.xml` in that module directory for `<groupId>` and `<artifactId>`
4. **Add the dependency** to the target test module's `pom.xml` (e.g., `tests/base/pom.xml`):

```xml
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-authz-client</artifactId>
    <scope>test</scope>
</dependency>
```

Use `<scope>test</scope>` since test modules only need it at test time. Version is inherited from the parent POM's dependency management.

5. **Rebuild** after adding the dependency: `./mvnw test-compile -pl tests/base -DskipTests -am`

**Do NOT** skip the migration or rewrite working code just because a dependency is missing — adding a Keycloak dependency to a test module is the correct fix.

---

## Rule: Dead Code Removal

After applying all transformations, scan the entire file and **remove anything that is no longer used**:

1. **Unused imports** — any import whose type/method is no longer referenced in the file. Common leftovers after migration:
   - `org.keycloak.representations.idm.RealmRepresentation` (was used in `configureTestRealm()` signature, now removed)
   - `org.jboss.arquillian.graphene.page.Page` (replaced by `@InjectPage`)
   - `org.keycloak.testsuite.AbstractTestRealmKeycloakTest` or other legacy base classes (removed via flattening)
   - `org.keycloak.testsuite.pages.*` (replaced by `org.keycloak.testframework.ui.page.*`)

2. **Unused fields** — fields that are no longer referenced after transformation. Watch for duplicates created by `migrate.sh`:
   - The script injects `@InjectRealm ManagedRealm managedRealm` — if you rename this to `realm`, don't leave the old one behind
   - Fields from the removed base class that are no longer needed

3. **Empty methods** — methods whose body is empty after removing the base class:
   - Empty `configureTestRealm(RealmRepresentation)` overrides
   - Empty `addTestRealms(List<RealmRepresentation>)` overrides
   - `@Override` methods that no longer override anything after removing `extends`

4. **Dead `extends` clause** — after flattening, remove the `extends SomeBaseClass` entirely

The migrated file should compile with **zero** unused-import warnings. Do not leave orphaned code.

---

## Rule: Hamcrest Assertions — No Migration Needed

Hamcrest assertions (`assertThat`, `is()`, `hasSize()`, `containsString()`, etc.) are available in the new test framework and require **no migration**. Keep all Hamcrest imports and usage as-is:

```java
// These imports stay unchanged
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

// Usage stays unchanged
assertThat(users, hasSize(3));
assertThat(response.getStatus(), is(200));
assertThat(roles, containsInAnyOrder("admin", "user"));
```

Do NOT convert Hamcrest assertions to JUnit 6 `Assertions.*` — they serve different purposes and can coexist.

---

## Rule: Annotations to Remove → KeycloakServerConfig

Any legacy annotation that configures the Keycloak **server** (features, vault, SPI providers, etc.) must be replaced with a `KeycloakServerConfig` implementation. The test framework restarts the server with the specified configuration before running the test class.

**Always search for existing examples first:**
```bash
grep -rln "implements KeycloakServerConfig" tests/base/src/test/java/
```
There are 50+ existing implementations covering features, vault, metrics, tracing, custom providers, SPI options, and more. Copy a matching pattern rather than guessing.

**Remove these annotations and replace with `KeycloakServerConfig`:**

| Legacy Annotation | `KeycloakServerConfigBuilder` Method | Example |
|---|---|---|
| `@EnableFeature(Feature.X)` | `features(Profile.Feature.X)` | `features(Profile.Feature.AUTHORIZATION)` |
| `@DisableFeature(Feature.X)` | `featuresDisabled(Profile.Feature.X)` | `featuresDisabled(Profile.Feature.IMPERSONATION)` |
| `@EnableVault` | `option("vault", "file").option("vault-dir", path)` | See `SMTPConnectionVaultTest`, `ClientVaultTest` |
| `@SetDefaultProvider(spi, providerId)` | `option("spi-<spi>-provider", "providerId")` | `option("spi-hostname-provider", "fixed")` |
| `@ModelTest` | `@TestOnServer` (preferred) or `RunOnServerClient` | See `server-and-registration.md` — `@TestOnServer` is a direct replacement |

**Remove these entirely — no server config replacement needed:**
- `@RunWith(KcArquillian.class)` — replaced by `@KeycloakIntegrationTest`
- `@RunAsClient` — default in new framework
- `@AppServerContainer` — not applicable
- `@AuthServerContainerExclude` — not applicable
- `@UncaughtServerErrorExpected` — handle errors explicitly
- `@FixMethodOrder` — replaced by `@TestMethodOrder` (see `lifecycle.md`)

**Concrete examples from existing tests:**

```java
// Feature flags
@KeycloakIntegrationTest(config = MyTest.ServerConfig.class)
public class MyTest {
    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.AUTHORIZATION);
        }
    }
}

// Vault
public static class VaultConfig implements KeycloakServerConfig {
    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        URL url = MyTest.class.getResource("vault");
        return config.option("vault", "file").option("vault-dir", url.getPath());
    }
}

// Metrics
public static class MetricsConfig implements KeycloakServerConfig {
    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config.option("metrics-enabled", "true");
    }
}

// Tracing
public static class TracingConfig implements KeycloakServerConfig {
    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config.option("tracing-enabled", "true");
    }
}

// Custom providers
public static class CustomProvidersConfig implements KeycloakServerConfig {
    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
    }
}

// Combining multiple configs
public static class ComplexConfig implements KeycloakServerConfig {
    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config
            .features(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)
            .dependency("org.keycloak.tests", "keycloak-tests-custom-providers")
            .option("metrics-enabled", "true");
    }
}
```

Add a `// TODO: removed @AnnotationName — verify behavior` comment if the annotation might affect test logic and you're unsure about the correct `KeycloakServerConfig` translation.

---

## Rule: Admin Client Factory (Multiple Admin Clients)

When a legacy test creates multiple `Keycloak` instances with different credentials (via `AdminClientUtil`, `KeycloakBuilder`, or `Keycloak.getInstance`), use `@InjectAdminClientFactory`:

```java
// OLD
Keycloak realmAdmin = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(),
    TEST_REALM_NAME, "realmAdmin", "password", Constants.ADMIN_CLI_CLIENT_ID, null);

Keycloak serviceClient = KeycloakBuilder.builder()
    .serverUrl(getKeycloakServerUrl()).realm(realmName)
    .clientId("service-cl").clientSecret("secret1")
    .grantType(OAuth2Constants.CLIENT_CREDENTIALS).build();

// NEW
@InjectAdminClientFactory
AdminClientFactory adminClientFactory;

// Password grant
Keycloak realmAdmin = adminClientFactory.create()
    .realm(testRealm.getName())
    .username("realmAdmin").password("password")
    .clientId(Constants.ADMIN_CLI_CLIENT_ID)
    .autoClose()  // auto-closed when factory is destroyed
    .build();

// Client credentials grant
Keycloak serviceClient = adminClientFactory.create()
    .realm(testRealm.getName())
    .clientId("service-cl").clientSecret("secret1")
    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
    .autoClose()
    .build();

// Bearer token auth
Keycloak bearerClient = adminClientFactory.create()
    .realm(testRealm.getName())
    .authorization(accessTokenString)
    .autoClose()
    .build();
```

**When to use `@InjectAdminClient` vs `@InjectAdminClientFactory`:**

| Scenario | Use |
|---|---|
| Single bootstrap admin | `@InjectAdminClient Keycloak adminClient` |
| Admin scoped to a managed realm | `@InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM)` |
| Multiple admin clients with different users | `@InjectAdminClientFactory` |
| Service account / client credentials | `@InjectAdminClientFactory` |
| Permission tests (admin vs non-admin) | `@InjectAdminClientFactory` |

**AdminClientBuilder methods:** `.realm()`, `.username()`, `.password()`, `.clientId()`, `.clientSecret()`, `.grantType()`, `.scope()`, `.authorization()`, `.autoClose()`, `.build()`

**Import:**
```
org.keycloak.testframework.annotations.InjectAdminClientFactory
org.keycloak.testframework.admin.AdminClientFactory
```

---

## Rule: User Configuration

**IMPORTANT — `@InjectUser` does NOT support `roles()` or `clientRoles()`.**

The `UserSupplier` creates users via the Keycloak Users Resource API, which does not support setting roles during creation. Calling `roles()` or `clientRoles()` on a `UserConfigBuilder` used with `@InjectUser` will throw `UnsupportedOperationException`.

**If the user needs roles**, use `RealmConfigBuilder.addUser()` instead — realm import DOES support roles:

```java
// OLD — UserBuilder in addTestRealms
UserBuilder.create().username("testuser").password("pass").role("realm", "admin").build()

// NEW Option A — addUser in RealmConfig (REQUIRED when user needs roles)
@InjectRealm(config = TestRealmConfig.class)
ManagedRealm realm;

public static class TestRealmConfig implements RealmConfig {
    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        return realm
            .roles("admin")  // create the role first
            .addUser("testuser").password("pass").roles("admin").build();
    }
}

// NEW Option B — @InjectUser (only for users WITHOUT roles)
@InjectUser(config = TestUserConfig.class)
ManagedUser testUser;

public static class TestUserConfig implements UserConfig {
    @Override
    public UserConfigBuilder configure(UserConfigBuilder user) {
        return user.username("testuser").password("pass");
        // Do NOT call .roles() or .clientRoles() here — it will throw!
    }
}

// NEW Option C — realm.addUser() (for dynamic users in tests, supports roles via realm import)
realm.addUser(UserConfigBuilder.create()
    .username("testuser").password("pass").roles("admin"));
// Auto-cleanup is registered

// NEW Option D — AdminApiUtil (when you need the userId back, roles assigned separately)
UserRepresentation user = UserConfigBuilder.create()
    .username("testuser").password("pass").build();
String userId = AdminApiUtil.createUserWithAdminClient(realm.admin(), user);
// Assign roles separately via admin API:
realm.admin().users().get(userId).roles().realmLevel()
    .add(List.of(realm.admin().roles().get("admin").toRepresentation()));
realm.cleanup().add(r -> r.users().delete(userId));
```

**Summary — where roles work:**

| Method | `roles()` / `clientRoles()` | Why |
|---|---|---|
| `RealmConfigBuilder.addUser()` | **Supported** | User created via realm import |
| `realm.addUser(UserConfigBuilder)` | **Supported** | User created via realm import |
| `@InjectUser` with `UserConfig` | **NOT supported** — throws | User created via Users Resource API |
| `AdminApiUtil.createUserWithAdminClient()` | **NOT supported** | User created via Users Resource API, assign roles separately |

---

## Rule: Client Configuration

```java
// OLD — ClientBuilder in configureTestRealm
RealmBuilder.edit(testRealm).client(ClientBuilder.create()
    .clientId("my-client").secret("secret1")
    .serviceAccountsEnabled(true).directAccessGrantsEnabled(true));

// NEW Option A — inline in RealmConfig (preferred for clients the realm needs)
public static class MyRealmConfig implements RealmConfig {
    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        realm.addClient("my-client")
            .secret("secret1")
            .serviceAccountsEnabled(true)
            .directAccessGrantsEnabled(true);
        return realm;
    }
}

// NEW Option B — separate @InjectClient (when test needs direct client access)
@InjectClient(config = MyClientConfig.class)
ManagedClient client;

public static class MyClientConfig implements ClientConfig {
    @Override
    public ClientConfigBuilder configure(ClientConfigBuilder client) {
        return client.clientId("my-client").secret("secret1")
            .serviceAccountsEnabled(true).directAccessGrantsEnabled(true);
    }
}
```

**Note on `addClient()` / `addUser()` / `addGroup()`**: These methods on `RealmConfigBuilder` immediately add the object to the realm and return the nested builder (e.g., `ClientConfigBuilder`). Configure the nested builder, then continue with the next `realm.addX()` call. Do NOT call `.build()` expecting it to return back to `RealmConfigBuilder` — `.build()` returns the raw representation.

---

## Rule: JUnit 4 Assertion Message Order

JUnit 4 puts the message **first**, JUnit 6 puts it **last**:

```java
// OLD (JUnit 4 — message first)
Assert.assertEquals("User count should be 3", 3, users.size());
Assert.assertTrue("Should be enabled", user.isEnabled());

// NEW (JUnit 6 — message last)
assertEquals(3, users.size(), "User count should be 3");
assertTrue(user.isEnabled(), "Should be enabled");
```

---

## Rule: RealmConfigBuilder Method Reference

When converting `RealmRepresentation` setter calls to `RealmConfigBuilder` builder methods:

| `RealmRepresentation` setter | `RealmConfigBuilder` method |
|---|---|
| `setRealm(name)` | `name(name)` |
| `setEnabled(b)` | *(auto-set to true)* |
| `setEventsEnabled(b)` | `eventsEnabled(b)` |
| `setAdminEventsEnabled(b)` | `adminEventsEnabled(b)` |
| `setAdminEventsDetailsEnabled(b)` | `adminEventsDetailsEnabled(b)` |
| `setDefaultSignatureAlgorithm(a)` | `defaultSignatureAlgorithm(a)` |
| `setRegistrationAllowed(b)` | `registrationAllowed(b)` |
| `setRegistrationEmailAsUsername(b)` | `registrationEmailAsUsername(b)` |
| `setEditUsernameAllowed(b)` | `editUsernameAllowed(b)` |
| `setResetPasswordAllowed(b)` | `resetPasswordAllowed(b)` |
| `setRememberMe(b)` | `setRememberMe(b)` |
| `setInternationalizationEnabled(b)` | `internationalizationEnabled(b)` |
| `setSupportedLocales(set)` | `supportedLocales(String...)` |
| `setDefaultLocale(l)` | `defaultLocale(l)` |
| `setBruteForceProtected(b)` | `bruteForceProtected(b)` |
| `setFailureFactor(n)` | `failureFactor(n)` |
| `setDuplicateEmailsAllowed(b)` | `duplicateEmailsAllowed(b)` |
| `setSslRequired(s)` | `sslRequired(s)` |
| `setOrganizationsEnabled(b)` | `organizationsEnabled(b)` |
| `setRevokeRefreshToken(b)` | `revokeRefreshToken(b)` |
| `setRefreshTokenMaxReuse(n)` | `refreshTokenMaxReuse(n)` |
| `setSsoSessionIdleTimeout(n)` | `ssoSessionIdleTimeout(n)` |
| `setSsoSessionMaxLifespan(n)` | `ssoSessionMaxLifespan(n)` |
| `setClientSessionMaxLifespan(n)` | `clientSessionMaxLifespan(n)` |
| `setClientSessionIdleTimeout(n)` | `clientSessionIdleTimeout(n)` |
| `setBrowserFlow(f)` | `browserFlow(f)` |
| `setAdminPermissionsEnabled(b)` | `adminPermissionsEnabled(b)` |
| `setDisplayName(s)` | `displayName(s)` |
| `setEnabledEventTypes(list)` | `enabledEventTypes(String...)` or `setEnabledEventTypes(String...)` |
| `setEventsListeners(list)` | `eventsListeners(String...)` (adds) or `overwriteEventsListeners(String...)` (replaces) |
| `setEventsExpiration(n)` | `eventsExpiration(n)` |
| `setSmtpServer(map)` | `smtp(host, port, from)` |
| `setVerifiableCredentialsEnabled(b)` | `verifiableCredentialsEnabled(b)` |
| `setScimApiEnabled(b)` | `scimEnabled(b)` |
| `setSsoSessionIdleTimeoutRememberMe(n)` | `ssoSessionIdleTimeoutRememberMe(n)` |
| `setSsoSessionMaxLifespanRememberMe(n)` | `ssoSessionMaxLifespanRememberMe(n)` |
| `setId(s)` | `id(s)` |
| `setGroups(list)` | `groups(String...)` |
| `setDefaultGroups(list)` | `defaultGroups(String...)` |
| `setIdentityProviders(list)` | `identityProvider(IdentityProviderRepresentation)` (call per-provider) |
| `setIdentityProviderMappers(list)` | `identityProviderMapper(IdentityProviderMapperRepresentation)` (call per-mapper) |
| `setRequiredActions(list)` | `requiredAction(RequiredActionProviderRepresentation)` (call per-action) |
| *(client policies)* | `clientPolicy(ClientPolicyRepresentation)`, `clientProfile(ClientProfileRepresentation)`, `resetClientPolicies()`, `resetClientProfiles()` |

If the setter you need isn't listed, check `test-framework/core/src/main/java/org/keycloak/testframework/realm/RealmConfigBuilder.java`.

---

## Rule: Unrecognized Patterns

If you encounter any code pattern that doesn't match any rule:
1. Do NOT silently drop it
2. Do NOT guess at a transformation
3. Leave the code as-is and add a comment: `// TODO MIGRATION: manual review needed — <description of what this code does>`
4. Report it to the user at the end
