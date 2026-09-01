# Keycloak OpenID for Verifiable Credentials conformance tests

This module runs Keycloak against the official [OpenID Foundation conformance suite](https://gitlab.com/openid/conformance-suite)
to verify that Keycloak's OpenID for Verifiable Credentials implementation conforms to the specifications. Each test
drives a real conformance suite test module (plan creation, module execution and result assertion) against a live
Keycloak server, so a passing run is evidence of spec conformance, not just internal correctness.

The conformance suite itself (its `mongodb`, `conformance` and `nginx` containers) is started automatically via
Testcontainers; the suite version is pinned by the `conformance.version` property in the root `pom.xml`
(currently `release-v5.2.2`). Keycloak is started by the test framework as a distribution server over HTTPS and
reached by the suite at `https://host.testcontainers.internal:8443`.

## What is covered

The tests are grouped into three JUnit suites, one per profile/role. Each suite runs in its own JVM (one Keycloak
server configuration), which is also how CI runs them (`.github/workflows/conformance.yml`, one matrix job each).

| Suite | Package | Role / profile | Client authentication |
|-------|---------|----------------|-----------------------|
| `Oid4VciHaipTestSuite` | `vci.haip` | OID4VCI issuer, **HAIP** (`oid4vci-1_0-issuer-haip-test-plan`) | client attestation + DPoP; includes the FAPI2 and mDoc modules |
| `Oid4VciNonHaipTestSuite` | `vci.nonhaip` | OID4VCI issuer, **non-HAIP** (`oid4vci-1_0-issuer-test-plan`, `fapi_profile=vci`) | `private_key_jwt` + DPoP |
| `Oid4VpTestSuite` | `vp` | OID4VP verifier (`oid4vp-1final-verifier-test-plan`) | wallet mock; plain and encrypted (`direct_post.jwt`) response modes |

The suite/profile split is intentional: HAIP requires attestation-based client authentication, whereas the
conformance suite's issuer modules do not permit public or `client_secret` authentication, so the non-HAIP profile
uses `private_key_jwt`. Shared realm building blocks live in `vci/VciConformanceRealmUtil`; each profile's realm and
server configuration lives in `vci/haip` and `vci/nonhaip` respectively.

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
```

Run all suites together in one invocation:

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
