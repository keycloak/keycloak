# Dual-identity HTTPS verification

Verified on 2026-11-13, Linux x86-64, using Temurin JDK 21.0.7+6-LTS for both the harness and Keycloak.
The server used this branch's HTTPS BC TLS implementation, Quarkus 3.40.1.1, and BC JSSE 1.86.1.
The recorded test distribution included the earlier cache POC and was updated with this HTTPS runtime jar.
The HTTPS changes were subsequently separated onto a standalone branch; the demonstration uses `--cache=local` and does not depend on the cache POC.

Command, from the repository root:

```bash
/home/aschwart/.jdks/temurin-21.0.7/bin/java \
    -cp '/tmp/keycloak-https-bc-probe/keycloak-999.0.0-SNAPSHOT/lib/lib/main/*' \
    poc/https-dual-identity/DualIdentityHttps.java \
    /tmp/keycloak-https-bc-probe/keycloak-999.0.0-SNAPSHOT
```

Observed harness output, omitting BC warnings about unsupported JDK algorithm-constraint entries:

```text
Runtime: 21.0.7+6-LTS
Artifacts and server log: /tmp/keycloak-dual-identity-10050619317435592205
Generated rsa: RSA, SHA-256=f167c34ee79b202fc64f10edebddbc98baebc39c963cfaab2628a3843614b042
Generated mldsa: ML-DSA-65, SHA-256=64f604ccd40ff913265f307045833edfffc4d5998f4581a15404f09528412c86
Generated one PKCS12 keystore with TWO private-key entries: rsa, mldsa
Both certificates have SAN DNS:localhost; neither connection uses an alias option.
One server process, PID=1546480, endpoint=https://localhost:40733
PASS: rsa-only client -> RSA, SHA-256=f167c34ee79b202fc64f10edebddbc98baebc39c963cfaab2628a3843614b042, TLS=TLSv1.3, key exchange=X25519MLKEM768 (only permitted group), HTTP 200
PASS: mldsa-only client -> ML-DSA-65, SHA-256=64f604ccd40ff913265f307045833edfffc4d5998f4581a15404f09528412c86, TLS=TLSv1.3, key exchange=X25519MLKEM768 (only permitted group), HTTP 200
PASS: both identities served concurrently by the same listener, without restart or reload.
PASS: both fresh TLS 1.3 connections required hybrid key exchange; no classical fallback was permitted.
This is negotiated authentication, not a composite RSA+ML-DSA signature.
```

The process exited with status 0 and stopped the test server.
Both certificate fingerprints matched their generated counterparts.
No identity was replaced or reloaded during the test.
Each fresh TLS 1.3 connection permitted only `X25519MLKEM768`, so neither successful handshake could use classical-only key exchange.
The group restriction and negotiated TLS version were asserted by the harness.

The runtime regression suite also passed all five transport tests, including RSA, HTTP/2 ALPN, ML-DSA server-only and mutual authentication, and SNI selection for two different RSA hostnames.
All 14 selected HTTP configuration tests passed.
