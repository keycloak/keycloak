# Custom Provider Catalog

## Already in New Module (`tests/custom-providers/`)

| Provider Class | SPI | PROVIDER_ID | Package |
|---|---|---|---|
| `ClickThroughAuthenticator` | AuthenticatorFactory | `click-through-auth` | `o.k.tests.providers.forms` |
| `DummyRequiredActionFactory` | RequiredActionFactory | `dummy-required-action` | `o.k.tests.providers.actions` |
| `DummyConfigurableRequiredActionFactory` | RequiredActionFactory | `dummy-configurable-required-action` | `o.k.tests.providers.actions` |
| `OverwrittenMappersTestIdentityProviderFactory` | IdentityProviderFactory | — | `o.k.tests.providers.broker.oidc` |
| `DummyUserFederationProviderFactory` | UserStorageProviderFactory | `dummy` | `o.k.tests.providers.federation` |
| `UserMapStorageFactory` | UserStorageProviderFactory | `user-password-map` | `o.k.tests.providers.federation` |
| `HardcodedClientStorageProviderFactory` | ClientStorageProviderFactory | `hardcoded-client` | `o.k.tests.providers.federation` |
| `GrantPolicyProvider` | PolicyProviderFactory | — | `o.k.tests.providers.authorization.policy` |
| `TrackEventsClientPolicyExecutor` | ClientPolicyExecutorProviderFactory | — | `o.k.tests.providers.client.policies` |
| `TestComponentProviderFactory` | — | — | `o.k.tests.providers.components` |

---

## NOT Yet Migrated (commonly needed)

Sorted by likely migration demand. Located in `testsuite/integration-arquillian/servers/auth-server/services/testsuite-providers/`.

### Authenticators (AuthenticatorFactory)

| Legacy Class | PROVIDER_ID | Legacy Package | Usage |
|---|---|---|---|
| `PassThroughAuthenticator` | `testsuite-dummy-passthrough` | `o.k.testsuite.forms` | Authentication flow tests |
| `SetUserAttributeAuthenticatorFactory` + `SetUserAttributeAuthenticator` | `set-attribute` | `o.k.testsuite.authentication` | Conditional flow tests |
| `PushButtonAuthenticatorFactory` + `PushButtonAuthenticator` | `push-button-auth` | `o.k.testsuite.authentication` | Interactive authenticator tests |
| `ExpectedParamAuthenticatorFactory` + `ExpectedParamAuthenticator` | `expected-param-auth` | `o.k.testsuite.authentication` | Parameter validation tests |
| `DelayedAuthenticatorFactory` + `DelayedAuthenticator` | `delayed-auth` | `o.k.testsuite.authentication` | Timeout tests |
| `UsernameOnlyAuthenticator` | — | `o.k.testsuite.forms` | Username-only flow |
| `PassThroughRegistration` | — | `o.k.testsuite.forms` | Registration flow |
| `PassThroughClientAuthenticator` | — | `o.k.testsuite.forms` | Client auth flow |
| `ErrorEventAuthenticator` | — | `o.k.testsuite.forms` | Error event tests |
| `CustomAuthenticationFlowCallbackFactory` | — | `o.k.testsuite.authentication` | Flow callback tests |

### Client Policy (ClientPolicyExecutorProviderFactory / ConditionFactory)

| Legacy Class | Legacy Package |
|---|---|
| `TestRaiseExceptionExecutorFactory` | `o.k.testsuite.services.clientpolicy.executor` |
| `TestEnhancedPluggableTokenManagerExecutorFactory` | `o.k.testsuite.services.clientpolicy.executor` |
| `TestRaiseExceptionConditionFactory` | `o.k.testsuite.services.clientpolicy.condition` |

### Federation (UserStorageProviderFactory)

| Legacy Class | PROVIDER_ID | Legacy Package |
|---|---|---|
| `UserPropertyFileStorageFactory` | `user-prop-file` | `o.k.testsuite.federation` |
| `BackwardsCompatibilityUserStorageFactory` | — | `o.k.testsuite.federation` |
| `FailableHardcodedStorageProviderFactory` | — | `o.k.testsuite.federation` |
| `PassThroughFederatedUserStorageProviderFactory` | — | `o.k.testsuite.federation` |
| `HardcodedGroupStorageProviderFactory` | — | `o.k.testsuite.federation` |
| `HardcodedRoleStorageProviderFactory` | — | `o.k.testsuite.federation` |

### REST / Testing Resources

| Legacy Class | Legacy Package | Notes |
|---|---|---|
| `TestingResourceProvider` | `o.k.testsuite.rest` | Large — most functionality replaced by framework injections |
| `TestApplicationResourceProvider` | `o.k.testsuite.rest` | Test app — replaced by `TestApp` in framework |

### Other

| Legacy Class | SPI | Legacy Package |
|---|---|---|
| `CustomOIDCWellKnownProviderFactory` | WellKnownProviderFactory | `o.k.testsuite.wellknown` |
| `CustomUserProfileProviderFactory` | UserProfileProviderFactory | `o.k.testsuite.user.profile` |
| `TestEventsListenerProviderFactory` | EventListenerProviderFactory | `o.k.testsuite.events` |

---

## SPI Service Files in `tests/custom-providers/`

| Service File | Registered Providers |
|---|---|
| `o.k.authentication.AuthenticatorFactory` | `ClickThroughAuthenticator` |
| `o.k.authentication.RequiredActionFactory` | `DummyRequiredActionFactory`, `DummyConfigurableRequiredActionFactory` |
| Other service files | Check `tests/custom-providers/src/main/resources/META-INF/services/` |

When adding a new provider, register its factory in the appropriate service file.

---

## Migration Steps

See `rules/server-and-registration.md` "Creating a missing custom provider" for the detailed steps:
1. Copy provider + factory to `tests/custom-providers/src/main/java/org/keycloak/tests/providers/<subpackage>/`
2. Rewrite package: `org.keycloak.testsuite.*` → `org.keycloak.tests.providers.*`
3. Register factory in `META-INF/services/`
4. Build: `./mvnw install -pl tests/custom-providers -DskipTests`
5. Deploy: `builder.dependency("org.keycloak.tests", "keycloak-tests-custom-providers")`
