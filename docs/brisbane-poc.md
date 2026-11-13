# Brisbane FIPS provider PoC

This PoC follows the structure of [the GlaSSLess prototype](https://github.com/keycloak/keycloak/pull/51082), with Project Brisbane's `JipherJCE` as the Java security provider. Bouncy Castle remains the default; Brisbane requires the `brisbane` feature. It is for evaluation; this work does not establish that a Keycloak deployment is FIPS validated.

## Brisbane artifact

The [Brisbane fork](https://github.com/slaskawi/brisbane) publishes the Jipher JAR through JitPack. This PoC pins the tested source commit rather than a moving branch:

```xml
<repository><id>jitpack.io</id><url>https://jitpack.io</url></repository>
<dependency>
  <groupId>com.github.slaskawi</groupId>
  <artifactId>brisbane</artifactId>
  <version>d50c2f1</version>
</dependency>
```

The [published POM](https://jitpack.io/com/github/slaskawi/brisbane/d50c2f1/brisbane-d50c2f1.pom) has those coordinates. The resolved JAR is 640,731 bytes with SHA-256 `4bbb57739818203b5a478ef6c44cd9e61357a794d172da42aa9e3cbd8781e102`. The JAR is built from source by JitPack and is unsigned; it must not be treated as a certified cryptographic module. The original local build used `brisbane.jipher:jipher-jce:20.1`, which is no longer required in the local Maven cache.

## Selecting Brisbane

Enable Brisbane with `--features=fips,brisbane` and `--fips-mode=non-strict` or `--fips-mode=strict`. The `brisbane` feature depends on `fips`, so Keycloak rejects `--features=brisbane` alone. With only `--features=fips`, Keycloak uses BouncyCastle FIPS. Brisbane strict mode sets `jipher.fips.enforcement=FIPS_STRICT`; non-strict sets `FIPS` before Jipher is initialized.

Keycloak must register Jipher itself. If `JipherJCE` is already registered, startup fails because the PoC cannot verify the policy that was active when it was initialized.

## Integration design

`Profile.Feature.BRISBANE` is experimental and depends on the existing `FIPS` feature. Keycloak's build step selects Brisbane only when that feature is enabled; an ordinary `--features=fips` build continues to select BouncyCastle FIPS. The provider choice is persisted through Quarkus augmentation, so change it by rebuilding the distribution or image.

The `crypto/brisbane` module registers a `CryptoProviderFactory` through `ServiceLoader`. Its implementation extends Keycloak's existing Elytron provider integration and delegates JCA operations to Jipher, including RSA and EC key generation/factories, signatures, AES-CBC, AES-GCM, and PBKDF2. The Elytron JWE wrappers bind ciphers to the selected Brisbane provider; an algorithm Jipher does not implement fails rather than falling through to SunJCE. Strict mode also preserves Keycloak's allowed RSA key sizes of 2048, 3072, and 4096 bits. Feature-based artifact filtering excludes the Brisbane JAR when Brisbane is off and excludes the BouncyCastle FIPS provider JARs when Brisbane is on.

The AES-CBC cipher serves only Keycloak's JOSE CBC-HMAC encryption; the JWE decoder checks its authentication tag before decrypting ciphertext. Brisbane rejects provider fallback if Jipher does not implement AES-CBC.

Brisbane rejects `RSA1_5` JWE key management. The [OpenSSL 3.1.2 FIPS 140-3 security policy](https://csrc.nist.gov/CSRC/media/projects/cryptographic-module-validation-program/documents/security-policies/140sp4985.pdf) lists RSA-OAEP, not RSAES-PKCS1-v1_5, for approved RSA key transport; [NIST's transition guidance](https://csrc.nist.gov/projects/cryptographic-module-validation-program/programmatic-transitions) ended allowance for PKCS#1 v1.5-only RSA key transport on January 1, 2024. This restriction applies to Brisbane's JWE provider in this PoC. Keycloak's default and Bouncy Castle providers retain their existing behavior, and other encryption paths still require an audit before any FIPS compliance claim. The shared Elytron provider still supports legacy `RSA1_5` with Keycloak's random-CEK fallback; its CodeQL warning is documented and narrowly suppressed on that unchanged compatibility path.

The Brisbane factory sets `jipher.fips.enforcement` before loading `JipherJCE`. Strict mode uses `FIPS_STRICT` and requires Jipher to have Java security provider priority one. If Jipher was previously registered, startup fails because its initial enforcement setting cannot be established by this integration.

Brisbane requires Java 25 or later at runtime. Keycloak's local build guidance supports JDK 17, 21, or 25, so JDK 25 is the shared supported version. With the JAR on Keycloak's class path, the JVM also needs `--enable-native-access=ALL-UNNAMED`. Jipher loads OpenSSL 3's cryptographic library, FIPS provider, and configuration file from the filesystem. Configure `-Djipher.openssl.dir=<openssl-root>` with the layout described in [Brisbane's prerequisites](https://github.com/openjdk/brisbane/blob/master/doc/prerequisites.md). On Oracle Linux 9.4 or later, Brisbane also documents `-Djipher.openssl.useOsInstance=true` for the OS-provided instance.

## OpenSSL 3 runtime on macOS and Linux

OpenSSL [lists 3.1.2 as FIPS 140-3 validated](https://www.openssl-library.org/source/) under [certificate #4985](https://csrc.nist.gov/projects/cryptographic-module-validation-program/certificate/4985). Its [FIPS build guide](https://github.com/openssl/openssl/blob/master/README-FIPS.md) permits that provider with a newer supported OpenSSL 3 library. This build uses the 3.1.2 FIPS provider and the supported 3.5.9 LTS library. The 3.5.9 FIPS provider must not be used in its place.

The official source tarballs and their SHA-256 files were downloaded from the OpenSSL GitHub releases. The verified tarball hashes were `a0ce69b8b97ea6a35b96875235aa453b966ba3cba8af2de23657d8b6767d6539` for 3.1.2 and `603f5602e2eef00d77fbd429d34dcd5822bb301757a1bc9cdb24c670f1eb859a` for 3.5.9. Build the native libraries on each target platform. The steps below ran on macOS ARM64 and Ubuntu 24.04 ARM64 in a disposable container.

```sh
# Set these to absolute paths. Run from a disposable work directory.
OPENSSL_WORK=/path/to/openssl-work
OPENSSL_PREFIX="$OPENSSL_WORK/install"
mkdir -p "$OPENSSL_WORK"
cd "$OPENSSL_WORK"

for version in 3.1.2 3.5.9; do
  curl -fL --retry 3 -o "openssl-$version.tar.gz" \
    "https://github.com/openssl/openssl/releases/download/openssl-$version/openssl-$version.tar.gz"
  curl -fL --retry 3 -o "openssl-$version.tar.gz.sha256" \
    "https://github.com/openssl/openssl/releases/download/openssl-$version/openssl-$version.tar.gz.sha256"
done

for version in 3.1.2 3.5.9; do
  actual=$(openssl dgst -sha256 -r "openssl-$version.tar.gz" | awk '{print $1}')
  expected=$(awk '{print $1}' "openssl-$version.tar.gz.sha256")
  test "$actual" = "$expected" || { echo "OpenSSL $version checksum mismatch" >&2; exit 1; }
done
tar -xzf openssl-3.1.2.tar.gz
tar -xzf openssl-3.5.9.tar.gz

for version in 3.1.2 3.5.9; do
  cd "$OPENSSL_WORK/openssl-$version"
  ./Configure enable-fips --prefix="$OPENSSL_PREFIX" \
    --openssldir="$OPENSSL_PREFIX/ssl" --libdir=lib
  make -j4
done

# Install the supported library, then only the validated FIPS provider.
cd "$OPENSSL_WORK/openssl-3.5.9"
make install_sw install_ssldirs
cd "$OPENSSL_WORK/openssl-3.1.2"
make install_fips
```

`--libdir=lib` yields Brisbane's expected layout on both platforms: `lib/libcrypto.3.dylib` and `lib/ossl-modules/fips.dylib` on macOS; `lib/libcrypto.so.3` and `lib/ossl-modules/fips.so` on Linux; plus `ssl/fipsmodule.cnf`. The FIPS module configuration must be generated on each target machine, not copied from another machine. On macOS, the 3.1.2 `openssl fipsinstall` self-tests and its `-verify` command passed for the installed provider. For FIPS deployment, follow the [module security policy](https://csrc.nist.gov/CSRC/media/projects/cryptographic-module-validation-program/documents/security-policies/140sp4985.pdf), including its operating-environment and installation requirements.

Verify the installed module and library versions:

```sh
case "$(uname -s)" in
  Darwin) FIPS_MODULE=fips.dylib ;;
  Linux) FIPS_MODULE=fips.so; export LD_LIBRARY_PATH="$OPENSSL_PREFIX/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}" ;;
esac
cd "$OPENSSL_WORK/openssl-3.1.2"
./apps/openssl fipsinstall -out "$OPENSSL_WORK/fipsmodule-selftest.cnf" \
  -module "$OPENSSL_PREFIX/lib/ossl-modules/$FIPS_MODULE"
./apps/openssl fipsinstall -verify -in "$OPENSSL_WORK/fipsmodule-selftest.cnf" \
  -module "$OPENSSL_PREFIX/lib/ossl-modules/$FIPS_MODULE"

cat > "$OPENSSL_PREFIX/ssl/openssl-fips.cnf" <<EOF
openssl_conf = openssl_init
.include $OPENSSL_PREFIX/ssl/fipsmodule.cnf
[openssl_init]
providers = provider_sect
alg_section = algorithm_sect
[provider_sect]
fips = fips_sect
base = base_sect
[base_sect]
activate = 1
[algorithm_sect]
default_properties = fips=yes
EOF
OPENSSL_CONF="$OPENSSL_PREFIX/ssl/openssl-fips.cnf" \
  "$OPENSSL_PREFIX/bin/openssl" list \
  -provider-path "$OPENSSL_PREFIX/lib/ossl-modules" -providers
```

The last command must show `base` version 3.5.9 and `fips` version 3.1.2. The Linux `LD_LIBRARY_PATH` setting makes the installed CLI use the built `libcrypto.so.3`; without it, the system library may be loaded. The separate self-test output is a local check; use the generated `ssl/fipsmodule.cnf` for Jipher.

Build Brisbane with JDK 25 using its wrapper:

```sh
cd /path/to/brisbane
JAVA_HOME=/path/to/jdk-25 /bin/sh gradlew --no-daemon jar
if [ "$(uname -s)" = Linux ]; then
  export LD_LIBRARY_PATH="$OPENSSL_PREFIX/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
fi
JAVA_HOME=/path/to/jdk-25 /path/to/jdk-25/bin/java \
  --enable-native-access=ALL-UNNAMED \
  "-Djipher.openssl.dir=$OPENSSL_PREFIX" \
  -cp build/libs/jipher-jce-20.1.jar com.oracle.jipher.provider.JipherJCE
```

The final command should report both `OpenSSL 3.5.9` and `OpenSSL FIPS Provider version 3.1.2`. It did on macOS and Linux ARM64. This proves the built Jipher JAR loads the intended native versions; it is not a claim that Keycloak itself is FIPS validated.

For a Keycloak distribution containing the locally installed JDK 25 Jipher JAR, the strict-mode smoke used:

```sh
export JAVA_HOME=/path/to/jdk-25
export JAVA_OPTS_APPEND="--enable-native-access=ALL-UNNAMED -Djipher.openssl.dir=$OPENSSL_PREFIX"
./bin/kc.sh build --features=fips,brisbane --fips-mode=strict
./bin/kc.sh start-dev --features=fips,brisbane --fips-mode=strict \
  --http-port=18080 --http-host=127.0.0.1
```

Repeat the build-time flags on `start-dev`: it re-augments the distribution. Without them, the smoke would start with the default provider and would not check Brisbane. The observed startup log named `JipherJCE version 20.1` with `FIPS policy: FIPS_STRICT`; the master realm's OIDC discovery endpoint returned HTTP 200.

## UBI image

[`quarkus/container/Dockerfile.brisbane`](../quarkus/container/Dockerfile.brisbane) builds OpenSSL on UBI 9.8 for the image's target architecture. Its downloaded release tarballs have pinned SHA-256 values. The build installs OpenSSL 3.5.9's runtime libraries, installs only the 3.1.2 FIPS provider, runs `fipsinstall` and verifies the self-tests, then copies the native installation into a UBI 9.8 image with OpenJDK 25. The final image runs Keycloak as UID 1000 and is augmented for strict Brisbane FIPS mode. It sets the native-access and OpenSSL directory JVM options required by Jipher.

The published `quay.io/sebastian_laskawiec/keycloak:brisbane` tag has OCI index digest `sha256:63a9a9999e64b4469a6fa5aa3c09e1fdc1b68f26895992b7f1db6613e41a89f7`. The index contains `linux/arm64` and `linux/amd64` images. Both platform pulls and the remote manifest inspection succeeded.

From this checkout, build the local distribution and image:

```sh
./mvnw -pl quarkus/deployment,quarkus/dist -am -DskipTests -DskipProtoLock=true package
mkdir -p /tmp/keycloak-brisbane-image
cp quarkus/dist/target/keycloak-999.0.0-SNAPSHOT.tar.gz /tmp/keycloak-brisbane-image/
docker build -f quarkus/container/Dockerfile.brisbane \
  -t quay.io/sebastian_laskawiec/keycloak:brisbane \
  /tmp/keycloak-brisbane-image
```

The distribution must be rebuilt after changing the Maven dependencies.

### Run the published image on macOS

Start OrbStack or Docker Desktop and confirm `docker version` works. Docker selects the Linux ARM64 variant on Apple Silicon and Linux AMD64 on Intel Macs. The host does not need a local JDK or OpenSSL installation. Run the shared commands below in a terminal.

### Run the published image on Linux

Start the Docker daemon and confirm `docker version` works. Docker selects the Linux ARM64 or AMD64 variant matching the host. The host does not need a separate JDK or OpenSSL installation. Run the same commands below.

### Shared startup and verification commands

On either host, run:

```sh
docker pull quay.io/sebastian_laskawiec/keycloak:brisbane
CONTAINER_ID=$(docker run --detach --publish 18080:8080 \
  --env KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  --env KC_BOOTSTRAP_ADMIN_PASSWORD=brisbaneAdminPassword25 \
  quay.io/sebastian_laskawiec/keycloak:brisbane \
  start --optimized --features=fips,brisbane --fips-mode=strict \
  --http-enabled=true --hostname=localhost)
docker logs "$CONTAINER_ID"
until curl --fail --silent --output /dev/null \
  http://127.0.0.1:18080/realms/master/.well-known/openid-configuration; do
  sleep 2
done
curl --fail --silent --output /dev/null --write-out 'HTTP %{http_code}\n' \
  http://127.0.0.1:18080/realms/master/.well-known/openid-configuration
curl --fail --silent --request POST \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data client_id=admin-cli \
  --data username=admin \
  --data password=brisbaneAdminPassword25 \
  --data grant_type=password \
  http://127.0.0.1:18080/realms/master/protocol/openid-connect/token
docker exec "$CONTAINER_ID" java --enable-native-access=ALL-UNNAMED \
  -Djipher.openssl.dir=/opt/jipher/openssl \
  -cp /opt/keycloak/lib/lib/main/com.github.slaskawi.brisbane-d50c2f1.jar \
  com.oracle.jipher.provider.JipherJCE
docker rm --force "$CONTAINER_ID"
```

The loop waits for Keycloak to start. Discovery should return `HTTP 200`; the token response should contain a bearer `access_token` with a three-part JWT. The provider command should report JipherJCE 20.1, OpenSSL 3.5.9, and OpenSSL FIPS provider 3.1.2. This example uses HTTP and the embedded database for a local PoC; it is not a production deployment guide. The image is pre-augmented with the same feature and strict-mode settings, so the runtime flags make the selection explicit and verify that the invocation matches the build. Assess the target FIPS operating environment and security policy before any production deployment.

## Limits and follow-up

The JitPack coordinate is pinned to a source commit, but JitPack is an external build service. Every Maven build resolves that dependency even when the Brisbane feature is off; a JitPack outage can therefore break ordinary Keycloak CI. A production dependency should come from a reviewed, controlled artifact build and repository.

Jipher does not provide a Java `KeyStore` SPI. The inherited PKCS12/JKS keystore path still uses a JDK provider; certificate-path and JSSE operations also require a separate provider audit. Strict Brisbane mode only sets Jipher's own enforcement policy and does not make those other providers FIPS approved. The OpenSSL FIPS certificate applies to the listed OpenSSL module and operating environments, not to this Keycloak image, the unsigned Brisbane JAR, or all Keycloak cryptographic paths. The PoC tests provider selection, startup, discovery, and a token flow; it does not audit every algorithm or establish FIPS 140-3 compliance for Keycloak. Container startup and token checks are currently manual smoke tests, not automated Keycloak CI jobs.

## Validation

The Brisbane `jar` Gradle task passed on JDK 25. On macOS ARM64 and Ubuntu 24.04 ARM64, OpenSSL 3.5.9 loaded the 3.1.2 FIPS provider, the provider self-tests and verification passed, and Jipher on JDK 25 reported both versions. The prior local-artifact PoC passed its common, Brisbane, Elytron JWE, Quarkus configuration/Picocli, and CLI help tests; it also started in strict mode and issued an RS256 token on macOS.

For this published-artifact, feature-gated revision, the focused JDK 25 suites passed for `Profile`, FIPS provider selection, Brisbane provider registration, Picocli, ignored artifacts, and runtime configuration. The rebuilt distribution package and CLI help approval tests passed, as did `./mvnw spotless:check` and `git diff --check`. The distribution includes `com.github.slaskawi.brisbane-d50c2f1.jar` and `keycloak-crypto-brisbane`.

The UBI 9.8 Linux ARM64 and AMD64 images built successfully. The OpenSSL FIPS self-test and verification passed during each image build. Both optimized, strict-mode containers logged `FIPS policy: FIPS_STRICT`, served OIDC discovery with HTTP 200, and issued bearer JWTs whose headers declared `alg=RS256`. In each container, Jipher reported `OpenSSL 3.5.9` with `OpenSSL FIPS Provider version 3.1.2`. The AMD64 container was tested under emulation on an ARM64 host. These checks do not prove every Keycloak algorithm uses Jipher or establish FIPS validation of either image.

The brutal review identified a JWE cipher fallback, an `ECDSA`/`EC` key-pair-generator alias mismatch, and FIPS guide wording that could select the wrong provider; these were fixed. The independent finding-verification pass confirmed the fixes. The remaining review limits are the JDK keystore provider path, JitPack resolution during ordinary Maven builds, and the lack of automated CI startup/token checks; those are described above and require follow-up before production use.
