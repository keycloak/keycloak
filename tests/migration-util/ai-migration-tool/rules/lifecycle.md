# Lifecycle & Cleanup Migration Rules

Read this file when the legacy test uses `getCleanup()`, manual `@After` cleanup, `setTimeOffset()`, `updateWithCleanup`, `@FixMethodOrder`, `@Rule`/`@ClassRule`, `Assume.*`, or conditional execution.

---

## @Before/@After → Replace with Managed Objects

When migrating `@Before`/`@After` (or `@BeforeEach`/`@AfterEach`) methods, **do not blindly copy the setup/teardown code**. Check what the methods create or delete:

- **Creates a user** → replace with `@InjectUser` or `RealmConfigBuilder.addUser()`
- **Creates a client** → replace with `@InjectClient` or `RealmConfigBuilder.addClient()`
- **Creates a realm** → replace with `@InjectRealm`
- **Creates roles/groups** → replace with `RealmConfigBuilder.addRole()` / `addGroup()`
- **Configures realm settings** → replace with `RealmConfig` implementation
- **Deletes resources in @After** → remove entirely, managed objects auto-cleanup
- **Resets realm settings in @After** → use `updateWithCleanup()` instead (auto-resets)
- **Resets time offset** → use `@InjectTimeOffSet` (auto-resets)
- **Clears events** → use `@InjectEvents` (auto-managed)

The test framework manages the full lifecycle of injected resources. Prefer declarative `@Inject*` annotations and `RealmConfig` over imperative setup code.

**Only keep `@BeforeEach`/`@AfterEach` for things the framework cannot handle**, such as:
- Clearing browser cookies (`webDriver.cookies().deleteAll()`)
- Marking a realm dirty (`realm.dirty()`)
- Test-specific state that varies per method

```java
// OLD — imperative setup + teardown
@Before
public void setup() {
    UserRepresentation user = new UserRepresentation();
    user.setUsername("testuser");
    user.setEnabled(true);
    Response response = adminClient.realm("test").users().create(user);
    userId = ApiUtil.getCreatedId(response);
    response.close();

    CredentialRepresentation cred = new CredentialRepresentation();
    cred.setType("password");
    cred.setValue("password");
    adminClient.realm("test").users().get(userId).resetPassword(cred);
}

@After
public void cleanup() {
    adminClient.realm("test").users().get(userId).remove();
}

// NEW — declarative, framework-managed
@InjectRealm(config = MyRealmConfig.class)
ManagedRealm realm;

public static class MyRealmConfig implements RealmConfig {
    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        realm.addUser("testuser").password("password");
        return realm;
    }
}
// No @BeforeEach or @AfterEach needed — the framework creates and cleans up the user
```

```java
// OLD — client setup + teardown
@Before
public void setup() {
    ClientRepresentation client = new ClientRepresentation();
    client.setClientId("my-client");
    client.setSecret("secret");
    client.setDirectAccessGrantsEnabled(true);
    Response response = adminClient.realm("test").clients().create(client);
    clientId = ApiUtil.getCreatedId(response);
}

@After
public void cleanup() {
    adminClient.realm("test").clients().get(clientId).remove();
}

// NEW — declarative
@InjectClient(config = MyClientConfig.class)
ManagedClient client;

public static class MyClientConfig implements ClientConfig {
    @Override
    public ClientConfigBuilder configure(ClientConfigBuilder client) {
        return client.secret("secret").directAccessGrantsEnabled(true);
    }
}
// No setup/teardown needed
```

---

## Cleanup Patterns

### TestCleanup → realm.cleanup()

```java
// OLD
getCleanup("realmName").addCleanup(() -> adminClient.realm("realmName").clients().get(id).remove());

// NEW
realm.cleanup().add(r -> r.clients().get(id).remove());
```

### Manual @After realm deletion → Remove entirely

```java
// OLD: manual @After realm deletion
@After
public void cleanup() {
    adminClient.realm("test").remove();
}

// NEW: Remove entirely — ManagedRealm auto-deletes on lifecycle end
```

### Modify-and-reset → updateWithCleanup

```java
// OLD: modify and manually reset
RealmRepresentation rep = adminClient.realm("test").toRepresentation();
rep.setDefaultSignatureAlgorithm("ES256");
adminClient.realm("test").update(rep);
// ... in finally/After: reset to original value

// NEW — updateWithCleanup takes RealmUpdate (RealmConfigBuilder -> RealmConfigBuilder)
managedRealm.updateWithCleanup(r -> r.defaultSignatureAlgorithm("ES256"));
// Auto-resets to original representation after test — no try/finally needed
```

### updateWithCleanup API Details

**IMPORTANT**: `updateWithCleanup` takes `RealmUpdate...` varargs. `RealmUpdate` is a functional interface:

```java
@FunctionalInterface
public interface RealmUpdate {
    RealmConfigBuilder update(RealmConfigBuilder builder);
}
```

It uses **`RealmConfigBuilder` method names**, NOT `RealmRepresentation` setter names:
- `defaultSignatureAlgorithm()` NOT `setDefaultSignatureAlgorithm()`
- `eventsEnabled()` NOT `setEventsEnabled()`
- `adminEventsEnabled()` NOT `setAdminEventsEnabled()`

When unsure about the builder method name, check:
```
test-framework/core/src/main/java/org/keycloak/testframework/realm/RealmConfigBuilder.java
```

### Multiple updates in one call

```java
managedRealm.updateWithCleanup(
    r -> r.eventsEnabled(true),
    r -> r.adminEventsEnabled(true),
    r -> r.adminEventsDetailsEnabled(true)
);
```

---

## Time Manipulation

```java
// OLD (any of these forms)
testingClient.testing().setTimeOffset(Map.of("offset", String.valueOf(offset)));
Time.setOffset(offset);
setTimeOffset(offset);  // inherited from AbstractKeycloakTest
// ... in @After: resetTimeOffset();

// NEW
@InjectTimeOffSet
TimeOffSet timeOffSet;

// In test method:
timeOffSet.set(offset);
// Auto-resets — no manual cleanup needed. Remove resetTimeOffset() calls.
```

**Imports:**
```
org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet
org.keycloak.testframework.remote.timeoffset.TimeOffSet
```

---

## Test Ordering

```java
// OLD
@FixMethodOrder(MethodSorters.NAME_ASCENDING)

// NEW
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// Add @Order(1), @Order(2), etc. to test methods
```

---

## JUnit @Rule / @ClassRule

JUnit 4 `@Rule` and `@ClassRule` have no direct JUnit 6 equivalent. Replace with framework injection or `@BeforeEach`/`@BeforeAll`:

```java
// OLD
@Rule
public AssertEvents events = new AssertEvents(this);
// → See events.md for migration

// OLD
@Rule
public GreenMailRule greenMail = new GreenMailRule();
// → @InjectMailServer MailServer mailServer

// OLD
@ClassRule
public static CryptoInitRule cryptoInitRule = new CryptoInitRule();
// → Remove — framework handles crypto initialization

// OLD
@Rule
public TokenUtil tokenUtil = new TokenUtil("test-user@localhost", "password");
// → Replace with OAuthClient.doPasswordGrantRequest() — see utilities.md

// OLD
@Rule
public LDAPRule ldapRule = new LDAPRule();
// → TODO MIGRATION: LDAP rules have no framework equivalent yet
```

General pattern: `@Rule` fields that provide test resources → replace with `@Inject*` annotations. `@Rule` fields that enforce behavior → replace with `@BeforeEach`/`@AfterEach` or JUnit 6 extensions.

---

## Conditional Execution (Assume / ProfileAssume)

```java
// OLD — JUnit 4 Assume
import org.junit.Assume;
Assume.assumeTrue(someCondition);
Assume.assumeFalse(someCondition);

// NEW — JUnit 6 Assumptions
import org.junit.jupiter.api.Assumptions;
Assumptions.assumeTrue(someCondition);
Assumptions.assumeFalse(someCondition);
```

```java
// OLD — ProfileAssume (feature-based)
import org.keycloak.testsuite.ProfileAssume;
ProfileAssume.assumeFeatureEnabled(Feature.ADMIN_FINE_GRAINED_AUTHZ);
ProfileAssume.assumeCommunity();

// NEW — use framework conditions or server config instead
// Option 1: Skip for specific infrastructure
@DisabledForDatabases({"mssql"})
@DisabledForServers({"remote"})

// Option 2: Runtime assumption
Assumptions.assumeTrue(condition, "Skipped because...");

// Option 3: Feature requirement via server config (preferred)
// If the test REQUIRES a feature, use KeycloakServerConfig to enable it
// rather than skipping when it's not enabled
@KeycloakIntegrationTest(config = MyTest.ServerConfig.class)
```

**Imports for conditions:**
```
org.keycloak.testframework.conditions.DisabledForDatabases
org.keycloak.testframework.conditions.DisabledForServers
```

---

## @UncaughtServerErrorExpected

```java
// OLD
@UncaughtServerErrorExpected
@Test
public void testSomethingThatCausesServerError() { ... }

// NEW — Remove the annotation. If the test verifies error behavior,
// use assertThrows or catch the expected exception explicitly.
// The new framework does not have a server error suppression mechanism.
@Test
void testSomethingThatCausesServerError() {
    // If server errors are expected, catch or assert them explicitly
}
```

---

## @IgnoreBrowserDriver

```java
// OLD
@IgnoreBrowserDriver(FirefoxDriver.class)
@Test
public void testChromeOnly() { ... }

// NEW — No direct equivalent. Use assumptions based on browser type:
// import org.keycloak.testframework.ui.webdriver.BrowserType;
@Test
void testChromeOnly() {
    Assumptions.assumeTrue(
        webDriver.getBrowserType() != BrowserType.FIREFOX,
        "Skipped on Firefox"
    );
    // test logic
}
```
