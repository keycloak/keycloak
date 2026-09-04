# Decision Trees for Ambiguous Migration Choices

Use these trees when the correct transformation isn't obvious. Each tree replaces multiple paragraphs of explanation with a single traversal.

---

## 1. Realm Configuration Strategy

```
Does the test extend AbstractTestRealmKeycloakTest or load testrealm.json?
├── NO → Create a minimal RealmConfig with only what the test uses
│        → @InjectRealm(config = MyConfig.class)
│
└── YES → Does the test use >5 users or users with OTP credentials?
    ├── YES → Does it also use @InjectOAuthClient?
    │   ├── YES → Use fromJson + @InjectOAuthClient(ref = "browser")
    │   │        to avoid test-app 409 conflict
    │   └── NO  → Use @InjectRealm(fromJson = "/org/keycloak/tests/testrealm.json")
    │
    └── NO → Extract only needed users/clients into a minimal RealmConfig
             → Do NOT load the full JSON
```

→ Full details: `core.md` §Realm Configuration, `specs/testrealm-json.md`

---

## 2. User Creation Strategy

```
Does the user need realm or client roles?
├── YES → Can the user be created at realm setup time (static)?
│   ├── YES → RealmConfigBuilder.addUser("u").password("p").roles("r")
│   │        inside a RealmConfig class
│   └── NO (dynamic, created mid-test) →
│       realm.addUser(UserConfigBuilder.create()
│           .username("u").password("p").roles("r"))
│       (auto-cleanup registered)
│
└── NO → Does the test need a ManagedUser handle (to call user.admin(), etc.)?
    ├── YES → @InjectUser(config = MyUserConfig.class) ManagedUser user
    │        (do NOT call .roles() in UserConfig — throws!)
    └── NO  → RealmConfigBuilder.addUser("u").password("p")
             (simpler, no extra field)
```

→ Full details: `core.md` §User Configuration

---

## 3. OAuthClient Configuration

```
Does the test need multiple OAuth clients (multi-browser, multi-client)?
├── YES → Use ref on each: @InjectOAuthClient(ref = "second")
│        Creates client "test-app-second" automatically
│        For second browser: @InjectOAuthClient(webDriverRef = "second", ref = "second")
│
└── NO → Is the realm loaded from JSON that already has a "test-app" client?
    ├── YES → @InjectOAuthClient(ref = "browser")
    │        Avoids 409 conflict with existing test-app
    └── NO  → @InjectOAuthClient (no attributes needed)
             Supplier creates "test-app" automatically

NEVER change clientId via custom ClientConfig — it breaks TestApp redirect wiring.
```

→ Full details: `oauth.md` §OAuthClient Automatic Client Management

---

## 4. Base Class Resolution

```
Does the legacy test extend a base class?
├── NO → Just add @KeycloakIntegrationTest and inject what you need
│
└── YES → Which base class?
    ├── AbstractKeycloakTest / AbstractTestRealmKeycloakTest
    │   → Flatten: remove extends, add @InjectRealm + @InjectAdminClient
    │
    ├── AbstractAdminTest
    │   → Check: does AbstractRealmTest exist in tests/?
    │     ├── YES → extend AbstractRealmTest
    │     └── NO  → flatten with @InjectRealm + @InjectAdminClient
    │
    ├── AbstractAuthenticationTest / AbstractGroupTest / AbstractIdentityProviderTest / etc.
    │   → Check: does equivalent exist in tests/base/src/test/java/?
    │     Search: find tests/base -name "Abstract*Test.java"
    │     ├── YES → extend it (check its injected resources match what you need)
    │     └── NO  → flatten using specs/base-class-flattening.md recipe
    │
    └── Other / custom base class
        → Read the ENTIRE chain. Identify used fields/methods.
          Inline only what the specific test needs.
```

→ Full details: `core.md` §Base Class Resolution, `specs/base-class-flattening.md`

---

## 5. Server Configuration

```
Does the legacy test have @EnableFeature, @DisableFeature, @EnableVault, or @SetDefaultProvider?
├── NO → No KeycloakServerConfig needed (use plain @KeycloakIntegrationTest)
│
└── YES → Search: grep -rln "implements KeycloakServerConfig" tests/base/
          Does an existing ServerConfig class match your needs?
    ├── YES → Reuse it: @KeycloakIntegrationTest(config = ExistingConfig.class)
    └── NO  → Create inner class:
              public static class ServerConfig implements KeycloakServerConfig {
                  public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder c) {
                      return c.features(Profile.Feature.X);  // or .option(), .dependency(), etc.
                  }
              }

Does it also need custom providers?
├── YES → Add: config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers")
│        Check if provider exists in tests/custom-providers/ (see specs/custom-providers.md)
└── NO  → Done
```

→ Full details: `server-and-registration.md` §Feature Flags, `core.md` §Annotations to Remove

---

## 6. Admin Client Strategy

```
How many Keycloak admin client instances does the test need?
├── ONE (bootstrap admin, most common)
│   → @InjectAdminClient Keycloak adminClient
│   Or just use realm.admin() for realm-scoped operations
│
├── ONE scoped to the managed realm
│   → @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM) Keycloak adminClient
│
└── MULTIPLE (different users, service accounts, permission tests)
    → @InjectAdminClientFactory AdminClientFactory adminClientFactory
      Keycloak client1 = adminClientFactory.create()
          .realm(realm.getName()).username("u").password("p")
          .clientId(Constants.ADMIN_CLI_CLIENT_ID)
          .autoClose().build();
```

→ Full details: `core.md` §Admin Client Factory

---

## 7. testingClient Replacement

```
What does the test use testingClient for?
├── setTimeOffset / resetTimeOffset
│   → @InjectTimeOffSet TimeOffSet timeOffSet → timeOffSet.set(n)
│
├── pollEvent / clearEventQueue
│   → @InjectEvents Events events → events.poll() / events.clear()
│
├── pollAdminEvent / clearAdminEventQueue
│   → @InjectAdminEvents AdminEvents → adminEvents.poll() / adminEvents.clear()
│
├── server().run(session -> ...) — entire test method is server-side
│   → @TestOnServer annotation on the test method
│
├── server().run(session -> ...) — mixed client/server in same method
│   → @InjectRunOnServer RunOnServerClient → runOnServer.run(session -> ...)
│
├── server().fetch(session -> ..., Type.class)
│   → runOnServer.fetch(session -> ..., Type.class)
│
└── testApp()
    → @InjectTestApp TestApp testApp (rarely needed — OAuthClient handles it)
```

→ Full details: `server-and-registration.md` §testingClient, `utilities.md` §testingClient

---

## 8. Utility Class Resolution

```
Does the test import a utility from org.keycloak.testsuite.*?
│
├── Check utilities.md mapping table first
│   Found? → Use the mapped replacement
│
├── Search tests/utils/ and tests/utils-shared/ by METHOD name
│   Found? → Update import
│
├── Is the usage simple (1-3 lines)?
│   → Inline the logic directly
│
└── Complex usage, not found anywhere
    → Do other non-migrated tests also use this utility?
      ├── YES → Create in tests/utils-shared/ (package: o.k.testsuite.util)
      └── NO  → Create in tests/utils/ (package: o.k.tests.utils)
```

→ Full details: `utilities.md` §Missing Util Classes

---

## 9. Page Object Resolution

```
Does the test use @Page with a page object?
│
├── Check specs/page-objects.md for the page name
│   Is it listed in "Available in New Framework"?
│   ├── YES → Use @InjectPage with new import (check name mapping — some names differ)
│   └── NO  → Create the page object:
│             1. Read old page from testsuite/
│             2. Create in test-framework/ui/.../page/
│             3. Extend AbstractLoginPage (for auth pages) or AbstractPage
│             4. Constructor: public XxxPage(ManagedWebDriver driver) { super(driver); }
│             5. Implement getExpectedPageId() (check themes/ for data-page-id)
│             6. Port only methods the test actually uses
│             7. ./mvnw install -pl test-framework/ui -DskipTests
│
└── Does the old page have login()/isCurrent()/open() methods?
    ├── login() → fillLogin() + submit() (or fillPassword() + submit(), etc.)
    ├── isCurrent() → getExpectedPageId() replaces this
    └── open() → oauthClient.openLoginForm() / webDriver.open(url)
```

→ Full details: `webdriver.md` §Creating New Page Objects, `specs/page-objects.md`
