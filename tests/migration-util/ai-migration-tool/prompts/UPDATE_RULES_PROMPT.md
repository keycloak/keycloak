# Keycloak Migration — Update Rules & Specs

This prompt verifies that all migration knowledge files (specs and rules) contain correct data by checking them against the actual codebase. Run this after pulling latest changes or before starting a migration session.

All paths below are relative to the Keycloak project root unless otherwise noted.

Spec files are at `tests/migration-util/ai-migration-tool/specs/`.
Rule files are at `tests/migration-util/ai-migration-tool/rules/`.

---

## Step 0: Prerequisite — Ensure project is built

Check if `tests/base/target/test-classes` exists. If not, run:

```bash
./mvnw clean install -DskipTests
```

This must succeed before proceeding. If it fails, fix the build errors first.

---

## Step 1: Verify directory structure

Confirm these directories exist and are accessible:
- `testsuite/integration-arquillian/tests/base/src/test/java/`
- `test-framework/`
- `tests/base/src/test/java/`

If any are missing, stop and report the error.

---

## Step 2: Update spec file counts and tables

### 2.1 `keycloak-tests-module.md`

1. **File counts**: Run `find tests/base/src/test/java -name "*Test.java" | wc -l` and `find tests -name "*.java" -not -path "*/target/*" | wc -l`. Compare against spec. Update if different.

2. **Submodule file counts**: Verify the file counts in the Section 1 table for each submodule:
   ```bash
   find tests/base/src -name "*.java" -not -path "*/target/*" | wc -l
   grep -rln "@KeycloakIntegrationTest" tests/base/src/test/java/ | wc -l
   find tests/utils/src -name "*.java" -not -path "*/target/*" | wc -l
   find tests/utils-shared/src -name "*.java" -not -path "*/target/*" | wc -l
   find tests/custom-providers/src -name "*.java" -not -path "*/target/*" | wc -l
   find tests/webauthn/src -name "*.java" -not -path "*/target/*" | wc -l
   ```

3. **Abstract base classes**: Run `find tests/base/src/test/java -name "Abstract*Test.java" | sed 's|.*/||'` and compare against Section 2. Add any new base classes (read them to determine their injected resources).

4. **Common config classes**: Check `tests/base/src/test/java/org/keycloak/tests/common/` for any new config classes not in Section 3.

5. **Test packages**: Run `find tests/base/src/test/java/org/keycloak/tests -type d | sed 's|.*/tests/||'` and compare against Section 6. Add any new packages.

### 2.2 `arquillian-testsuite.md`

1. **File counts**: Run `find testsuite/integration-arquillian/tests/base/src/test/java -name "*Test.java" | wc -l` and `find testsuite -name "*.java" -not -path "*/target/*" | wc -l`. Update if different.

2. **Base class hierarchy**: Verify the key base classes in Section 2 still exist and haven't been renamed.

### 2.3 `keycloak-test-framework.md`

1. **File count**: Run `find test-framework -name "*.java" -not -path "*/target/*" | wc -l`. Update if different.

2. **@Inject* annotations**: Search `test-framework/` for all `@interface Inject` declarations. Compare against Section 1. Add any new annotations.

3. **Config builders**: Run `find test-framework -name "*ConfigBuilder.java" -not -path "*/target/*"`. Verify Section 2 lists all of them.

4. **Managed resource methods**: Read `ManagedRealm.java`, `ManagedClient.java`, `ManagedUser.java` and verify Section 3 lists all public methods.

### 2.4 `page-objects.md`

1. **Available pages**: Run `ls test-framework/ui/src/main/java/org/keycloak/testframework/ui/page/*.java | sed 's|.*/||' | sed 's/.java//'`. Compare against the "Available in New Framework" table. Add any new pages.

2. **Page methods**: For any page that was recently created or modified, read it and verify the methods listed in the spec match the actual class.

### 2.5 `custom-providers.md`

1. **Migrated providers**: Run `find tests/custom-providers/src/main/java -name "*.java" -not -path "*/target/*" | sed 's|.*/java/||'`. Compare against the "Already in New Module" table. Add any new providers.

2. **SPI registrations**: Check all service files in `tests/custom-providers/src/main/resources/META-INF/services/` and verify they match the spec.

### 2.6 `base-class-flattening.md`

1. **New base classes in tests/**: Run `find tests/base/src/test/java -name "Abstract*Test.java" | sed 's|.*/||'` and verify any new base class that could replace a flattening recipe is noted.

2. **Legacy base classes**: Verify the key legacy base classes in the flattening recipes still exist in `testsuite/` and haven't changed their fields/methods significantly.

### 2.7 `reference-tests.md`

1. **Reference tests still exist**: Verify each referenced test file still exists at the listed path.

2. **New complex tests**: Search for recently migrated tests that use multiple injections (UI + events + flows) and add them if they provide better reference patterns:
   ```bash
   grep -rln "@InjectPage" tests/base/src/test/java/ | xargs grep -l "InjectOAuthClient\|InjectEvents" | head -10
   ```

### 2.8 `testrealm-json.md`

1. **JSON still matches**: Verify the user/client/role summary still matches the actual `tests/base/src/test/resources/org/keycloak/tests/testrealm.json`. Key things to check: user count, user credentials, client list, role list.

---

## Step 3: Extract and verify all import paths in rules

For each rule file in `rules/`, extract every Java import path (anything matching `org.keycloak.*` in code blocks). Then verify each resolves to an actual `.java` file.

```bash
# Extract all org.keycloak imports from rule files
grep -roh 'org\.keycloak\.[a-zA-Z0-9_.]*' tests/migration-util/ai-migration-tool/rules/ | sort -u
```

For each extracted path, verify it exists:
```bash
find test-framework/ tests/ -name "<ClassName>.java" -path "*/<expected/package/path>/*"
```

**Known critical paths to always verify** (these have drifted before):

| Import | Expected location |
|---|---|
| `o.k.testframework.oauth.annotations.InjectOAuthClient` | `test-framework/oauth/` |
| `o.k.testframework.ui.annotations.InjectWebDriver` | `test-framework/ui/` |
| `o.k.testframework.ui.annotations.InjectPage` | `test-framework/ui/` |
| `o.k.testframework.mail.annotations.InjectMailServer` | `test-framework/email-server/` |
| `o.k.testframework.remote.annotations.TestOnServer` | `test-framework/remote/` |
| `o.k.testframework.remote.runonserver.InjectRunOnServer` | `test-framework/remote/` |
| `o.k.testframework.remote.runonserver.RunOnServerClient` | `test-framework/remote/` |
| `o.k.testframework.remote.timeoffset.InjectTimeOffSet` | `test-framework/remote/` |
| `o.k.testframework.remote.timeoffset.TimeOffSet` | `test-framework/remote/` |
| `o.k.testframework.events.EventAssertion` | `test-framework/core/` |
| `o.k.testframework.events.AdminEventAssertion` | `test-framework/core/` |
| `o.k.testframework.annotations.InjectDependency` | `test-framework/core/` |
| `o.k.testframework.annotations.InjectSimpleHttp` | `test-framework/core/` |
| `o.k.testframework.admin.AdminClientFactory` | `test-framework/core/` |
| `o.k.testframework.ui.webdriver.ManagedWebDriver` | `test-framework/ui/` |
| `o.k.testframework.mail.MailServer` | `test-framework/email-server/` |
| `o.k.testframework.ui.webdriver.BrowserType` | `test-framework/ui/` |
| `o.k.testframework.oauth.TestApp` | `test-framework/oauth/` |
| `o.k.tests.utils.Assert` | `tests/utils/` |
| `o.k.tests.utils.admin.AdminApiUtil` | `tests/utils/` |
| `o.k.tests.utils.admin.AdminEventPaths` | `tests/utils/` |

Fix any that are wrong by searching for the class: `find test-framework/ tests/ -name "<ClassName>.java"`

---

## Step 4: Verify method signatures in code examples

For each rule file, identify code examples that call specific methods on framework classes. Verify the method exists with the shown signature.

### 4.1 Events API (`rules/events.md`)

Verify against `test-framework/core/src/main/java/org/keycloak/testframework/events/`:

1. `Events` / `AdminEvents` (extend `AbstractEvents`):
   - Must have: `poll()`, `skip()`, `skip(int)`, `skipAll()`, `clear()`
   - Must NOT have: `assertion()`, `events()`, `waitForEvent()`

2. `EventAssertion`:
   - Static: `assertSuccess(EventRepresentation)`, `assertError(EventRepresentation)`
   - Chain: `type(EventType)`, `userId(String)`, `sessionId(String)`, `clientId(String)`, `details(String, String)`, `withoutDetails(String...)`, `error(String)`, `hasSessionId()`, `hasIpAddress()`, `isCodeId()`

3. `AdminEventAssertion`:
   - Static: `assertSuccess(AdminEventRepresentation)`, `assertError(AdminEventRepresentation)`, `assertEvent(event, opType, path, resourceType)`, `assertEvent(event, opType, path, rep, resourceType)`
   - Chain: `operationType(OperationType)`, `resourceType(ResourceType)`, `resourcePath(String...)`, `representation(Object)`, `auth(String, String, String)`

4. `EventMatchers`:
   - Must have: `isUUID()`, `isCodeId()`, `isSessionId()`
   - Must NOT have: `isType()`, `isClient()` or any event-type matchers

### 4.2 OAuth API (`rules/oauth.md`)

Verify against TWO files:
- `tests/utils-shared/src/main/java/org/keycloak/testsuite/util/oauth/AbstractOAuthClient.java` — shared API
- `test-framework/oauth/src/main/java/org/keycloak/testframework/oauth/OAuthClient.java` — new OAuthClient (extends AbstractOAuthClient)

1. **`do*()` methods must return response directly** (NOT a request builder):
   - `doAccessTokenRequest(String code)` -> `AccessTokenResponse`
   - `doPasswordGrantRequest(String username, String password)` -> `AccessTokenResponse`
   - `doRefreshTokenRequest(String refreshToken)` -> `AccessTokenResponse`
   - `doLogin(String user, String pass)` -> `AuthorizationEndpointResponse`
   - `doLogout(String refreshToken)` -> `LogoutResponse`
   - `doClientCredentialsGrantAccessTokenRequest()` -> `AccessTokenResponse`
   - `doJWTAuthorizationGrantRequest(String assertion)` -> `AccessTokenResponse`
   - `doTokenExchange(String subjectToken)` -> `AccessTokenResponse`
   - `doTokenRevoke(String token)` -> `TokenRevocationResponse`
   - `doFetchExternalIdpToken(String alias, String token)` -> `AccessTokenResponse`
   - `doPushedAuthorizationRequest()` -> `ParResponse`
   - `doUserInfoRequest(String accessToken)` -> `UserInfoResponse`
   - `doIntrospectionAccessTokenRequest(String token)` -> `IntrospectionResponse`
   - `doIntrospectionRefreshTokenRequest(String token)` -> `IntrospectionResponse`
   - `doWellKnownRequest()` -> `OIDCConfigurationRepresentation`
   - `doBackchannelLogout(String logoutToken)` -> `BackchannelLogoutResponse`

2. **Builder methods must return request objects** (which have `.send()`):
   - `passwordGrantRequest(String, String)` -> `PasswordGrantRequest`
   - `accessTokenRequest(String)` -> `AccessTokenRequest`
   - `refreshRequest(String)` -> `RefreshRequest`
   - `introspectionRequest(String)` -> `IntrospectionRequest`
   - `logoutRequest()` -> `LogoutRequest`
   - `wellknownRequest()` -> `OpenIDProviderConfigurationRequest`
   - `userInfoRequest(String)` -> `UserInfoRequest`
   - `tokenRevocationRequest(String)` -> `TokenRevocationRequest`
   - `tokenExchangeRequest(String)` -> `TokenExchangeRequest`
   - `backchannelLogoutRequest(String)` -> `BackchannelLogoutRequest`
   - `clientCredentialsGrantRequest()` -> `ClientCredentialsGrantRequest`
   - `pushedAuthorizationRequest()` -> `ParRequest`
   - `permissionGrantRequest()` -> `PermissionGrantRequest`

3. **New OAuthClient-specific methods** (NOT on AbstractOAuthClient):
   - `clientRegistration()` -> `ClientRegistration`
   - `close()` -> removes the client via `ClientResource`
   - `fillLoginForm(String, String)` -> overrides abstract method, uses `LoginPage` internally
   - `parseLoginResponse()` -> overrides to wait for OAuth callback first

4. **Sub-clients**:
   - `ciba()` -> `CibaClient`
   - `device()` -> `DeviceClient`
   - `oid4vc()` -> `OID4VCClient`

5. **Token parsing/verification**:
   - `verifyToken(String)`, `verifyIDToken(String)`, `parseRefreshToken(String)`, `parseToken(String, Class)`

6. **Old-only methods that must NOT appear in new rules**:
   - `newConfig()` — replaced by `@InjectOAuthClient(webDriverRef = "second")`
   - `init()` — supplier handles initialization
   - `setDriver()` — supplier handles WebDriver wiring
   - `clientId(String)` — deprecated, use `client(String)`
   - `OAuthClient.AUTH_SERVER_ROOT` / `SERVER_ROOT` / `APP_ROOT` — use `@InjectKeycloakUrls`

7. Verify the rules do NOT show `.send()` appended to `do*()` methods.

### 4.2b OAuthClientSupplier architecture (`rules/oauth.md`)

Verify against `test-framework/oauth/src/main/java/org/keycloak/testframework/oauth/OAuthClientSupplier.java`:

1. Depends on `TestApp` (provides redirect URI), `ManagedRealm`, `ManagedWebDriver`, `HttpClient`
2. Creates a client with `redirectUris(testApp.getRedirectionUri())`
3. If `ref` is non-empty, appends `-<ref>` to the clientId
4. Holds `ClientResource` reference for cleanup
5. Verify rules state: use `ref` for unique clientIds, NOT custom `ClientConfig` with different clientId

### 4.3 ManagedRealm API (`rules/core.md`, `rules/lifecycle.md`)

Verify against `test-framework/core/src/main/java/org/keycloak/testframework/realm/ManagedRealm.java`:

1. `admin()` -> returns `RealmResource`
2. `getName()`, `getId()`, `getBaseUrl()`
3. `getCreatedRepresentation()` -> returns original `RealmRepresentation`
4. `updateWithCleanup(RealmUpdate...)` — verify `RealmUpdate` is `RealmConfigBuilder -> RealmConfigBuilder`
5. `addUser(UserConfigBuilder)`
6. `updateUser(String username, UserConfigBuilder.UserUpdate)`
7. `updateUserWithCleanup(String username, UserConfigBuilder.UserUpdate)`
8. `updateIdentityProvider(String alias, IdentityProviderUpdate)`
9. `updateComponent(String id, ComponentUpdate)`
10. `cleanup()` -> returns `ManagedRealmCleanup`
11. `dirty()` -> marks realm for recreation after test (inherited from `ManagedTestResource`)

### 4.4 RealmConfigBuilder API (`rules/core.md`)

Verify the RealmConfigBuilder method mapping table against `test-framework/core/src/main/java/org/keycloak/testframework/realm/RealmConfigBuilder.java`:

1. Check each listed builder method actually exists
2. Check for new builder methods NOT in the table — add any that correspond to commonly-used `RealmRepresentation` setters

### 4.5 KeycloakServerConfigBuilder API (`rules/server-and-registration.md`)

Verify against `test-framework/core/src/main/java/org/keycloak/testframework/server/KeycloakServerConfigBuilder.java`:

1. `features(Profile.Feature...)`, `featuresDisabled(Profile.Feature...)`
2. `option(String, String)`, `spiOption(String, String, String, String)`
3. `dependency(String, String)`, `dependency(String, String, boolean)`, `dependencyCurrentProject()`
4. `cache(CacheType)`, `externalInfinispanEnabled(boolean)`
5. `options(Map<String,String>)`, `bootstrapAdminClient(id, secret)`, `bootstrapAdminUser(user, pass)`
6. `shutdownDelay(String)`, `shutdownTimeout(String)`, `log()` (returns `LogBuilder`)
7. Check for new methods not listed in the rules

### 4.6 AdminClientFactory / AdminClientBuilder (`rules/core.md`)

Verify against `test-framework/core/src/main/java/org/keycloak/testframework/admin/`:

1. `AdminClientFactory.create()` -> returns `AdminClientBuilder`
2. `AdminClientBuilder` methods: `.realm()`, `.username()`, `.password()`, `.clientId()`, `.clientSecret()`, `.grantType()`, `.scope()`, `.authorization()`, `.autoClose()`, `.build()`

### 4.7 UserSupplier roles limitation (`rules/core.md`)

Verify against `test-framework/core/src/main/java/org/keycloak/testframework/realm/UserSupplier.java`:

1. Check that `getValue()` throws `UnsupportedOperationException` when `realmRoles` or `clientRoles` are set
2. Verify rules state that `@InjectUser` does NOT support `roles()` / `clientRoles()`
3. Verify rules recommend `RealmConfigBuilder.addUser()` or `realm.addUser()` for users with roles

### 4.8 KeycloakServerConfig examples (`rules/core.md`, `rules/server-and-registration.md`)

Verify that existing `KeycloakServerConfig` implementations in `tests/base/` match the patterns documented in rules:

```bash
grep -rln "implements KeycloakServerConfig" tests/base/src/test/java/ | wc -l
```

Spot-check 3-5 implementations to verify documented patterns for:
- Feature flags (`features(Profile.Feature.X)`)
- Vault (`option("vault", "file")`)
- SPI options (`option("spi-...")` and `spiOption(...)`)
- Custom providers (`dependency(...)`)

---

## Step 5: Check for new framework features not covered

### 5.1 New @Inject* annotations

```bash
grep -rh "@interface Inject" test-framework/ --include="*.java" | sed 's/.*@interface //' | sed 's/[{ }]//g' | sort -u
```

Compare against annotations mentioned across ALL rule files AND specs. Report any new annotation that no file covers.

### 5.2 New ConfigBuilder classes

```bash
find test-framework/ -name "*ConfigBuilder.java" -not -path "*/target/*" | sed 's|.*/||' | sort
```

Compare against builders mentioned in `rules/core.md` and `specs/keycloak-test-framework.md`. Report any new ones.

### 5.3 New page objects

```bash
ls test-framework/ui/src/main/java/org/keycloak/testframework/ui/page/*.java 2>/dev/null | sed 's|.*/||' | sed 's/.java//' | sort
```

Compare against the page mapping table in `rules/webdriver.md`. Add any new pages.

### 5.4 New managed resource methods

Read `ManagedRealm.java`, `ManagedClient.java`, `ManagedUser.java` and check if any public methods are NOT documented in rules or specs.

---

## Step 6: Cross-check rules against specs

1. Read `specs/keycloak-test-framework.md` Section 1 (injection annotations table).
2. Read `rules/core.md` imports section.
3. Verify every annotation in the spec is listed in the rules, and vice versa.
4. If the spec lists an annotation with a different package than the rules, fix whichever is wrong (verify against actual code).

---

## Step 7: Verify code examples compile conceptually

For each code block in the rule files marked as `// NEW`:

1. Check that the class names used match actual classes
2. Check that method calls use correct parameter counts and types
3. Check that return types in chained calls are plausible
4. Flag any example that uses a method or class that doesn't exist

Do NOT try to actually compile the examples. This is a manual review step.

---

## Step 8: Regenerate checklist

Since file counts changed, regenerate the migration checklist:

```bash
cd tests/migration-util/ai-migration-tool/checklist && ./generate-checklist.sh
```

Verify the output counts are consistent with the spec file counts updated in Step 2.

---

## Step 9: Report

```
=== KNOWLEDGE UPDATE SUMMARY ===

Spec files:
  keycloak-tests-module.md:
    File count: [old] -> [new] (updated/unchanged)
    New base classes: [list or "none"]
    New config classes: [list or "none"]
    New packages: [list or "none"]
  keycloak-test-framework.md:
    File count: [old] -> [new] (updated/unchanged)
    New annotations: [list or "none"]
    New builders: [list or "none"]
    Method signature fixes: [list or "none"]
  arquillian-testsuite.md:
    File count: [old] -> [new] (updated/unchanged)

Rule files:
  Import paths verified: <N>
    Broken: [list or "none"]
    Fixed:  [list or "none"]
  Method signatures verified: <N>
    Wrong: [list or "none"]
    Fixed: [list or "none"]

New framework features not covered:
  Annotations: [list or "none"]
  Builders: [list or "none"]
  Page objects: [list or "none"]
  Managed resource methods: [list or "none"]

Spec <-> Rule consistency:
  Mismatches: [list or "none"]

Code examples:
  Issues found: [list or "none"]

Status: ALL CURRENT / UPDATES APPLIED
```
