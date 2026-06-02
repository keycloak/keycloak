# AccountRestServiceReadOnlyAttributesTest Migration - Visual Guide

## Architecture Comparison

### OLD FRAMEWORK (Arquillian-based)

```
┌─────────────────────────────────────────┐
│  AccountRestServiceReadOnlyAttributesTest │
│            (extends)                     │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│      AbstractRestServiceTest            │
│            (extends)                    │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│   AbstractTestRealmKeycloakTest          │
│                                         │
│ @Rule TokenUtil tokenUtil               │
│ @Rule AssertEvents events               │
│ protected CloseableHttpClient httpClient│
│                                         │
│ @Before  - lifecycle setup              │
│ @After   - lifecycle teardown           │
│                                         │
│ configureTestRealm() - override method  │
└─────────────────────────────────────────┘
```

### NEW FRAMEWORK (Test Framework)

```
┌──────────────────────────────────────────────────────────────┐
│ @KeycloakIntegrationTest                                     │
│ AccountRestServiceReadOnlyAttributesTest                      │
│                                                              │
│ @InjectRealm ─────────────────────────┐                    │
│ @InjectUser  ──────────────────────┐  │                    │
│ @InjectSimpleHttp ──────────────┐  │  │                    │
│ @InjectOAuthClient ──────────┐  │  │  │                    │
│                              │  │  │  │                    │
├──────────────────────────────┼──┼──┼──┼────────────────────┤
│  @TestSetup                  │  │  │  │                    │
│  configureUserProfile()      │  │  │  │                    │
│  ┌────────────────────────┐  │  │  │  │                    │
│  │ • Realm Config Class ◄─┼──┘  │  │  │                    │
│  │ • User Config Class  ◄─┼─────┘  │  │                    │
│  └────────────────────────┘        │  │                    │
│                                    │  │                    │
│  Test Methods (unchanged logic)    │  │                    │
│  ┌────────────────────────────┐   │  │                    │
│  │ • testUpdate...Readonly... ◄───┼──┘                    │
│  │ • testUpdate...Unmanaged...    │                        │
│  │ • testAccountUpdate... (2x)    │                        │
│  │ • Helper methods               │                        │
│  └────────────────────────────┘   │                        │
│                                   │                        │
│  HTTP Methods (refactored)        │                        │
│  ┌──────────────────────────────┐ │                        │
│  │ • updateAndGet(User)       ◄──┘                        │
│  │ • get()                       │                        │
│  │ • updateError(...)            │                        │
│  │ • getAccountUrl(String)       │                        │
│  └──────────────────────────────┘                        │
└──────────────────────────────────────────────────────────────┘
     │              │           │          │
     │              │           │          │
     ▼              ▼           ▼          ▼
  ManagedRealm  ManagedUser  SimpleHttp OAuthClient
```

## Dependency Lifecycle

### OLD FRAMEWORK

```
Test Class Instantiation
          ↓
[Inheritance] Parent class constructor runs
          ↓
[Rules] TokenUtil & AssertEvents initialized
          ↓
@Before configureUserProfile() runs
          ↓
Test Method executes
          ↓
@After cleanup runs (in parent)
          ↓
Repeat for next test
```

### NEW FRAMEWORK

```
Test Class Instantiation
          ↓
[Annotation Processing] @Inject* resolved by framework
          ↓
[Dependency Injection] OAuthClient, SimpleHttp, ManagedRealm, ManagedUser created
          ↓
[Config Application] AccountRestServiceReadOnlyAttributesRealm.configure()
                      AccountRestServiceReadOnlyAttributesUser.configure()
          ↓
@TestSetup configureUserProfile() runs
          ↓
Test Method executes
          ↓
[Automatic Cleanup] ManagedRealm cleanup() handlers executed (reverse order)
                    ManagedUser cleanup() handlers executed
                    Realm/user state restored
          ↓
Repeat for next test
```

## Token Management Flow

### OLD FRAMEWORK

```
TokenUtil.getToken()
    ↓
    └─→ Performs OAuth Password Grant internally
    └─→ Returns raw token string
    └─→ Used in: SimpleHttpDefault.auth(token)
```

### NEW FRAMEWORK

```
OAuthClient.doPasswordGrantRequest(username, password)
    ↓
    └─→ Returns AccessTokenResponse object
    └─→ Call getAccessToken() to get token string
    └─→ Store in private field: accessToken
    └─→ Use in HTTP requests: .header("Authorization", "Bearer " + accessToken)
```

## HTTP Request Evolution

### OLD: Using SimpleHttpDefault

```java
SimpleHttpDefault.doGet(getAccountUrl(null), httpClient)
    .auth(tokenUtil.getToken())
    .asJson(UserRepresentation.class);

SimpleHttpDefault.doPost(getAccountUrl(null), httpClient)
    .auth(tokenUtil.getToken())
    .json(user)
    .asStatus();
```

### NEW: Using Injected SimpleHttp

```java
simpleHttp.doGet(getAccountUrl(null))
    .header("Authorization", "Bearer " + accessToken)
    .asJson(UserRepresentation.class);

simpleHttp.doPost(getAccountUrl(null))
    .header("Authorization", "Bearer " + accessToken)
    .json(user)
    .asStatus();
```

## Configuration Inheritance Evolution

### OLD: Override Method Pattern

```java
class AccountRestServiceReadOnlyAttributesTest extends AbstractRestServiceTest {
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.getUsers().add(UserBuilder.create().username("user1")...);
        testRealm.getClients().add(ClientBuilder.create().clientId("client1")...);
        // Modifying passed-in object
    }
}
```

### NEW: Configuration Class Pattern

```java
class AccountRestServiceReadOnlyAttributesTest {
    @InjectRealm(config = AccountRestServiceReadOnlyAttributesRealm.class)
    ManagedRealm managedRealm;

    public static class AccountRestServiceReadOnlyAttributesRealm
        implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.user(UserBuilder.create().username("user1")...)
                        .user(UserBuilder.create().username("user2")...);
            // Returns builder with fluent API
        }
    }
}
```

## Cleanup Handler Evolution

### OLD: Inherited Method

```java
@Test
public void testSomething() {
    getCleanup().addCleanup(() -> {
        // Cleanup code runs after test
    });
    // Test code
}
```

### NEW: Managed Resource Method

```java
@Test
public void testSomething() {
    managedRealm.cleanup().add(() -> {
        // Cleanup code runs after test
        // ManagedRealm handles order & timing
    });
    // Test code
}
```

## Files Generated for This Migration

1. **AccountRestServiceReadOnlyAttributesTest_MIGRATED.java**
   - Full migrated test class ready to use
   - Location: `/c:\Users\Manuel\Desktop\keycloak\`
   - 350+ lines with complete configuration

2. **MIGRATION_DIFF_SUMMARY.md**
   - High-level overview of changes
   - Benefits and migration patterns
   - Key differences highlighted

3. **DETAILED_CODE_DIFF.md**
   - Line-by-line code comparison
   - Import changes documented
   - Before/after code blocks

4. **This Visual Guide**
   - Architecture diagrams
   - Flow comparisons
   - Evolution of patterns

## Migration Validation Checklist

- ✓ All imports updated to testframework modules
- ✓ Class annotation: @KeycloakIntegrationTest applied
- ✓ Realm configuration extracted to RealmConfig class
- ✓ User configuration extracted to UserConfig class
- ✓ Dependencies injected via @Inject\* annotations
- ✓ Token retrieval in @TestSetup method
- ✓ Token stored as field for use in tests
- ✓ HTTP client changed from SimpleHttpDefault to injected SimpleHttp
- ✓ HTTP request headers updated for authorization
- ✓ Cleanup handlers migrated to managedRealm.cleanup()
- ✓ Test logic remains 100% unchanged
- ✓ All assertions preserved
- ✓ Helper methods refactored for new HTTP client

## Key Takeaways

| Aspect          | Old                      | New                     | Benefit             |
| --------------- | ------------------------ | ----------------------- | ------------------- |
| Class Structure | Inheritance              | Annotations             | Loose coupling      |
| Dependencies    | Rules                    | Injection               | Type-safe, explicit |
| Lifecycle       | @Before/@After           | @TestSetup/@TestCleanup | Clear intent        |
| Configuration   | Override method          | Config class            | Reusable, testable  |
| HTTP Client     | SimpleHttpDefault static | Injected SimpleHttp     | Testable, mockable  |
| Auth Token      | TokenUtil rule           | OAuthClient injection   | Explicit, flexible  |
| Cleanup         | getCleanup()             | resource.cleanup()      | Resource-owned      |
| JUnit Version   | JUnit 4                  | JUnit 5                 | Modern, extensible  |

## Next Steps

1. Copy `AccountRestServiceReadOnlyAttributesTest_MIGRATED.java` to replace the original
2. Run tests to verify all pass with new framework
3. Check IDE for any warnings or errors
4. Update any IDE inspections for new framework patterns
5. Consider migrating other Account REST tests similarly
