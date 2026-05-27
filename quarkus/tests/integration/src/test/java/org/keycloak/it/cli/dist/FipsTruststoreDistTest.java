/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
import java.nio.file.Path;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.StopServer.Mode;
import org.keycloak.it.utils.RawKeycloakDistribution;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Reproduces <a href="https://github.com/keycloak/keycloak/issues/49139">#49139</a>:
 * When FIPS is enabled with truststore configuration, the configureTruststore build
 * step lacks a dependency on CryptoProviderInitBuildItem, making its execution order
 * relative to setCryptoProvider non-deterministic. This causes failures when the
 * truststore code requires BCFIPS-provided keystore types or FIPS-compliant PKCS12.
 */
@DistributionTest(stopServer = Mode.MANUAL, defaultOptions = { "--db=dev-file", "--http-enabled=true", "--hostname-strict=false" })
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SLOW)
public class FipsTruststoreDistTest {

    /**
     * Scenario 1: JVM truststore properties set to BCFKS type.
     * When javax.net.ssl.trustStoreType=BCFKS is set (as typical on FIPS hosts),
     * TruststoreBuilder.includeDefaultTruststore() attempts to load the truststore
     * as BCFKS. If configureTruststore runs before setCryptoProvider registers the
     * BCFIPS provider, KeyStore.getInstance("BCFKS") fails with "BCFKS not found".
     */
    @Test
    void testFipsWithBcfksTruststoreTypeFailsWhenProviderNotRegistered(KeycloakRunner runner) {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        installBcFips(rawDist);

        rawDist.copyOrReplaceFileFromClasspath("/self-signed.pem", Path.of("conf", "self-signed.pem"));

        Path truststorePath = rawDist.getDistPath().resolve("conf").resolve("self-signed.pem").toAbsolutePath();

        String cacertsPath = System.getProperty("java.home") + File.separator + "lib"
                + File.separator + "security" + File.separator + "cacerts";

        // Simulates FIPS host where javax.net.ssl.trustStore points to a BCFKS
        // truststore. On the real system the truststore IS BCFKS format, but the
        // failure occurs because the BCFIPS provider hasn't been registered yet
        // when configureTruststore runs — so KeyStore.getInstance("BCFKS") fails.
        // Here we point to cacerts (not BCFKS) which triggers the same loadStore
        // failure path.
        runner.setEnvVar("JAVA_OPTS_APPEND",
                "-Djavax.net.ssl.trustStore=" + cacertsPath
                + " -Djavax.net.ssl.trustStoreType=BCFKS");

        CLIResult cliResult = runner.run("--verbose", "start", "--features=fips",
                "--truststore-paths=" + truststorePath);
        cliResult.assertError("Failed to initialize truststore");
        cliResult.assertError("cacerts");
    }

    /**
     * Scenario 2: BC-FIPS jars bundled but KC_FEATURES does NOT include fips
     * (non-FIPS host deployment). KC_TRUSTSTORE_PATHS is set.
     * The auto-build selects DefaultCryptoProvider since fips feature is not enabled.
     * DefaultCryptoProvider instantiates org.bouncycastle.jce.provider.BouncyCastleProvider
     * but the BC-FIPS jars on the classpath are incompatible with the non-FIPS BC
     * provider, causing NoSuchMethodError on CryptoServicesRegistrar.checkConstraints.
     */
    @Test
    void testBcFipsBundledWithoutFipsFeatureFailsOnCryptoProvider(KeycloakRunner runner) {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        installBcFips(rawDist);

        rawDist.copyOrReplaceFileFromClasspath("/self-signed.pem", Path.of("conf", "self-signed.pem"));

        Path truststorePath = rawDist.getDistPath().resolve("conf").resolve("self-signed.pem").toAbsolutePath();

        // KC_FEATURES does NOT include fips — simulates non-FIPS host deployment
        // where the same image (with BC-FIPS bundled) is used
        CLIResult cliResult = runner.run("--verbose", "start",
                "--truststore-paths=" + truststorePath);
        cliResult.assertError("Unexpected error when configuring the crypto provider");
    }

    private void installBcFips(RawKeycloakDistribution rawDist) {
        rawDist.copyProvider("org.bouncycastle", "bc-fips");
        rawDist.copyProvider("org.bouncycastle", "bctls-fips");
        rawDist.copyProvider("org.bouncycastle", "bcpkix-fips");
        rawDist.copyProvider("org.bouncycastle", "bcutil-fips");
    }
}