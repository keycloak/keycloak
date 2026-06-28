# Keycloak Tests Module — Migration Reference

**Module**: `keycloak-tests-parent`
**Location**: `tests/`
**Total**: ~424 Java files across 8 submodules (~265 test files + ~159 library files) *(counts as of 2026-03-18)*

---

## 1. Key Submodules

| Module | Artifact | Files | Purpose |
|--------|----------|-------|---------|
| `base/` | `keycloak-tests-base` | 297 files (248 use `@KeycloakIntegrationTest`) | Primary test suite |
| `utils/` | `keycloak-tests-utils` | 17 files | Test utilities (`Assert`, `AdminApiUtil`, `AdminEventPaths`, matchers) |
| `utils-shared/` | `keycloak-tests-utils-shared` | 72 files | Shared between old & new (OAuth classes, `FlowUtil`, `AccountHelper`, builders) |
| `custom-providers/` | `keycloak-tests-custom-providers` | 17 files | Custom SPI providers deployed into Keycloak server |
| `clustering/` | `keycloak-tests-clustering` | 2 files | Multi-node cluster tests |
| `webauthn/` | `keycloak-tests-webauthn` | 9 files | WebAuthn/FIDO2 tests |
| `migration-util/` | `keycloak-tests-migration-util` | 10 files | Automated migration tool (`migrate.sh`) |

---

## 2. Abstract Base Test Classes

Tests frequently extend shared base classes that provide common injections and helpers:

| Base Class | Injected Resources | Key Helpers |
|-----------|-------------------|-------------|
| `AbstractRealmTest` | `ManagedRealm`, `Keycloak`, `AdminClientFactory`, `RunOnServerClient`, `AdminEvents` | Realm admin operations |
| `AbstractUserTest` | `ManagedRealm`, `Keycloak`, `AdminEvents`, `ManagedWebDriver`, `RunOnServerClient`, `LoginPage` | `createUser()`, `updateUser()`, `deleteUser()`, `addFederatedIdentity()` |
| `AbstractAuthenticationTest` | `ManagedRealm`, `AuthenticationManagementResource`, `AdminEvents` | `findExecutionByProvider()`, `findFlowByAlias()`, `newFlow()`, `createFlow()` |
| `AbstractGroupTest` | `ManagedRealm` | Group CRUD helpers |
| `AbstractIdentityProviderTest` | `ManagedRealm` | IdP management helpers |
| `AbstractIdentityProviderStoreTokenTest` | `OAuthClient` (x2), `LoginPage`, `RunOnServerClient` | IdP token storage tests (broker) |
| `AbstractClientSearchTest` | `ManagedRealm` with `ClientSearchRealmConfig` | Client search helpers |
| `AbstractClientScopeTest` | `ManagedRealm` | Client scope helpers |
| `AbstractProtocolMapperTest` | `ManagedRealm` | Mapper management |
| `AbstractPartialImportTest` | `ManagedRealm` | Import operations |
| `AbstractConcurrencyTest` | `ManagedRealm`, `Keycloak` | Concurrent operation helpers |
| `AbstractPermissionTest` / `AbstractAdminRBACTest` | `ManagedRealm` | Authorization testing |
| `AbstractFineGrainedAdminTest` | `ManagedRealm` | Fine-grained admin permissions |
| `AbstractRealmRolesTest` | `ManagedRealm` | Role management |
| `AbstractUserStorageRestTest` | `ManagedRealm` | User storage/federation |
| `AbstractBaseClientAuthTest` / `AbstractClientAuthTest` | `ManagedRealm` | Client authentication |
| `AbstractPermissionsTest` | `ManagedRealm` | Permissions testing |
| `AbstractDBSchemaTest` | `ManagedRealm` | Database schema tests |
| `AbstractJWTAuthorizationGrantTest` | `ManagedRealm` | JWT authorization grant |
| `AbstractWorkflowTest` | `ManagedRealm` | Workflow engine tests |

---

## 3. Common Configuration Classes

### Shared Configs (in `tests/base/src/test/java/org/keycloak/tests/common/`)

| Config Class | Type | Purpose |
|-------------|------|---------|
| `BasicRealmWithUserConfig` | `RealmConfig` | Realm with user: basic-user / password / basic@localhost |
| `BasicUserConfig` | `UserConfig` | Standard test user: basic-user / password |
| `TestRealmUserConfig` | `UserConfig` | Legacy-compatible: test-user@localhost / password (Tom Brady) |

### Server Configs (used with `@KeycloakIntegrationTest(config = ...)`)

| Config Class | Location | Purpose |
|-------------|----------|---------|
| `CustomProvidersServerConfig` | `tests/model/` | Deploys custom-providers JAR |
| `KeycloakAdminPermissionsV1ServerConfig` | `admin/authz/fgap/` | Admin permissions v1 |
| `WorkflowsServerConfig` | `workflow/config/` | Workflow engine features |
| `WorkflowsBlockingServerConfig` | `workflow/config/` | Workflow blocking features |
| `WorkflowsScheduledTaskServerConfig` | `workflow/config/` | Workflow scheduled tasks |
| `DefaultAuthzServicesServerConfig` | `authz/services/config/` | Default authz services |
| `DefaultResourceServerConfig` | `authz/services/config/` | Default resource server |
| `ClientAuthIdpServerConfig` | `client/authentication/external/` | Client auth with IdP |
| `SpiffeClientAuthTest.SpiffeServerConfig` | inner class | SPIFFE client auth |
| `AbstractPartialImportTest.PartialImportServerConfig` | inner class | Partial import testing |

---

## 4. Utility Classes

For complete method lists, read the source files directly. Key gotchas only:

### utils/ (`org.keycloak.tests.utils`)

- `Assert` — `assertNames()`, `assertMap()` etc. **Import changed**: `o.k.testsuite.Assert` → `o.k.tests.utils.Assert`
- `AdminApiUtil` — **Renamed** from `ApiUtil`. Import: `o.k.tests.utils.admin.AdminApiUtil`
- `AdminEventPaths` — Import: `o.k.tests.utils.admin.AdminEventPaths`
- For `getCreatedId()` only: use `o.k.testframework.util.ApiUtil` (framework version)
- Hamcrest Matchers in `o.k.tests.utils.matchers`

### utils-shared/ (`org.keycloak.testsuite.util`)

Shared between old and new tests — imports stay as `o.k.testsuite.util.*`:
- `FlowUtil`, `AccountHelper`, `OAuthClientConfig`, `CredentialBuilder`, `FederatedIdentityBuilder`
- OAuth/OIDC request/response classes (`PasswordGrantRequest`, `AccessTokenResponse`, etc.)

---

## 5. Custom Providers (`tests/custom-providers/`)

Deployed via `KeycloakServerConfig.dependency("org.keycloak.tests", "keycloak-tests-custom-providers")`:

| SPI | Providers |
|-----|-----------|
| RequiredActionFactory | `DummyRequiredActionFactory`, `DummyConfigurableRequiredActionFactory` |
| AuthenticatorFactory | `ClickThroughAuthenticator` |
| IdentityProviderFactory | `OverwrittenMappersTestIdentityProviderFactory` |
| UserStorageProviderFactory | `DummyUserFederationProviderFactory`, `UserMapStorageFactory`, `HardcodedClientStorageProviderFactory` |
| PolicyProviderFactory | `GrantPolicyProvider` |
| ClientPolicyExecutorProviderFactory | Various test executors |
| TestComponentProviderFactory | `TestComponentProviderFactory` |

---

## 6. Test Packages

Test files live under `tests/base/src/test/java/org/keycloak/tests/`:

`admin/authentication`, `admin/authz`, `admin/authz/fgap`, `admin/authz/rbac`, `admin/client`, `admin/concurrency`, `admin/event`, `admin/finegrainedadminv1`, `admin/group`, `admin/identityprovider`, `admin/metric`, `admin/partialexport`, `admin/partialimport`, `admin/realm`, `admin/tracing`, `admin/user`, `admin/userprofile`, `admin/userstorage`, `authz`, `authz/services`, `authz/services/config`, `authz/services/uma`, `broker`, `client`, `client/authentication`, `client/authentication/external`, `client/policies`, `common`, `cors`, `db`, `forms`, `i18n`, `infinispan`, `keys`, `login`, `model`, `oauth`, `oid4vc`, `securityprofile`, `suites`, `tracing`, `transactions`, `welcomepage`, `workflow`, `workflow/activation`, `workflow/condition`, `workflow/config`, `workflow/execution`, `workflow/step`, `workflow/util`
