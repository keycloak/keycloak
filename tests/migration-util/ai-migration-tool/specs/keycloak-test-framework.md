# Keycloak Test Framework — Migration Reference

**Module**: `keycloak-test-framework-parent`
**Location**: `test-framework/`
**Total**: 333 Java files across 20 submodules

---

## 1. All Injection Annotations

### 1.1 Core Annotations

| Annotation | Injected Type | Default Lifecycle | Config Interface |
|-----------|--------------|-------------------|-----------------|
| `@InjectRealm` | `ManagedRealm` | CLASS | `RealmConfig` | Attributes: `ref`, `config`, `lifecycle`, `fromJson` (load from JSON file), `attachTo` (attach to existing realm e.g. "master") |
| `@InjectUser` | `ManagedUser` | CLASS | `UserConfig` | Attributes: `ref`, `config`, `lifecycle`, `realmRef` |
| `@InjectClient` | `ManagedClient` | CLASS | `ClientConfig` | Attributes: `ref`, `config`, `lifecycle`, `realmRef`, `attachTo` |
| `@InjectAdminClient` | `Keycloak` | CLASS | — | Attributes: `ref`, `realmRef`, `mode` (BOOTSTRAP or MANAGED_REALM), `client`, `user` |
| `@InjectAdminClientFactory` | `AdminClientFactory` | GLOBAL | — |
| `@InjectTestDatabase` | `TestDatabase` | GLOBAL | `DatabaseConfig` |
| `@InjectKeycloakUrls` | `KeycloakUrls` | GLOBAL | — |
| `@InjectEvents` | `Events` | CLASS | — |
| `@InjectAdminEvents` | `AdminEvents` | CLASS | — |
| `@InjectHttpClient` | `CloseableHttpClient` | CLASS | — |
| `@InjectHttpServer` | `HttpServer` | CLASS | — |
| `@InjectSimpleHttp` | `SimpleHttp` | CLASS | — |
| `@InjectCryptoHelper` | `CryptoHelper` | GLOBAL | — |
| `@InjectCertificates` | `ManagedCertificates` | GLOBAL | `CertificatesConfig` |
| `@InjectSysLogServer` | `SysLogServer` | CLASS | — |
| `@InjectInfinispanServer` | `InfinispanServer` | GLOBAL | — |
| `@InjectDependency` | *(any managed resource)* | — | — | Used inside Config classes to inject dependencies (e.g., `ManagedRealm` into a `ClientConfig`). Package: `o.k.testframework.annotations` |

### 1.2 OAuth Module Annotations (package: `org.keycloak.testframework.oauth.annotations`)

| Annotation | Injected Type | Config Interface |
|-----------|--------------|-----------------|
| `@InjectOAuthClient` | `OAuthClient` | `ClientConfig` | Attributes: `ref`, `realmRef`, `kcAdmin` (boolean, admin mode) |
| `@InjectTestApp` | `TestApp` | — |
| `@InjectOAuthIdentityProvider` | `OAuthIdentityProvider` | `OAuthIdentityProviderConfig` |
| `@InjectCimdProvider` | `CimdProvider` | — |

### 1.3 UI Module Annotations (package: `org.keycloak.testframework.ui.annotations`)

| Annotation | Injected Type | Notes |
|-----------|--------------|-------|
| `@InjectWebDriver` | `ManagedWebDriver` | Browser type via config: `kc.test.browser` |
| `@InjectPage` | `<T extends AbstractPage>` | Page object injection |

### 1.4 Remote Module Annotations (package: `org.keycloak.testframework.remote.*`)

| Annotation | Injected Type | Notes |
|-----------|--------------|-------|
| `@InjectRemoteProviders` | `RemoteProviders` | `o.k.testframework.remote.InjectRemoteProviders` |
| `@InjectRunOnServer` | `RunOnServerClient` | `o.k.testframework.remote.runonserver.InjectRunOnServer` |
| `@InjectTestClassServer` | `TestClassServer` | `o.k.testframework.remote.runonserver.InjectTestClassServer` |
| `@InjectTimeOffSet` | `TimeOffSet` | `o.k.testframework.remote.timeoffset.InjectTimeOffSet` |

### 1.5 Clustering Module Annotations

| Annotation | Injected Type | Notes |
|-----------|--------------|-------|
| `@InjectLoadBalancer` | `LoadBalancer` | HTTP reverse proxy for cluster |

### 1.6 Email Module Annotations (package: `org.keycloak.testframework.mail`)

| Annotation | Injected Type | Notes |
|-----------|--------------|-------|
| `@InjectMailServer` | `MailServer` | `o.k.testframework.mail.annotations.InjectMailServer` |

### 1.7 Lifecycle Annotations (Method-level)

| Annotation | When | Purpose |
|-----------|------|---------|
| `@TestSetup` | After injection, before test methods | Setup with injected resources |
| `@TestCleanup` | After all test methods | Cleanup |
| `@TestOnServer` | Replaces `@Test` | Run test method on server with `KeycloakSession` param. Direct replacement for legacy `@ModelTest`. Package: `o.k.testframework.remote.annotations` |

### 1.8 Conditional Annotations

| Annotation | Purpose |
|-----------|---------|
| `@DisabledForDatabases` | Skip test for specific database types |
| `@DisabledForServers` | Skip test for specific server types |

---

## 2. Configuration Pattern (Builder + Interface)

Every configurable resource follows this pattern:

```java
public class MyRealmConfig implements RealmConfig {
    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        return realm.name("my-realm").roles("admin", "user").eventsEnabled(true);
    }
}

@InjectRealm(config = MyRealmConfig.class)
ManagedRealm realm;
```

**Config Interfaces and Builders:**

| Interface | Builder | Purpose |
|----------|---------|---------|
| `RealmConfig` | `RealmConfigBuilder` | Realm configuration |
| `UserConfig` | `UserConfigBuilder` | User creation |
| `ClientConfig` | `ClientConfigBuilder` | OAuth2 client |
| `DatabaseConfig` | `DatabaseConfigBuilder` | Database settings |
| `KeycloakServerConfig` | `KeycloakServerConfigBuilder` | Server startup options |
| `CertificatesConfig` | `CertificatesConfigBuilder` | TLS certificates |
| `OAuthIdentityProviderConfig` | `OAuthIdentityProviderConfigBuilder` | IdP settings |

**Nested builders**: `RealmConfigBuilder.addClient("id")` returns `ClientConfigBuilder`; the object is immediately added to the realm's list. Configure the nested builder, then call `realm.addX()` again for the next object. `.build()` returns the raw representation (e.g., `ClientRepresentation`), NOT back to `RealmConfigBuilder`. Same for `addUser()` → `UserConfigBuilder`, `addGroup()` → `GroupConfigBuilder`, `addRole()` → `RoleConfigBuilder`.

**Config classes can inject dependencies:**
```java
public class MyUserConfig implements UserConfig {
    @InjectDependency
    ManagedRealm realm;  // Injected before configure() is called
}
```

**Additional Builders:** `GroupConfigBuilder`, `RoleConfigBuilder`, `AuthenticationFlowConfigBuilder`, `AuthenticationExecutionExportConfigBuilder`

---

## 3. Managed Resources

All managed resources share: `admin()` (returns the admin API resource), `cleanup()`, `dirty()`, `updateWithCleanup()`.

For complete method lists, read the source files directly:
- `test-framework/core/src/main/java/org/keycloak/testframework/realm/ManagedRealm.java`
- `test-framework/core/src/main/java/org/keycloak/testframework/realm/ManagedClient.java`
- `test-framework/core/src/main/java/org/keycloak/testframework/realm/ManagedUser.java`

**Non-obvious gotchas:**
- `updateWithCleanup()` takes functional interfaces (`RealmUpdate`, `ClientUpdate`, `UserUpdate`) that use **builder method names**, not setter names. E.g., `realm.updateWithCleanup(r -> r.defaultSignatureAlgorithm("ES256"))`.
- `addUser(UserConfigBuilder)` on `ManagedRealm` supports roles (unlike `@InjectUser`) and registers auto-cleanup.
- `dirty()` marks the resource for recreation after the current test method — use when tests modify flows or realm state.

### 3.4 Lifecycle Scopes

```java
enum LifeCycle {
    GLOBAL,   // Single instance for entire test suite (database, server)
    CLASS,    // Per test class (realms, users, clients) — DEFAULT
    METHOD    // Per test method (fine-grained isolation)
}
```

---

## 4. Events API

```java
@InjectEvents Events events;
@InjectAdminEvents AdminEvents adminEvents;

// Poll and assert
EventAssertion.assertSuccess(events.poll())
    .type(EventType.LOGIN).userId(userId).sessionId(sessionId)
    .details(Details.USERNAME, "user");

EventAssertion.assertError(events.poll())
    .type(EventType.LOGIN_ERROR).error(Errors.INVALID_REDIRECT_URI);

AdminEventAssertion.assertSuccess(adminEvents.poll())
    .operationType(OperationType.CREATE).resourceType(ResourceType.USER)
    .resourcePath("users", userId);

// Skip/clear
events.skip(); events.skip(3); events.skipAll(); events.clear();
```

**Key classes:** `EventAssertion`, `AdminEventAssertion`, `EventMatchers` (UUID/sessionId/codeId matchers only)
