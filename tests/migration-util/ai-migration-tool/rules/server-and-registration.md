# Server URL, Feature Flags, ClientRegistration & TestingClient Migration Rules

Read this file when the legacy test uses `suiteContext`, `@EnableFeature`/`@DisableFeature`, `ClientRegistration`, `KeycloakServerConfig`, `testingClient`, `@ModelTest`, or `@SetDefaultProvider`.

---

## Server URL Access

When the legacy test uses `suiteContext.getAuthServerInfo().getContextRoot()` to get the Keycloak server URL:

```java
// OLD
String baseUrl = suiteContext.getAuthServerInfo().getContextRoot() + "/auth";

// NEW
@InjectKeycloakUrls
KeycloakUrls keycloakUrls;

// In code:
String baseUrl = keycloakUrls.getBaseUrl().toString();
```

**Imports:**
```
org.keycloak.testframework.annotations.InjectKeycloakUrls
org.keycloak.testframework.server.KeycloakUrls
```

---

## ClientRegistration

When the legacy test creates a `ClientRegistration` instance:

```java
// OLD (from AbstractClientRegistrationTest)
reg = ClientRegistration.create().url(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", "test").build();

// NEW
reg = ClientRegistration.create()
        .url(keycloakUrls.getBaseUrl().toString(), managedRealm.getName())
        .build();
```

The new `OAuthClient` also provides a `clientRegistration()` convenience method if injected:
```java
@InjectOAuthClient
OAuthClient oauthClient;

// Creates ClientRegistration pre-configured with base URL and realm
ClientRegistration reg = oauthClient.clientRegistration();
```

---

## Feature Flags & Server Config

`KeycloakServerConfig` configures the Keycloak server for a test class. Use `@KeycloakIntegrationTest(config = ...)`.

### @EnableFeature / @DisableFeature → features() / featuresDisabled()

Use `features()` and `featuresDisabled()` with `Profile.Feature` enum — NOT `option()`:

```java
// OLD
@EnableFeature(Feature.ADMIN_FINE_GRAINED_AUTHZ)
@DisableFeature(Feature.IMPERSONATION)
public class MyTest extends AbstractKeycloakTest { }

// NEW
@KeycloakIntegrationTest(config = MyTest.ServerConfig.class)
public class MyTest {
    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            return builder
                .features(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)
                .featuresDisabled(Profile.Feature.IMPERSONATION);
        }
    }
}
```

### KeycloakServerConfigBuilder API Reference

| Method | Purpose | Example |
|---|---|---|
| `features(Profile.Feature...)` | Enable features | `features(Profile.Feature.OID4VC_VCI)` |
| `featuresDisabled(Profile.Feature...)` | Disable features | `featuresDisabled(Profile.Feature.IMPERSONATION)` |
| `option(key, value)` | Set CLI option or select SPI provider | `option("spi-group-jpa-searchable-attributes", "attr1")` |
| `spiOption(spi, provider, key, value)` | Configure SPI provider settings | `spiOption("cors", "default", "allowed-headers", "x-trace-id")` |
| `dependency(groupId, artifactId)` | Deploy Maven dependency JAR | `dependency("org.keycloak.tests", "keycloak-tests-custom-providers")` |
| `dependency(groupId, artifactId, hotDeployable)` | Deploy with hot-deploy flag | `dependency("g", "a", true)` |
| `dependencyCurrentProject()` | Deploy current project as dependency | `dependencyCurrentProject()` |
| `cache(CacheType)` | Set cache type (`LOCAL` or `ISPN`) | `cache(CacheType.LOCAL)` |
| `externalInfinispanEnabled(boolean)` | Connect to managed external Infinispan | `externalInfinispanEnabled(true)` |
| `options(Map<String,String>)` | Set multiple CLI options at once | `options(Map.of("k","v"))` |
| `bootstrapAdminClient(id, secret)` | Configure admin client | |
| `bootstrapAdminUser(user, pass)` | Configure admin user | |
| `shutdownDelay(String)` | Set shutdown delay | `shutdownDelay("10s")` |
| `shutdownTimeout(String)` | Set shutdown timeout | `shutdownTimeout("30s")` |
| `log()` | Returns `LogBuilder` for logging config | `log().handlers(LogHandlers.SYSLOG).categoryLevel("org.keycloak", "DEBUG")` |

### @SetDefaultProvider → option() / spiOption()

```java
// OLD
@SetDefaultProvider(spi = "saml-artifact-resolver", providerId = "0005")

// NEW
builder.option("spi-saml-artifact-resolver-provider", "0005");

// With additional config
builder.option("spi-security-profile-provider", "default")
    .spiOption("security-profile", "default", "name", "lax-security-profile");
```

**When to use `option()` vs `spiOption()`:**
- Select which provider: `option("spi-<spi>-provider", "providerId")`
- Configure provider settings: `spiOption(spi, provider, key, value)`
- Non-SPI CLI options: `option(key, value)`

**Always search for existing examples** before writing SPI configuration from scratch — there are many patterns already in the tests module:

```bash
grep -rn "spiOption\|option(\"spi-" tests/base/src/test/java --include="*.java"
```

**Concrete examples from existing tests:**

```java
// Select SPI provider (option format: "spi-<spi>-provider")
config.option("spi-hostname-provider", "fixed");

// Configure SPI provider settings (spiOption format: spi, provider, key, value)
config.spiOption("cors", "default", "allowed-headers", "uber-trace-id,x-b3-traceid");
config.spiOption("security-profile", "default", "name", "lax-security-profile");

// SPI settings via option() — flat key format: "spi-<spi>--<provider>--<key>"
config.option("spi-connections-http-client-default-socket-timeout-millis", "2222");
config.option("spi-password-hashing-pbkdf2-max-padding-length", "14");
config.option("spi-user-profile-declarative-user-profile-admin-read-only-attributes", "deniedSomeAdmin");

// JPA searchable attributes
config.option("spi-client-jpa-searchable-attributes", "attr1,attr2");
config.option("spi-group-jpa-searchable-attributes", "attr1,attr2");

// Metrics counter tags
config.option("spi-credential-keycloak-password-validations-counter-tags", "realm");

// Workflow SPI
config.option("spi-workflow--default--executor-blocking", "true");
config.option("spi-events-listener--" + FactoryId + "--step-runner-task-interval", "1s");

// Login protocol SPI (nested key format)
config.option("spi-login-protocol--oidc--max-request-size", "3000");
```

### Deploying custom providers

```java
// OLD: ShrinkWrap deployment via Arquillian
// NEW:
builder.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
```

Use the shared `CustomProvidersServerConfig` class if it exists in `tests/base/`, or create an inline `ServerConfig`.

If a required provider is NOT yet in `tests/custom-providers/`, **create it as part of the migration** (this is NOT a blocker). See `specs/custom-providers.md` for the full catalog of migrated vs not-yet-migrated providers.

### Step-by-step: Creating a missing custom provider

1. **Locate the legacy provider** in `testsuite/integration-arquillian/servers/auth-server/services/testsuite-providers/src/main/java/org/keycloak/testsuite/`

2. **Check if it already exists in the new module** — search by class name in `tests/custom-providers/src/main/java/`. An equivalent may exist with a different name or in a different subpackage.

3. **Copy the provider + factory classes** to `tests/custom-providers/src/main/java/org/keycloak/tests/providers/<subpackage>/`
   - **Rewrite the package**: `org.keycloak.testsuite.<subpackage>` → `org.keycloak.tests.providers.<subpackage>`
   - Keep the same subpackage structure (e.g., `forms/`, `authentication/`, `actions/`)
   - Copy BOTH the provider class AND its factory class — they come as a pair

4. **Register the factory** in the appropriate SPI services file at `tests/custom-providers/src/main/resources/META-INF/services/<SPI-interface>`:
   - `org.keycloak.authentication.AuthenticatorFactory` — for authenticators
   - `org.keycloak.authentication.RequiredActionFactory` — for required actions
   - `org.keycloak.broker.provider.IdentityProviderFactory` — for identity providers
   - `org.keycloak.storage.UserStorageProviderFactory` — for user federation
   - Add the **fully qualified new class name** (e.g., `org.keycloak.tests.providers.forms.PassThroughAuthenticator`)
   - Create the services file if it doesn't exist for this SPI

5. **Build**: `./mvnw install -pl tests/custom-providers -DskipTests`

6. **Use `dependency()`** in the test's server config to deploy the provider:
   ```java
   builder.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
   ```

### Important notes
- The provider's `PROVIDER_ID` constant must stay the same — it's referenced in flow configurations and realm data
- If the provider references `testingClient` or Arquillian infrastructure, those dependencies must be removed
- If the provider imports from `org.keycloak.testsuite.util.*`, check if the utility exists in `tests/utils-shared/` (see `rules/utilities.md`)

**Imports:**
```
org.keycloak.testframework.server.KeycloakServerConfig
org.keycloak.testframework.server.KeycloakServerConfigBuilder
org.keycloak.common.Profile
```

---

## testingClient Replacement

The legacy `testingClient` (`KeycloakTestingClient`) is NOT available in the new framework. Each of its capabilities has a specific replacement:

| Legacy Pattern | New Replacement |
|---|---|
| `testingClient.testing().setTimeOffset(...)` | `@InjectTimeOffSet TimeOffSet` → `timeOffSet.set(n)` |
| `testingClient.testing().pollEvent()` | `@InjectEvents Events` → `events.poll()` |
| `testingClient.testing().pollAdminEvent()` | `@InjectAdminEvents AdminEvents` → `adminEvents.poll()` |
| `testingClient.testing().clearEventQueue()` | `events.clear()` |
| `testingClient.testing().removeUserSessions(realm)` | Use `RunOnServerClient` (see below) or admin API |
| `testingClient.server().run(session -> ...)` | `@InjectRunOnServer RunOnServerClient` → `runOnServer.run(session -> ...)` |
| `testingClient.server().fetch(session -> ..., T.class)` | `runOnServer.fetch(session -> ..., T.class)` |
| `testingClient.testApp()` | `@InjectTestApp TestApp testApp` — mock HTTP callback server (provides redirect URI for OAuth flows; auto-injected as OAuthClient dependency, so explicit injection is rarely needed) |

### RunOnServer examples

```java
@InjectRunOnServer
RunOnServerClient runOnServer;

// Execute code on server (no return value)
runOnServer.run(session -> {
    RealmModel realm = session.realms().getRealm(realmId);
    session.sessions().removeUserSessions(realm);
});

// Fetch data from server (with return value)
List<String> aliases = runOnServer.fetch(session ->
    session.identityProviders().getAllStream()
        .map(IdentityProviderModel::getAlias)
        .toList(),
    List.class);
```

Use `@InjectRunOnServer(realmRef = "myRealm")` to bind to a specific realm.

**Imports:**
```
org.keycloak.testframework.remote.runonserver.InjectRunOnServer
org.keycloak.testframework.remote.runonserver.RunOnServerClient
```

---

## @ModelTest

`@ModelTest` must be rewritten. There are two options:

**Option A — `@TestOnServer` (preferred)**: Direct replacement. The method keeps its `KeycloakSession` parameter and runs on the server. Use this when the entire test method is server-side code.

```java
// OLD
@ModelTest
public void testWithSession(KeycloakSession session) {
    RealmModel realm = session.realms().getRealmByName("test");
    // ... model-layer testing
}

// NEW — @TestOnServer (replaces both @Test and @ModelTest)
@TestOnServer
void testWithSession(KeycloakSession session) {
    RealmModel realm = session.realms().getRealmByName("test");
    // ... model-layer testing
}
```

Import: `org.keycloak.testframework.remote.annotations.TestOnServer`

**Option B — `RunOnServerClient`**: Use when you need to mix client-side and server-side code in the same test method, or when only part of the test runs on the server.

```java
// NEW — RunOnServerClient (for mixed client/server tests)
@InjectRunOnServer
RunOnServerClient runOnServer;

@Test
void testWithSession() {
    // client-side setup...
    runOnServer.run(session -> {
        RealmModel realm = session.realms().getRealmByName("test");
        // ... model-layer testing
    });
    // client-side assertions...
}
```

---

## @SetDefaultProvider

```java
// OLD
@SetDefaultProvider(spi = "hostname", providerId = "fixed")
public class MyTest extends AbstractKeycloakTest { ... }

// NEW — use KeycloakServerConfig
@KeycloakIntegrationTest(config = MyTest.ServerConfig.class)
public class MyTest {
    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            return builder.option("spi-hostname-provider", "fixed");
        }
    }
}
```

---

## Utility Class Migration

When the legacy test uses utility classes from `testsuite/integration-arquillian/tests/base/src/main/java/`:

1. **Check `rules/utilities.md`** first — it has a complete mapping table
2. **Check if the utility exists in the new module**: search `tests/utils/` and `tests/utils-shared/`
3. **If it exists**: update the import package
4. **If it doesn't exist and the usage is simple** (1-3 lines): inline the logic directly. Example:
   ```java
   // OLD: TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.ES256)
   // This just sets a realm property — inline it:
   managedRealm.updateWithCleanup(r -> r.defaultSignatureAlgorithm(Algorithm.ES256));
   ```
5. **If it doesn't exist and the usage is complex**: keep the utility reference and add `// TODO MIGRATION: utility class not available in new module`

---

## Authentication Flow Testing with RunOnServer

When the legacy test uses `testingClient.server().run()` with `FlowUtil`:

```java
// OLD
testingClient.server("test").run(session ->
    FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));

testingClient.server("test").run(session ->
    FlowUtil.inCurrentRealm(session)
        .selectFlow(newFlowAlias)
        .inForms(forms -> forms.clear()
            .addAuthenticatorExecution(Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID))
        .defineAsBrowserFlow());

// NEW
@InjectRunOnServer
RunOnServerClient runOnServer;

runOnServer.run(session ->
    FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));

runOnServer.run(session ->
    FlowUtil.inCurrentRealm(session)
        .selectFlow(newFlowAlias)
        .inForms(forms -> forms.clear()
            .addAuthenticatorExecution(Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID))
        .defineAsBrowserFlow());
```

`FlowUtil` is in `tests/utils-shared/` — same import, same API: `org.keycloak.testsuite.util.FlowUtil`
