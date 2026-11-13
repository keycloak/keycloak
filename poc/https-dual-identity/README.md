# RSA and ML-DSA on one Keycloak HTTPS listener

This Java harness demonstrates two server identities active at the same time on the same hostname and port.
It generates the certificates, starts one Keycloak process, runs concurrent clients, checks their results, and stops the server.
It requires Java 21 or later, Bash for `kc.sh`, and a distribution built from the HTTPS BC TLS POC branch.
It uses the Bouncy Castle jars already bundled with that distribution; no Maven execution or downloads occur during the test.
A released Keycloak distribution without this POC is not sufficient.

## Run

Extract a freshly built POC distribution into a separate test directory.
From the repository root, run:

```bash
PQC_KC_HOME=/path/to/extracted/keycloak-999.0.0-SNAPSHOT
java -cp "$PQC_KC_HOME/lib/lib/main/*" \
    poc/https-dual-identity/DualIdentityHttps.java "$PQC_KC_HOME"
```

The selected `java` must be JDK 21 or later.
The harness launches Keycloak with the same JDK, using `System.getProperty("java.home")`.
No separate certificate-generation tool is needed: both keys and self-signed certificates are generated in Java using BC.

If needed, build the POC distribution first:

```bash
./mvnw -pl quarkus/deployment,quarkus/dist -am install -DskipTests -DskipTestsuite
```

The ZIP is produced in `quarkus/dist/target/`.
The harness uses development mode and an in-memory database; use it only for this demonstration.
It binds the server to loopback and uses temporary ports.

## What it proves

One PKCS12 keystore contains two private-key entries, `rsa` and `mldsa`.
Both certificates have the same subject, `CN=localhost`, and the same DNS SAN, `localhost`.
The server starts once with that keystore and no certificate-alias setting.
Both clients use the same hostname, SNI value, port, trust anchors, and HTTP request.
Both trust both certificates, and neither presents a client certificate.

The clients advertise different signature policies:

| Client | TLS handshake signature | Accepted certificate signature | Expected server identity |
| --- | --- | --- | --- |
| RSA-only | `rsa_pss_rsae_sha256` | `rsa_pkcs1_sha256` | RSA |
| ML-DSA-only | `mldsa65` | `mldsa65` | ML-DSA-65 |

Each client uses a fresh SSLContext, preventing TLS session resumption from obscuring certificate selection.
The clients start together using a barrier.
Each verifies the hostname and trust, compares the received certificate byte-for-byte with the generated certificate, and requires HTTP 200 from `/realms/master`.
The harness also prints certificate fingerprints and the single server PID and endpoint.
There is no keystore replacement, certificate reload, or server restart between clients.
Any failed assertion or handshake makes the process exit unsuccessfully.

Strictly speaking, the server selects a certificate compatible with the client's advertised signature schemes.
The test shows that a client can constrain the choice to RSA or ML-DSA; it does not establish a preference when a client offers both.
This is negotiated authentication, not hybrid authentication requiring both signatures.
Both clients permit only `X25519MLKEM768`, which combines X25519 and ML-KEM-768.
The harness checks that the client retains this group restriction and that the handshake uses TLS 1.3.
With fresh contexts, no session resumption, and only this one permitted group, a successful handshake demonstrates hybrid key exchange without classical fallback.
This establishes hybrid confidentiality protection with either server identity; the RSA case still uses conventional authentication.
The output identifies the sole permitted group rather than reading a negotiated-group field from the SSLSession API.

Generated test keys and the server log remain in the temporary directory printed at startup for inspection.
The disposable keystore password is `changeit`, and its certificates expire after one day.
Every run generates fresh identities.

See [RESULTS.md](RESULTS.md) for a recorded successful run.
