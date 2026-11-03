/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.it.cli.dist;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.keycloak.config.CachingOptions;
import org.keycloak.config.Option;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.StopServer;
import org.keycloak.it.junit5.extension.StopServer.Mode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DistributionTest(stopServer = Mode.BEFORE_BOOTSTRAP)
public class CacheEmbeddedMtlsDistTest {

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @RawDistOnly(reason = "Containers are immutable")
    public void testCacheEmbeddedMtlsDisabled(KeycloakRunner runner) {
        for (var option : Arrays.asList(
                CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE,
                CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE,
                CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD,
                CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD,
                CachingOptions.CACHE_EMBEDDED_MTLS_ROTATION,
                CachingOptions.CACHE_EMBEDDED_MTLS_KEY_EXCHANGE_GROUPS
        )) {
            var result = runner.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-enabled=false", "--%s=1".formatted(option.getKey()));
            result.assertError("Disabled option: '--%s'. Available only when property 'cache-embedded-mtls-enabled' is enabled".formatted(option.getKey()));
        }
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @RawDistOnly(reason = "Containers are immutable")
    public void testCacheEmbeddedMtlsFileValidation(KeycloakRunner runner) {
        doFileAndPasswordValidation(runner, CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE, CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD);
        doFileAndPasswordValidation(runner, CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE, CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD);
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @RawDistOnly(reason = "Containers are immutable")
    public void testCacheEmbeddedMtlsFileExistsValidation(KeycloakRunner runner) throws IOException {
        var result = runner.run(
              "start-dev",
              "--cache=ispn",
              "--cache-embedded-mtls-enabled=true",
              "--cache-embedded-mtls-key-store-file=keystore.p12",
              "--cache-embedded-mtls-key-store-password=password",
              "--cache-embedded-mtls-trust-store-file=truststore.p12",
              "--cache-embedded-mtls-trust-store-password=password"
              );
        result.assertError("The 'cache-embedded-mtls-key-store-file' file 'keystore.p12' does not exist");

        File keystore = Util.createTempFile("key", ".p12");
        result = runner.run(
              "start-dev",
              "--cache=ispn",
              "--cache-embedded-mtls-enabled=true",
              "--cache-embedded-mtls-key-store-file=%s".formatted(keystore.getAbsolutePath()),
              "--cache-embedded-mtls-key-store-password=password",
              "--cache-embedded-mtls-trust-store-file=truststore.p12",
              "--cache-embedded-mtls-trust-store-password=password"
        );
        result.assertError("The 'cache-embedded-mtls-trust-store-file' file 'truststore.p12' does not exist");
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @RawDistOnly(reason = "Containers are immutable")
    public void testCacheEmbeddedMtlsValidation(KeycloakRunner runner) {
        var key = CachingOptions.CACHE_EMBEDDED_MTLS_ROTATION.getKey();
        // test zero
        var result = runner.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-enabled=true", "--%s=0".formatted(key));
        result.assertError("JGroups MTLS certificate rotation in '%s' option must positive.".formatted(key));

        // test negative
        result = runner.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-enabled=true", "--%s=-1".formatted(key));
        result.assertError("JGroups MTLS certificate rotation in '%s' option must positive.".formatted(key));

        // test blank
        result = runner.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-enabled=true", "--%s=".formatted(key));
        result.assertError("Invalid empty value for option '--%s'".formatted(key));
    }

    @Test
    @RawDistOnly(reason = "Containers are immutable")
    public void testCacheEmbeddedMtlsEnabled(KeycloakRunner runner) {
        var result = runner.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-enabled=true");
        result.assertMessage("JGroups JDBC_PING discovery enabled.");
        result.assertMessage("JGroups Encryption enabled (mTLS).");
    }

    @Test
    @RawDistOnly(reason = "Containers are immutable")
    public void testCacheEmbeddedMtlsRejectsUnsupportedNamedGroup(KeycloakRunner runner) throws Exception {
        assumeTrue(supportedNamedGroups().length > 0, "TLS provider must report supported named groups");
        var result = runner.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-key-exchange-groups=not-a-tls-group");
        result.assertError("Unsupported TLS named group 'not-a-tls-group' in option 'cache-embedded-mtls-key-exchange-groups'");
    }

    @Test
    @RawDistOnly(reason = "Containers are immutable")
    public void testCacheEmbeddedMtlsHybridKeyExchange(KeycloakRunner runner) throws Exception {
        assumeTrue(Arrays.stream(supportedNamedGroups()).anyMatch("X25519MLKEM768"::equalsIgnoreCase),
                "TLS provider must support X25519MLKEM768");
        var result = runner.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-key-exchange-groups=X25519MLKEM768");
        result.assertMessage("JGroups JDBC_PING discovery enabled.");
        result.assertMessage("JGroups Encryption enabled (mTLS).");
    }

    @Test
    @RawDistOnly(reason = "Exercises local server restarts")
    public void testIdentityMigrationAndRollback(KeycloakRunner runner) throws Exception {
        assumeTrue(Runtime.version().feature() >= 21, "Per-connection signature configuration requires Java 21 or later");
        for (String algorithm : new String[]{"RSA", "ML-DSA-65", "RSA", "ML-DSA-65"}) {
            var result = runner.run("start-dev", "--cache=ispn",
                    "--cache-embedded-mtls-key-exchange-groups=X25519MLKEM768",
                    "--cache-embedded-mtls-identity-algorithm=" + algorithm);
            result.assertMessage("JGroups Encryption enabled (mTLS).");
            runner.stop();
        }
        var strict = runner.run("start-dev", "--cache=ispn",
                "--cache-embedded-mtls-key-exchange-groups=X25519MLKEM768",
                "--cache-embedded-mtls-identity-algorithm=ML-DSA-65");
        strict.assertMessage("JGroups Encryption enabled (mTLS).");
    }

    @Test
    @RawDistOnly(reason = "Exercises local server restarts")
    public void testAutomaticProviderSelectionForPqIdentity(KeycloakRunner runner) throws Exception {
        assumeTrue(Runtime.version().feature() >= 21, "Per-connection TLS configuration requires Java 21 or later");
        var result = runner.run("start-dev", "--cache=ispn",
                "--cache-embedded-mtls-key-exchange-groups=X25519MLKEM768",
                "--cache-embedded-mtls-identity-algorithm=ML-DSA-65");
        result.assertMessage("JGroups Encryption enabled (mTLS).");
        result.assertMessage("identity=ML-DSA-65, provider=");
        result.assertMessage("hybrid key exchange=required");
    }

    @Test
    @RawDistOnly(reason = "Exercises automatic provider selection on the local runtime")
    public void testAutomaticHybridKeyExchangeWithRsaIdentity(KeycloakRunner runner) throws Exception {
        assumeTrue(Runtime.version().feature() >= 21, "Per-connection TLS configuration requires Java 21 or later");
        var result = runner.run("start-dev", "--cache=ispn",
                "--cache-embedded-mtls-key-exchange-groups=X25519MLKEM768");
        result.assertMessage("JGroups Encryption enabled (mTLS).");
    }

    private static String[] supportedNamedGroups() throws Exception {
        try {
            var groups = (String[]) SSLParameters.class.getMethod("getNamedGroups")
                    .invoke(SSLContext.getDefault().getSupportedSSLParameters());
            return groups == null ? new String[0] : groups;
        } catch (NoSuchMethodException e) {
            return new String[0];
        }
    }

    private void doFileAndPasswordValidation(KeycloakRunner runner, Option<String> fileOption, Option<String> passwordOption) {
        var result = runner.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-enabled=true", "--%s=file".formatted(fileOption.getKey()));
        result.assertError("The option '%s' requires '%s' to be enabled.".formatted(fileOption.getKey(), passwordOption.getKey()));

        result = runner.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-enabled=true", "--%s=secret".formatted(passwordOption.getKey()));
        result.assertError("The option '%s' requires '%s' to be enabled.".formatted(passwordOption.getKey(), fileOption.getKey()));
    }
}
