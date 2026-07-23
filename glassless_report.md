# Keycloak Glassless FIPS integration report

Date: 23 July 2026

Status: implemented, rebuilt, tested, containerized, pushed, and remotely verified

## Executive summary

This change ships Glassless as the default FIPS cryptographic provider for Keycloak while preserving Bouncy Castle compatibility. The default `fips-provider=auto` mode selects Bouncy Castle when its FIPS provider class is available and selects the bundled Glassless provider otherwise. Provider implementations are discovered through `ServiceLoader` factories. Selection uses a direct class resource check instead of cascading exception probes.

The resulting container is available at:

```text
quay.io/sebastian_laskawiec/keycloak:glassless
```

The immutable remote digest is:

```text
quay.io/sebastian_laskawiec/keycloak@sha256:a8b1e5942ad02d4f46336503c0f04565ed0188f2dc48e5e1f27687977cda8dfc
```

The tag is anonymously readable. It was verified without using the local Quay credentials.

Quay page: https://quay.io/repository/sebastian_laskawiec/keycloak?tab=tags

## Quick Docker manual

This is the shortest way to try the image locally. It uses HTTP and the embedded development database, so it is intended for evaluation only.

### 1. Pull the image

```bash
docker pull quay.io/sebastian_laskawiec/keycloak:glassless
```

For a reproducible pull, use the immutable digest:

```bash
docker pull quay.io/sebastian_laskawiec/keycloak@sha256:a8b1e5942ad02d4f46336503c0f04565ed0188f2dc48e5e1f27687977cda8dfc
```

To keep the complete demonstration immutable, replace the tagged image coordinate in the start command with this digest coordinate.

### 2. Start Keycloak

The bootstrap password below is deliberately long enough for strict FIPS mode.

```bash
CONTAINER_ID=$(docker run --detach \
  --publish 8080:8080 \
  --env KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  --env KC_BOOTSTRAP_ADMIN_PASSWORD=glasslessAdminPassword25 \
  quay.io/sebastian_laskawiec/keycloak:glassless \
  start --optimized \
  --features=fips \
  --fips-mode=strict \
  --http-enabled=true \
  --hostname-strict=false)

echo "$CONTAINER_ID"
```

The image is already optimized with these FIPS settings. The command intentionally shows `--features=fips` and `--fips-mode=strict`, but does not specify `--fips-provider`. Keycloak automatically selects the bundled Glassless provider because no Bouncy Castle FIPS provider class is present.

### 3. Wait for readiness

```bash
docker logs --follow "$CONTAINER_ID"
```

The important messages are:

```text
Automatically selected FIPS provider: glassless
GlasslessCryptoProvider created: KC(GlaSSLess version 0.13.0 [..., FIPS], FIPS mode: enabled, FIPS provider available: true, OpenSSL FIPS default properties: enabled)
Keycloak 999.0.0-SNAPSHOT on JVM ... started
```

Press `Ctrl+C` after the startup message. Then verify OpenID discovery:

```bash
curl --fail --silent --output /dev/null --write-out 'HTTP %{http_code}\n' \
  http://127.0.0.1:8080/realms/master/.well-known/openid-configuration
```

Expected result:

```text
HTTP 200
```

The administration console is available at http://127.0.0.1:8080/admin/ using `admin` and `glasslessAdminPassword25`.

### 4. Request a token

```bash
curl --fail --silent \
  --request POST \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data client_id=admin-cli \
  --data username=admin \
  --data password=glasslessAdminPassword25 \
  --data grant_type=password \
  http://127.0.0.1:8080/realms/master/protocol/openid-connect/token
```

A successful response contains a three segment JWT `access_token` and `"token_type":"Bearer"`.

### 5. Verify Glassless and OpenSSL FIPS status

```bash
docker exec "$CONTAINER_ID" \
  java --enable-native-access=ALL-UNNAMED \
  -jar /opt/keycloak/lib/lib/main/net.glassless.glassless-provider-0.13.0.jar
```

Expected result:

```text
Provider: GlaSSLess v0.13.0
FIPS Mode: ENABLED
FIPS Provider Available: true
OpenSSL FIPS Enabled: true
Hybrid Mode: DISABLED
```

Verify the persisted Keycloak configuration:

```bash
docker exec "$CONTAINER_ID" /opt/keycloak/bin/kc.sh show-config
```

Expected configuration includes:

```text
kc.features = fips (Persisted)
kc.optimized = true (Persisted)
```

The default `auto` provider value is not printed by `show-config`. The `Automatically selected FIPS provider: glassless` startup message is the authoritative selection evidence.

### 6. Stop and remove the demonstration container

```bash
docker rm --force "$CONTAINER_ID"
```

## User facing configuration

The build option accepts:

```text
--fips-provider=auto
--fips-provider=bouncycastle
--fips-provider=glassless
```

`auto` is the default. It selects Bouncy Castle when `org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider` is available and selects Glassless otherwise. A normal Glassless build therefore needs no provider option:

```bash
bin/kc.sh build \
  --features=fips \
  --fips-mode=strict
```

Glassless 0.13.0 requires a 64 bit Java 25 runtime, OpenSSL 3, and native access:

```bash
export JAVA_OPTS_APPEND='--enable-native-access=ALL-UNNAMED'
```

Glassless is bundled in the standard Keycloak distribution at `lib/lib/main/net.glassless.glassless-provider-0.13.0.jar`. Users do not need to download or copy it. To use Bouncy Castle instead, copy all supported Bouncy Castle FIPS JAR files into `KEYCLOAK_HOME/providers` before building. Explicit `glassless` and `bouncycastle` values remain available as overrides.

The implementation and container configuration follow the Glassless documentation:

1. https://glassless.net/
2. https://glassless.net/fips/
3. https://central.sonatype.com/artifact/net.glassless/glassless-provider/0.13.0

## Provider selection architecture

### Automatic provider model

`FipsProvider` defines the supported provider option values:

```text
AUTO -> auto
BOUNCY_CASTLE -> bouncycastle
GLASSLESS -> glassless
```

`SecurityOptions.FIPS_PROVIDER` exposes the build time option. Its default is `auto`.

`FipsProvider.resolve(ClassLoader)` checks for the Bouncy Castle FIPS provider class as a class loader resource. A present resource selects Bouncy Castle. An absent resource selects Glassless. Explicit provider values return themselves. This provides deterministic detection without loading provider classes, triggering static initialization, or using exception control flow.

### ServiceLoader registry

`CryptoProviderFactory` is a small provider factory contract:

```java
public interface CryptoProviderFactory {
    String getName();
    CryptoProvider create(FipsMode fipsMode);
}
```

Keycloak loads all factories with `ServiceLoader`, filters them by the configured provider name, and requires exactly one match. Missing and duplicate factories fail with deterministic errors. A factory returning `null` is also rejected.

The following factories are registered:

1. `DefaultCryptoProviderFactory`, provider name `default`
2. `Fips1402ProviderFactory`, provider name `bouncycastle`
3. `GlasslessCryptoProviderFactory`, provider name `glassless`

The existing `CryptoIntegration.init(ClassLoader)` entry point remains available for clients and existing test code. Keycloak server startup uses the new named factory entry point.

This keeps selection separate from provider construction and eliminates cascading exception blocks.

## Glassless provider implementation

The new `crypto/glassless` module contains:

1. `GlasslessCryptoProvider`
2. `GlasslessStrictCryptoProvider`
3. `GlasslessCryptoProviderFactory`
4. Unit tests using a controlled provider
5. Tests using the real Glassless 0.13.0 provider on Java 25

The implementation builds on the existing Elytron based Keycloak crypto provider. This reuses Keycloak certificate, PEM, OCSP, identity extraction, ECDSA, and JWE integration code instead of duplicating it.

Glassless is inserted at Java security provider priority one. Critical operations explicitly request the Glassless provider where the Keycloak crypto interface allows it. Covered operations include RSA key generation, RSA and EC key factories, RSA and RSA PSS signatures, EC parameters, AES GCM, and PBKDF2. AES CBC reuses the inherited Keycloak cipher implementation and resolves to Glassless through its priority one registration. The real provider test verifies that resolution.

### Bootstrap provider inventory

Every Glassless initialization logs the complete provider service inventory at info level. Services are grouped by type, types and algorithms are sorted for deterministic output, and each group includes its algorithm count. The output includes all registered ciphers, signatures, key generators, key factories, message digests, MACs, and other Java security services.

The distribution test verifies the inventory header, the cipher group, and `AES/GCM/NoPadding`. A controlled unit test verifies the complete deterministic format.

### Strict mode validation

Strict mode requires all of the following:

1. Glassless is installed and can initialize on Java 25 or later
2. Glassless reports that FIPS mode is enabled
3. Glassless reports that an OpenSSL FIPS provider is available
4. The OpenSSL default context reports that its FIPS properties are enabled
5. Glassless is the highest priority Java security provider

Startup fails if any strict mode requirement is not met. If initialization fails after Glassless was inserted, the provider is removed to avoid leaving partial global security state.

Glassless 0.13.0 does not expose requirement four through its public API. Keycloak temporarily isolates one reflective call to the internal `OpenSSLCrypto.isFIPSEnabled()` method. This method reads `EVP_default_properties_is_fips_enabled(NULL)` directly, so a `glassless.fips.mode=true` override or an operating system policy file cannot satisfy strict mode by itself. The workaround is encapsulated in `isOpenSslDefaultContextFipsEnabled` and can be replaced in one place when Glassless exposes the equivalent public API.

The strict provider retains the existing Keycloak FIPS RSA key size policy of 2048, 3072, and 4096 bits.

## Compatibility work

### Bouncy Castle compatibility

Bouncy Castle remains fully supported. Automatic mode selects it when the Bouncy Castle FIPS provider class is present. Its error message, strict and nonstrict modes, approved mode behavior, and explicit `--fips-provider=bouncycastle` override are preserved.

When the FIPS feature is disabled, the Glassless provider, both FIPS integrations, and their supporting artifacts are excluded from the optimized distribution. When the FIPS feature is enabled, both FIPS integrations are retained so automatic detection and explicit overrides work after user supplied provider JAR files are installed. The non FIPS Bouncy Castle artifacts and the default Keycloak crypto provider remain excluded.

### Keystore behavior

Strict Bouncy Castle mode continues to default HTTPS keystores and truststores to BCFKS.

Strict Glassless mode does not force BCFKS. BCFKS is a Bouncy Castle keystore implementation, so Glassless uses standard Java keystore implementations instead. PKCS12 is the default JDK keystore format and is the interoperable choice for deployments whose security policy permits it.

The HTTPS store type mapper resolves `auto` with the same provider detector. It defaults to BCFKS only when strict mode resolves to Bouncy Castle. Automatic Glassless mode leaves the type unset so file extension detection can select PKCS12.

#### Why PKCS12 works

The keystore format and the provider used for cryptographic operations are separate JCA concepts. Java 25 supplies the PKCS12 parser through the `SUN` provider. After the key is loaded, Glassless remains the highest priority provider and performs the supported cryptographic operations with OpenSSL. The strict Glassless container loaded a Java 25 PKCS12 keystore, started HTTPS, and returned HTTP 200 from OpenID discovery.

The tested Java 25 `keytool` output protected the private key and certificate contents with:

```text
PBES2
PBKDF2
HMAC SHA256
AES 256 CBC
10000 iterations
```

This avoids the legacy Triple DES, RC2, and SHA1 protection algorithms found in older PKCS12 files. Existing files must be inspected or recreated with the current Java runtime rather than assumed to use these settings.

#### Compliance boundary

PKCS12 working in strict Glassless mode is not, by itself, proof that every keystore protection operation is inside the OpenSSL FIPS module boundary. Provider tracing on the tested image established the following boundary:

```text
KeyStore.PKCS12                              SUN
Cipher.PBEWithHmacSHA256AndAES_256           GlaSSLess
Mac.HmacPBESHA256                            SunJCE
TLS operations after loading the private key GlaSSLess
```

The PKCS12 whole file MAC is the important qualification. Glassless 0.13.0 does not register `HmacPBESHA256`, so the standard Java 25 provider configuration resolves that operation to SunJCE. It does not run in Glassless or the OpenSSL FIPS module. RHEL 9 separately states that PKCS12 file processing is not FIPS compliant because the KDF used for the whole file HMAC is not approved.

The precise conclusion is therefore:

1. PKCS12 is functionally correct for strict Glassless Keycloak and was proven with HTTPS.
2. The BCFKS requirement is specific to BCFIPS and must not be imposed on Glassless.
3. PKCS12 is acceptable only when the deployment security policy permits this external container and its integrity mechanism.
4. A deployment that requires every key protection operation inside a validated module must use a platform approved keystore or external key management solution instead.

The applicable operating system policy and the validated OpenSSL module security policy remain authoritative. The RHEL qualification is documented at https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/9/html/security_hardening/switching-rhel-to-fips-mode_security-hardening.

### AES key wrapping

Glassless supports AES key wrapping through encrypt and decrypt operations, but does not support the Java `Cipher.wrap` and `Cipher.unwrap` SPI operations used by the existing Elytron implementation.

The focused compatibility change uses `ENCRYPT_MODE`, `DECRYPT_MODE`, and `doFinal` for AES key wrapping. The complete content encryption key is wrapped, which also fixes the previously skipped AES key wrap plus AES CBC HMAC JWE regression case.

The same compatible operation is used for ECDH ES with AES key wrapping.

## Main source changes

The main integration points are:

1. `common/src/main/java/org/keycloak/common/crypto/CryptoProviderFactory.java`
2. `common/src/main/java/org/keycloak/common/crypto/FipsProvider.java`
3. `common/src/main/java/org/keycloak/common/crypto/CryptoIntegration.java`
4. `crypto/default/src/main/java/org/keycloak/crypto/def/DefaultCryptoProviderFactory.java`
5. `crypto/fips1402/src/main/java/org/keycloak/crypto/fips/Fips1402ProviderFactory.java`
6. `crypto/glassless`
7. `quarkus/config-api/src/main/java/org/keycloak/config/SecurityOptions.java`
8. `quarkus/deployment/src/main/java/org/keycloak/quarkus/deployment/KeycloakProcessor.java`
9. `quarkus/runtime/src/main/java/org/keycloak/quarkus/runtime/KeycloakRecorder.java`
10. `quarkus/runtime/src/main/java/org/keycloak/quarkus/runtime/configuration/IgnoredArtifacts.java`
11. `quarkus/runtime/src/main/java/org/keycloak/quarkus/runtime/configuration/mappers/HttpPropertyMappers.java`
12. `crypto/elytron/src/main/java/org/keycloak/crypto/elytron/AesKeyWrapAlgorithmProvider.java`
13. `crypto/elytron/src/main/java/org/keycloak/crypto/elytron/ElytronEcdhEsAlgorithmProvider.java`
14. `docs/guides/server/fips.adoc`
15. `pom.xml` and `crypto/glassless/pom.xml` for the bundled provider dependency
16. `quarkus/tests/integration/src/test/java/org/keycloak/it/cli/dist/GlasslessFipsDistTest.java`

## Test coverage

### Clean distribution rebuild

The complete dependency reactor required for the Keycloak distribution was rebuilt from clean targets:

```bash
./mvnw -pl quarkus/dist -am -DskipTests -DskipExamples clean install
```

Result:

```text
Reactor modules: 55
Failures: 0
BUILD SUCCESS
```

Tests were deliberately run separately so their individual results remained visible.

### Focused unit and regression suites

The following suites were rerun after the clean build:

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| `CryptoProviderFactoryTest` | 4 | 0 | 0 | 0 |
| `FipsProviderTest` | 5 | 0 | 0 | 0 |
| `GlasslessCryptoProviderTest` | 6 | 0 | 0 | 0 |
| `GlasslessCryptoProviderActualTest` | 2 | 0 | 0 | 0 |
| `ElytronCryptoJWETest` | 14 | 0 | 0 | 2 |
| `ConfigurationTest` | 73 | 0 | 0 | 0 |
| `IgnoredArtifactsTest` | 16 | 0 | 0 | 0 |
| Total | 120 | 0 | 0 | 2 |

The complete Elytron crypto module suite was also run during implementation:

```text
Tests: 173
Failures: 0
Errors: 0
Skipped: 2
```

### Real Glassless cryptography

The real provider test uses Glassless 0.13.0, not a provider mock. It covers:

1. ServiceLoader factory discovery
2. Provider priority
3. RSA key pair generation
4. RS256 signing and verification
5. PS256 signing and verification
6. AES CBC encryption and decryption
7. AES key wrapping of a 32 byte key
8. HMAC SHA 256 compared with the reference SunJCE output
9. PBKDF2 with HMAC SHA 256
10. EC parameter creation
11. End to end A128KW plus A128CBC HS256 JWE encoding and decoding
12. Strict mode rejection when OpenSSL FIPS is unavailable or not selected by the default context
13. Strict mode acceptance against a real active OpenSSL FIPS default context

### Distribution integration tests

`FipsDistTest` was run against the freshly built distribution:

```text
Tests: 10
Failures: 0
Errors: 0
Skipped: 0
```

This suite covers existing Bouncy Castle behavior, automatic Bouncy Castle selection, missing provider failures, strict and nonstrict modes, and keystore compatibility.

`GlasslessFipsDistTest` runs in a separate provider specific distribution lifecycle:

```text
Tests: 1
Failures: 0
Errors: 0
Skipped: 0
```

It proves that the bundled provider is selected automatically without `--fips-provider`, starts a real Glassless nonstrict server, logs the complete provider inventory including the cipher group and AES GCM, checks OpenID discovery, and obtains an access token. The dedicated class keeps the Bouncy Castle and Glassless distribution augmentation lifecycles independent.

`HelpCommandDistTest` verifies all command line help approval snapshots:

```text
Tests: 23
Failures: 0
Errors: 0
Skipped: 0
```

The generated help contains:

```text
--fips-provider <provider>
The cryptographic provider used when FIPS mode is enabled. The 'auto' value
uses Bouncy Castle when its FIPS provider classes are available, otherwise
it uses Glassless. Possible values are: auto, bouncycastle, glassless.
Default: auto.
```

The complete Keycloak guide build also passed after the FIPS guide update:

```text
./mvnw -pl docs/guides -DskipTests package
BUILD SUCCESS
```

The exact formatting check used by the Keycloak CI also passed:

```text
./mvnw -Pdocs,distribution,operator spotless:check
Reactor modules: 163
BUILD SUCCESS
```

The complete 55 module distribution reactor was additionally built with OpenJDK 21. This proves that bundling the Java 25 Glassless artifact does not prevent a normal Java 21 Keycloak distribution build. On Java 21, users enabling FIPS must provide Bouncy Castle FIPS JAR files because Glassless itself requires Java 25.

### Current pull request CI remediation

The Ubuntu and Windows Quarkus unit test jobs from workflow run `29988221565` failed for the same Glassless related reason. The Quarkus runtime exposed `GlasslessCryptoProviderFactory` to `ServiceLoader`, but its wildcard dependency exclusions removed `keycloak-crypto-elytron`. Loading the Glassless factory therefore failed with `NoClassDefFoundError: org/keycloak/crypto/elytron/WildFlyElytronProvider` before the default provider could be selected. The failing aggregate Keycloak CI status was only a consequence of those two jobs.

The Quarkus runtime now declares its Elytron crypto dependency directly. The Glassless dependency can continue excluding all transitives, which keeps the Java 25 Glassless provider JAR out of consumers that only need the Keycloak runtime module, while every classpath that exposes the Glassless factory also contains its Keycloak base implementation.

The exact Quarkus unit test command from CI was rerun:

```text
./mvnw test -f quarkus/pom.xml -pl '!tests,!tests/junit5,!tests/integration,!dist'
Reactor modules: 5
Failures: 0
Errors: 0
BUILD SUCCESS
```

The two classes that failed on both CI platforms now pass:

```text
KeycloakMetricsConfigurationTest: 2 tests, 0 failures, 0 errors
KeycloakPathConfigurationTest: 5 tests, 0 failures, 0 errors
```

## Container construction

### Container contents

| Component | Value |
| --- | --- |
| Base | Red Hat Universal Base Image 10 |
| Architecture | `linux/amd64` |
| Java | OpenJDK 25.0.4 |
| OpenSSL | 3.5.5 |
| OpenSSL FIPS provider | Red Hat Enterprise Linux 9 OpenSSL FIPS Provider, 3.0.7 based module |
| Glassless | 0.13.0 |
| Keycloak | 999.0.0 SNAPSHOT from the current working tree |
| Local image size | 1,103,426,103 bytes |
| Local image ID | `sha256:913af873187a172ff799ca7e5a3712781632948097b79aa0043b91021ca7767e` |
| Remote manifest digest | `sha256:a8b1e5942ad02d4f46336503c0f04565ed0188f2dc48e5e1f27687977cda8dfc` |

The image is prebuilt with:

```text
--features=fips
--fips-mode=strict
```

No provider option is persisted. Automatic selection happens at startup and selects the bundled Glassless provider unless Bouncy Castle FIPS classes are supplied.

It also persists:

```text
JAVA_OPTS_APPEND=--enable-native-access=ALL-UNNAMED
OPENSSL_CONF=/etc/pki/tls/openssl-glassless-fips.cnf
```

The OpenSSL configuration activates the base and FIPS providers and sets the default property query to `fips=yes`.

Image inspection found Glassless only in the bundled runtime library directory:

```text
/opt/keycloak/lib/lib/main/org.keycloak.keycloak-crypto-glassless-999.0.0-SNAPSHOT.jar
/opt/keycloak/lib/lib/main/net.glassless.glassless-provider-0.13.0.jar
```

No Glassless JAR was copied into `/opt/keycloak/providers`.

### Input integrity

The rebuilt Keycloak distribution archive was:

```text
SHA256 bac0f887eef845995ba2fd90d30af51f3f161daf8632ca90e84204fee200057f
Size 185716280 bytes
quarkus/dist/target/keycloak-999.0.0-SNAPSHOT.tar.gz
```

The Glassless provider JAR was verified during the image build:

```text
SHA256 5b815a1f81ba01d8c0470e879f9b46d7b081bc05c2561cd99be4c76a0fb855b8
glassless-provider-0.13.0.jar
```

### Build command

The final image was built without Docker layer cache:

```bash
docker build --no-cache \
  --progress=plain \
  --file /tmp/keycloak-glassless-image/Containerfile \
  --tag quay.io/sebastian_laskawiec/keycloak:glassless \
  /tmp/keycloak-glassless-image
```

The build itself verified the Glassless JAR checksum, active OpenSSL providers, Glassless FIPS status, and successful Keycloak strict mode augmentation before producing the image.

## Container verification evidence

### Image build evidence

```text
/opt/keycloak/lib/lib/main/net.glassless.glassless-provider-0.13.0.jar: OK
FIPS
Providers:
  base
    name: OpenSSL Base Provider
    version: 3.5.5
    status: active
  fips
    name: Red Hat Enterprise Linux 9 - OpenSSL FIPS Provider
    version: 3.0.7-cda111b5812c30d4
    status: active

Provider: GlaSSLess v0.13.0
OpenSSL: OpenSSL 3.5.5 27 Jan 2026
FIPS Mode: ENABLED
FIPS Provider Available: true
OpenSSL FIPS Enabled: true
Hybrid Mode: DISABLED

Quarkus augmentation completed
Server configuration updated and persisted
```

### Running container evidence

Docker reported the exact container command:

```text
command=["start","--optimized","--features=fips","--fips-mode=strict","--http-enabled=true","--hostname-strict=false"]
```

There is no `fips-provider` argument. The startup log then proves the automatic decision:

```text
Automatically selected FIPS provider: glassless
GlasslessCryptoProvider created: KC(GlaSSLess version 0.13.0 [OpenSSL 3.5.5 27 Jan 2026, FIPS], FIPS mode: enabled, FIPS provider available: true, OpenSSL FIPS default properties: enabled)
Created temporary admin user with username admin
Keycloak 999.0.0-SNAPSHOT on JVM started in 5.543s
Listening on: http://0.0.0.0:8080
Profile prod activated
```

Functional requests returned:

```text
discovery_http=200
issuer=http://127.0.0.1:18081/realms/master
token_http=200
token_type=Bearer
access_token_segments=3
```

### Push and remote verification evidence

```text
The push refers to repository [quay.io/sebastian_laskawiec/keycloak]
glassless: digest: sha256:a8b1e5942ad02d4f46336503c0f04565ed0188f2dc48e5e1f27687977cda8dfc size: 1587
```

Anonymous remote manifest inspection returned:

```text
Name:      quay.io/sebastian_laskawiec/keycloak:glassless
MediaType: application/vnd.docker.distribution.manifest.v2+json
Digest:    sha256:a8b1e5942ad02d4f46336503c0f04565ed0188f2dc48e5e1f27687977cda8dfc
```

An anonymous pull using an empty Docker credential configuration also succeeded:

```text
quay.io/sebastian_laskawiec/keycloak@sha256:a8b1e5942ad02d4f46336503c0f04565ed0188f2dc48e5e1f27687977cda8dfc: Pulling from sebastian_laskawiec/keycloak
Digest: sha256:a8b1e5942ad02d4f46336503c0f04565ed0188f2dc48e5e1f27687977cda8dfc
Status: Image is up to date for quay.io/sebastian_laskawiec/keycloak@sha256:a8b1e5942ad02d4f46336503c0f04565ed0188f2dc48e5e1f27687977cda8dfc
```

## Production guidance

The quick manual intentionally enables HTTP and uses the embedded database. A production deployment must additionally configure at least:

1. TLS or a correctly configured trusted reverse proxy
2. A supported external database
3. Persistent storage and backup policies
4. Production bootstrap and credential management
5. Hostname and proxy settings
6. Resource limits and health checks
7. A tested upgrade and rollback process
8. Image digest pinning rather than a mutable tag

Use PKCS12 for Glassless keystores only when the deployment security policy permits its integrity mechanism. Otherwise, use a platform approved keystore or external key management solution.

## FIPS compliance boundary

The container proves that Glassless reports FIPS mode enabled, that the OpenSSL FIPS provider is active and available, and that Keycloak strict mode accepts and uses that provider.

The UBI tooling also emits this important warning:

```text
Using update-crypto-policies --set FIPS is not sufficient for FIPS compliance.
The kernel must be started with fips=1 for FIPS compliance.
```

Therefore, the container result must not be interpreted as certification of an arbitrary host. Formal compliance depends on the complete deployment boundary, including the host kernel, validated OpenSSL module, platform configuration, operational controls, and the requirements of the applicable certification program.

## Build provenance

The image was produced from branch `glassless`, based on commit:

```text
42f8e0ee84973df358a862bb8e06972e8628edb9
```

The automatic selection, bundled dependency, tests, and documentation were present as working tree changes on top of that commit when the distribution and container were built. The remote image digest is the authoritative identifier for the exact published container content.
