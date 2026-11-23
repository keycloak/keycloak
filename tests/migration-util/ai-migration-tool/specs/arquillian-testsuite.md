# Legacy Arquillian Testsuite — Reference

**Module**: `keycloak-testsuite-pom`
**Location**: `testsuite/`
**Status**: DEPRECATED (JUnit 4, Arquillian-based — no new files allowed)
**Total**: ~1,338 Java files (~683 test files) *(counts as of 2026-03-18, decreasing as tests are migrated)*

---

## 1. Module Structure

```
testsuite/
├── integration-arquillian/                     (1,270 Java files)
│   ├── servers/
│   │   └── auth-server/services/
│   │       └── testsuite-providers/            (140 files) — Custom SPI providers
│   ├── tests/base/                             (1,092 files) — THE MAIN TEST SUITE
│   │   ├── src/main/java/                      (335 files) — Shared infrastructure
│   │   └── src/test/java/                      (~686 test classes)
│   └── util/                                   (8 files)
├── model/                                      (57 files) — JPA model layer tests
└── utils/                                      (21 files) — Shared utilities
```

---

## 2. Base Class Hierarchy

```
AbstractKeycloakTest (ROOT)
├── AbstractTestRealmKeycloakTest (loads /testrealm.json)
│   ├── AbstractAdminTest (admin realm, event listeners, SMTP)
│   ├── AbstractAuthTest (test realm with testUser, bburkeUser)
│   │   └── AbstractClientTest
│   └── other specialized bases...
├── AbstractAuthenticationTest (auth flow testing)
├── AbstractEventsTest (event store testing)
└── Abstract*Test (various domain-specific bases)
```

All tests use `@RunWith(KcArquillian.class)` and typically `@RunAsClient`.

---

## 3. Key Infrastructure Classes (in `src/main/java/`)

| Class | Purpose |
|-------|---------|
| `AbstractKeycloakTest` | Root base class — `adminClient`, `testingClient`, `addTestRealms()`, lifecycle |
| `AbstractTestRealmKeycloakTest` | Adds `configureTestRealm()` hook, loads `/testrealm.json` |
| `AbstractAdminTest` | Admin realm access, event listeners |
| `AbstractAuthTest` | Test realm with pre-configured users |
| `OAuthClient` | OAuth/OIDC protocol test client |
| `ApiUtil` | Admin API helpers (`getCreatedId()`, find clients/users) |
| `AssertEvents` | JUnit 4 `@Rule` for login event assertions |
| `AssertAdminEvents` | JUnit 4 `@Rule` for admin event assertions |
| `TestCleanup` | Per-realm cleanup handler |
| `FlowUtil` | Authentication flow construction (in utils-shared, shared with new tests) |

---

## 4. Resource Injection (Arquillian)

| Annotation | Type | Purpose |
|-----------|------|---------|
| `@ArquillianResource` | `SuiteContext`, `TestContext`, `OAuthClient` | Arquillian-managed resources |
| `@Drone` | `WebDriver` | Selenium WebDriver injection |
| `@Page` | Page objects | Graphene page object injection |
| `@Rule` | `AssertEvents`, `AssertAdminEvents`, `GreenMailRule` | JUnit 4 rules |
| `@ClassRule` | `CryptoInitRule` | JUnit 4 class-level rules |

---

## 5. Testsuite Providers (140 files)

Located in `testsuite/integration-arquillian/servers/auth-server/services/testsuite-providers/`

Custom SPI implementations deployed into Keycloak during tests via ShrinkWrap:
- Authenticator factories
- Required action factories
- Identity provider factories
- User storage provider factories
- Event listener factories
- REST resource providers (TestingResourceProvider for time offset, events, sessions)
- Protocol mapper factories
- Policy provider factories
- Client policy executor factories

When migrating tests that depend on these providers, check if the provider has already been copied to `tests/custom-providers/`. If not, copy it as part of the migration (see `rules/server-and-registration.md`).

---

## 6. Key Annotations

| Annotation | Purpose | Migration |
|-----------|---------|-----------|
| `@RunWith(KcArquillian.class)` | Test runner | → `@KeycloakIntegrationTest` |
| `@RunAsClient` | Client-side execution | → remove (default) |
| `@EnableFeature` / `@DisableFeature` | Feature flags | → `KeycloakServerConfig` |
| `@AppServerContainer` | Target app server | → remove |
| `@AuthServerContainerExclude` | Exclude auth server | → remove |
| `@ModelTest` | Server-side model test | → `@TestOnServer` (preferred, direct replacement) or `RunOnServerClient` (for mixed client/server tests) |
| `@SetDefaultProvider` | SPI provider selection | → `KeycloakServerConfig.option()` |
| `@UncaughtServerErrorExpected` | Suppress server errors | → remove |
| `@FixMethodOrder` | Test ordering | → `@TestMethodOrder` |
