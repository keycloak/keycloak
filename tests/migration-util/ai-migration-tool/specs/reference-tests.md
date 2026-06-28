# Reference Tests for Complex Migrations

When migrating a complex test, find and read a similar already-migrated test first. These are the best references organized by pattern complexity.

---

## By Pattern

### UI + OAuthClient + Page Objects + Events

| Test | File | Patterns Used |
|---|---|---|
| `LoginEventsTest` | `tests/base/.../admin/event/LoginEventsTest.java` | `@InjectOAuthClient`, `@InjectPage LoginPage`, `@InjectEvents`, `@InjectRunOnServer`, `openLoginForm()`, `fillLoginForm()` |
| `ConcurrentLoginTest` | `tests/base/.../admin/concurrency/ConcurrentLoginTest.java` | `@InjectOAuthClient`, `@InjectWebDriver`, `@InjectRunOnServer`, `@InjectUser`, `@InjectKeycloakUrls`, `LifeCycle.METHOD` realm |
| `ImpersonationTest` | `tests/base/.../admin/ImpersonationTest.java` | `@InjectOAuthClient`, `@InjectWebDriver`, `@InjectPage`, `@InjectEvents`, `@InjectRunOnServer` |

### RunOnServer + FlowUtil (authentication flow tests)

| Test | File | Patterns Used |
|---|---|---|
| `AuthenticatorConfigTest` | `tests/base/.../admin/authentication/AuthenticatorConfigTest.java` | `@InjectRunOnServer`, extends `AbstractAuthenticationTest`, `AdminEvents` |
| `LoginEventsTest` | `tests/base/.../admin/event/LoginEventsTest.java` | `@InjectRunOnServer`, `FlowUtil` usage |

### KeycloakServerConfig (features, custom providers, SPI options)

| Test | File | Config Pattern |
|---|---|---|
| `InstallationTest` | `tests/base/.../admin/client/InstallationTest.java` | `features(AUTHORIZATION)` |
| `ClientSearchTest` | `tests/base/.../admin/client/ClientSearchTest.java` | `option("spi-client-jpa-searchable-attributes", ...)` |
| `TracingTest` | `tests/base/.../admin/tracing/TracingTest.java` | `option("tracing-enabled", "true")` + SPI options |
| `PasswordValidationMetricTest` | `tests/base/.../admin/metric/PasswordValidationMetricTest.java` | `option("metrics-enabled", "true")` |
| `SMTPConnectionVaultTest` | `tests/base/.../admin/SMTPConnectionVaultTest.java` | `option("vault", "file")` |
| `LaxSecurityProfileTest` | `tests/base/.../securityprofile/LaxSecurityProfileTest.java` | `spiOption("security-profile", ...)` |
| `CustomCorsAllowedHeadersTest` | `tests/base/.../cors/CustomCorsAllowedHeadersTest.java` | `spiOption("cors", "default", ...)` |
| `RequiredActionsTest` | `tests/base/.../admin/authentication/RequiredActionsTest.java` | `dependency("org.keycloak.tests", "keycloak-tests-custom-providers")` |

### AdminClientFactory (multiple admin clients)

| Test | File | Patterns Used |
|---|---|---|
| `AbstractFineGrainedAdminTest` | `tests/base/.../admin/finegrainedadminv1/AbstractFineGrainedAdminTest.java` | `@InjectAdminClientFactory`, multiple admin clients with different permissions |
| `FineGrainedAdminWithTokenExchangeTest` | `tests/base/.../admin/finegrainedadminv1/...` | `@InjectAdminClientFactory` + features |

### ManagedRealm lifecycle patterns

| Test | File | Pattern |
|---|---|---|
| `ConcurrentLoginTest` | See above | `@InjectRealm(lifecycle = LifeCycle.METHOD)` — realm recreated per test |
| `AbstractUserTest` | `tests/base/.../admin/user/AbstractUserTest.java` | `realm.addUser()`, `realm.updateUser()`, `realm.cleanup()` |

### @InjectUser and user management

| Test | File | Patterns Used |
|---|---|---|
| `ConcurrentLoginTest` | See above | Multiple `@InjectUser` with different configs |
| `AbstractUserTest` | See above | `realm.addUser()` for dynamic users with roles |

### Mail server

| Test | File | Patterns Used |
|---|---|---|
| `SMTPConnectionVaultTest` | See above | `@InjectMailServer` |

---

## How to Find More References

```bash
# Find tests using a specific injection
grep -rln "InjectOAuthClient" tests/base/src/test/java/

# Find tests using a combination
grep -rln "InjectPage" tests/base/src/test/java/ | xargs grep -l "InjectEvents"

# Find tests using server config
grep -rln "implements KeycloakServerConfig" tests/base/src/test/java/

# Find tests using FlowUtil
grep -rln "FlowUtil" tests/base/src/test/java/

# Find tests using a specific base class
grep -rln "extends AbstractRealmTest" tests/base/src/test/java/
```

---

## Anatomy of a Well-Migrated Complex Test

```java
@KeycloakIntegrationTest(config = MyTest.ServerConfig.class)  // if server config needed
public class MyTest {

    // 1. Realm — minimal config, not fromJson
    @InjectRealm(config = MyRealmConfig.class)
    ManagedRealm realm;

    // 2. OAuthClient — auto-creates client, auto-wires WebDriver
    @InjectOAuthClient
    OAuthClient oauthClient;

    // 3. WebDriver — only if test interacts with browser directly
    @InjectWebDriver
    ManagedWebDriver webDriver;

    // 4. Pages — only what the test actually uses
    @InjectPage
    LoginPage loginPage;

    // 5. Events — only if test asserts events
    @InjectEvents
    Events events;

    // 6. RunOnServer — only if test needs server-side code (FlowUtil, etc.)
    @InjectRunOnServer
    RunOnServerClient runOnServer;

    // 7. Server config — features, vault, custom providers
    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }

    // 8. Realm config — minimal, only what the test needs
    public static class MyRealmConfig implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.eventsEnabled(true);
            realm.addUser("testuser").password("password").roles("user");
            return realm;
        }
    }

    // 9. Setup — cookie clearing for UI tests, password reset if needed
    @BeforeEach
    void setup() {
        webDriver.cookies().deleteAll();
    }

    // 10. Tests — no `public`, use Assertions/assertThat
    @Test
    void testSomething() {
        oauthClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();
        // ...
    }
}
```
