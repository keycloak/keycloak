# Keycloak OpenID conformance tests

This module runs Keycloak against the official [OpenID Foundation conformance suite](https://gitlab.com/openid/conformance-suite)
to verify that Keycloak's OpenID for Verifiable Credentials and OpenID Connect implementations conform to the
specifications. Each test drives a real conformance suite test module (plan creation, module execution and result
assertion) against a live Keycloak server, so a passing run is evidence of spec conformance, not just internal
correctness.

The conformance suite itself (its `mongodb`, `conformance` and `nginx` containers) is started automatically via
Testcontainers; the suite version is pinned by the `conformance.version` property in the root `pom.xml`
(currently `release-v5.3.0`). Keycloak is started by the test framework as a distribution server over HTTPS and
reached by the suite at `https://host.testcontainers.internal:8443`.

## What is covered

The tests are grouped into JUnit suites, one per profile/role, each with its own Keycloak server configuration. CI
runs every suite as a separate matrix job (`.github/workflows/conformance.yml`), so each suite gets its own JVM.
Running a single suite locally (see [Running the tests](#running-the-tests)) is equivalent.
Note that the combined `./mvnw package` invocation does **not** give per-suite process isolation: Surefire uses a
single reusable fork, so all suites run in the same JVM.

| Suite | Package | Role / profile | Client authentication |
|-------|---------|----------------|-----------------------|
| `Oid4VciHaipTestSuite` | `vci.haip` | OID4VCI issuer, **HAIP** (`oid4vci-1_0-issuer-haip-test-plan`) | client attestation + DPoP; includes the FAPI2 and mDoc modules |
| `Oid4VciNonHaipTestSuite` | `vci.nonhaip` | OID4VCI issuer, **non-HAIP** (`oid4vci-1_0-issuer-test-plan`, `fapi_profile=vci`) | `private_key_jwt` + DPoP |
| `Oid4VpTestSuite` | `vp` | OID4VP verifier (`oid4vp-1final-verifier-test-plan`) | wallet mock; plain and encrypted (`direct_post.jwt`) response modes |
| `OidcTestSuite` | `oidc` | OpenID provider, **OpenID Connect Core** Config, Basic, Implicit and Hybrid certification plans | `client_secret_basic` / `client_secret_post`, static clients, discovery |
| `OidcFormPostTestSuite` | `oidc` | OpenID provider, **OpenID Connect Core** Form Post Basic, Implicit and Hybrid certification plans (`response_mode=form_post`) | `client_secret_basic` / `client_secret_post`, static clients, discovery |
| `OidcDynamicTestSuite` | `oidc` | OpenID provider, **OpenID Connect Core** Dynamic certification plan, for each of its six response types | `private_key_jwt`, clients the suite registers dynamically, discovery |

The suite/profile split is intentional: HAIP requires attestation-based client authentication, whereas the
conformance suite's issuer modules do not permit public or `client_secret` authentication, so the non-HAIP profile
uses `private_key_jwt`. Shared realm building blocks live in `vci/VciConformanceRealmUtil`; each profile's realm and
server configuration lives in `vci/haip` and `vci/nonhaip` respectively.

Each specification has its own package. Infrastructure the packages share, i.e. the conformance suite containers,
the suite API client and the scripted browser interactions, lives in the test framework's `conformance-runner`
module, and the OpenID provider tests share one Keycloak server configuration (`ConformanceServerConfig`).

Modules are grouped by the browser interaction and the result they are expected to finish with. Besides `PASSED`,
a module may be expected to finish with `REVIEW` (Keycloak's error page or a second login is snapshotted for a
human reviewer), `SKIPPED` or `WARNING`; the reasons are documented next to the module lists.

Keycloak always adds the `typ` claim to the ID token, and puts the claims of a requested scope, e.g. `email`, in it even when an access token is issued for the userinfo endpoint.
The specification permits both, but the suite warns about them, and no realm setting changes them.
The OpenID Connect realms therefore apply a conformance test adjustment, a script based protocol mapper of this module (`src/main/resources/conformance-adjustment-oidc-id-token.js`), which documents what it changes and why: it removes `typ`, and the email claims unless the response type is `id_token` or the client requested them in the ID token with the `claims` parameter.
It is test configuration, not a recommended Keycloak setup; the `scripts` feature and the deployment of this module as a provider (`ConformanceServerConfig`) are only there for it.
The OpenID Connect tests also disable the CSS processing of the suite's browser (`browsercontrol_css_enable`), where most of their time used to go.

In the Dynamic OP plan, the suite registers its clients itself through Keycloak's anonymous dynamic client registration.
Its realm (`OidcDynamicRealmConfig`) replaces Keycloak's default anonymous client registration policies, whose trusted hosts policy rejects the suite's registrations, with the consent required policy and a higher limit of clients.
The consent page then shows the logo, terms of service and policy a client registers, which the suite snapshots for review.
A default client scope of the realm gives the registered clients the ID token mappers of the static clients.
The suite skips `oidcc-idtoken-unsigned`, as Keycloak does not issue unsigned ID tokens (`alg=none`), which is acceptable for certification.

Keycloak fetches some URLs the suite publishes for its clients, e.g. their `jwks_uri`, `request_uri` and `sector_identifier_uri`.
The suite publishes them on its container internal host, which Keycloak, running on the Docker host, cannot resolve.
The test server therefore sends the requests for that host through a tunnel in the test JVM (`SuiteTunnel` of the `conformance-runner` module) to the suite's port on the Docker host, with the `proxy-mappings` option of Keycloak's outgoing HTTP client, and trusts the suite's TLS certificate.
The tunnel is started only when a Keycloak server configuration asks for it, so the OID4VCI and OID4VP tests do not open it.

## Prerequisites

- JDK and Maven (use the `./mvnw` wrapper).
- A running container runtime — **Docker or Podman** — with permission to pull from `registry.gitlab.com`.
- Keycloak built locally so the Quarkus distribution artifact is in your local Maven repository:

  ```bash
  ./mvnw install -DskipTests
  ```

## Running the tests

Run one suite (mirrors how CI runs them, one matrix job each):

```bash
./mvnw package -f tests/conformance/pom.xml -Dtest=Oid4VciNonHaipTestSuite
./mvnw package -f tests/conformance/pom.xml -Dtest=Oid4VciHaipTestSuite
./mvnw package -f tests/conformance/pom.xml -Dtest=Oid4VpTestSuite
./mvnw package -f tests/conformance/pom.xml -Dtest=OidcTestSuite
./mvnw package -f tests/conformance/pom.xml -Dtest=OidcFormPostTestSuite
./mvnw package -f tests/conformance/pom.xml -Dtest=OidcDynamicTestSuite
```

Run all suites together in one invocation (all suites share a single Surefire reusable fork, i.e. the same
JVM — this does not reproduce the per-process isolation of the CI matrix jobs):

```bash
./mvnw package -f tests/conformance/pom.xml
```

Run a single test class (fully qualified name; useful when iterating):

```bash
./mvnw test -f tests/conformance/pom.xml \
  -Dtest='org.keycloak.tests.conformance.vci.nonhaip.issuer.IssuerHappyFlowTest'
```

The first run pulls the conformance suite container images, so allow extra time. Full logs for a failed conformance
module are printed to the test output to aid debugging.

### Retries

Modules occasionally fail for reasons outside of Keycloak, e.g. network errors or the timing of the suite's waits.
Retries are opt-in per test: the OpenID Connect tests run a module that does not finish with its expected result, or whose run fails with an exception, again, in a new plan, up to 3 times; the OID4VCI and OID4VP tests do not retry.
Only if all attempts fail does the test fail, with the result of the last attempt.
A module whose run failed with an exception, e.g. as it timed out, is stopped before it runs again; if it cannot be stopped, the test fails right away, as the attempts could interfere.
Each attempt also uses its own suite alias (`keycloak`, `keycloak-retry1`, ...), so a late callback of an earlier attempt's browser, e.g. an authorization response, cannot reach a later attempt's module; the static clients register the callback of every attempt.
Every retry is printed to the test output, prefixed with `[conformance retry]`, also when a later attempt passes, so modules that pass only when run again stay visible.
Set the number of retries for all tests with the `kc.test.conformance.retries` option (system property, `KC_TEST_CONFORMANCE_RETRIES` environment variable or `keycloak-test.properties`), e.g. `0` to see every failure while iterating.
The OID4VCI and OID4VP tests use the same alias for all their attempts, so a late callback could still reach a retry of them:

```bash
./mvnw package -f tests/conformance/pom.xml -Dtest=OidcTestSuite -Dkc.test.conformance.retries=0
```
