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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.StopServer.Mode;
import org.keycloak.it.utils.RawKeycloakDistribution;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
     *
     * With the fix (configureTruststore @Consume CryptoProviderInitBuildItem),
     * setCryptoProvider registers BCFIPS first, so BCFKS type is available and
     * the server starts successfully.
     */
    @Test
    void testFipsWithBcfksTruststoreType(KeycloakRunner runner) {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        installBcFips(rawDist);

        rawDist.copyOrReplaceFileFromClasspath("/server.keystore.bcfks", Path.of("conf", "server.truststore.bcfks"));

        Path bcfksTruststorePath = rawDist.getDistPath().resolve("conf").resolve("server.truststore.bcfks").toAbsolutePath();

        runner.setEnvVar("JAVA_OPTS_APPEND",
                "-Djavax.net.ssl.trustStore=" + bcfksTruststorePath
                + " -Djavax.net.ssl.trustStoreType=BCFKS"
                + " -Djavax.net.ssl.trustStorePassword=passwordpassword");

        CLIResult cliResult = runner.run("--verbose", "start", "--features=fips");
        cliResult.assertStarted();
        cliResult.assertMessage("FIPS1402Provider created");
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

    @Test
    void testGeneratedTruststoreUsesBcfksInStrictMode(KeycloakRunner runner) {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        installBcFips(rawDist);

        rawDist.copyOrReplaceFileFromClasspath("/self-signed.pem", Path.of("conf", "truststores", "self-signed.pem"));

        CLIResult cliResult = runner.run("--verbose", "start", "--features=fips", "--fips-mode=strict");
        cliResult.assertStarted();
        cliResult.assertMessage("FIPS1402Provider created");

        assertTrue(Files.exists(rawDist.getDistPath().resolve("data").resolve("keycloak-truststore.bcfks")));
        assertFalse(Files.exists(rawDist.getDistPath().resolve("data").resolve("keycloak-truststore.p12")));
    }


    @Test
    void testTruststorePathsWithDefaultTruststoreInNonStrictMode(KeycloakRunner runner) {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        installBcFips(rawDist);

        rawDist.copyOrReplaceFileFromClasspath("/self-signed.pem", Path.of("conf", "self-signed.pem"));

        Path truststorePath = rawDist.getDistPath().resolve("conf").resolve("self-signed.pem").toAbsolutePath();

        CLIResult cliResult = runner.run("--verbose", "start", "--features=fips", "--fips-mode=non-strict",
                "--truststore-paths=" + truststorePath);
        cliResult.assertStarted();
        cliResult.assertMessage("FIPS1402Provider created");
    }

    @Test
    void testJksDefaultTruststoreFallbackInNonStrictMode(KeycloakRunner runner) throws Exception {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        installBcFips(rawDist);

        rawDist.copyOrReplaceFileFromClasspath("/self-signed.pem", Path.of("conf", "self-signed.pem"));
        Path truststorePath = rawDist.getDistPath().resolve("conf").resolve("self-signed.pem").toAbsolutePath();
        // Content is JKS, but the JVM is told it is PKCS12 to force the PKCS12 -> JKS fallback.
        Path cacerts = writeDefaultTruststore(rawDist, "cacerts.jks", "JKS", "changeit".toCharArray());

        runner.setEnvVar("JAVA_OPTS_APPEND",
                "-Djavax.net.ssl.trustStore=" + cacerts
                + " -Djavax.net.ssl.trustStoreType=PKCS12"
                + " -Djavax.net.ssl.trustStorePassword=changeit");

        CLIResult cliResult = runner.run("--verbose", "start", "--features=fips", "--fips-mode=non-strict",
                "--truststore-paths=" + truststorePath);
        cliResult.assertStarted();
        cliResult.assertMessage("FIPS1402Provider created");
    }

    @Test
    void testPkcs12DefaultTruststoreInNonStrictMode(KeycloakRunner runner) throws Exception {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        installBcFips(rawDist);

        rawDist.copyOrReplaceFileFromClasspath("/self-signed.pem", Path.of("conf", "self-signed.pem"));
        Path truststorePath = rawDist.getDistPath().resolve("conf").resolve("self-signed.pem").toAbsolutePath();
        Path cacerts = writeDefaultTruststore(rawDist, "cacerts.p12", "PKCS12", "changeit".toCharArray());

        runner.setEnvVar("JAVA_OPTS_APPEND",
                "-Djavax.net.ssl.trustStore=" + cacerts
                + " -Djavax.net.ssl.trustStoreType=PKCS12"
                + " -Djavax.net.ssl.trustStorePassword=changeit");

        CLIResult cliResult = runner.run("--verbose", "start", "--features=fips", "--fips-mode=non-strict",
                "--truststore-paths=" + truststorePath);
        cliResult.assertStarted();
        cliResult.assertMessage("FIPS1402Provider created");
    }

    @Test
    void testCorruptTruststorePathFailsInNonStrictMode(KeycloakRunner runner) throws Exception {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        installBcFips(rawDist);

        // A genuinely unreadable truststore-paths entry must fail startup: the null/"" and JKS fallbacks
        // must not silently swallow a corrupt file.
        Path corrupt = rawDist.getDistPath().resolve("conf").resolve("corrupt.p12");
        Files.createDirectories(corrupt.getParent());
        Files.writeString(corrupt, "this is not a valid keystore");

        CLIResult cliResult = runner.run("--verbose", "start", "--features=fips", "--fips-mode=non-strict",
                "--truststore-paths=" + corrupt.toAbsolutePath());
        cliResult.assertError("Failed to initialize truststore");
    }

    @Test
    void testPasswordlessPkcs12TruststorePathInNonStrictMode(KeycloakRunner runner) throws Exception {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        installBcFips(rawDist);

        // No-MAC PKCS12 (stored with a null password), the only PKCS12 flavour truststore-paths has ever supported.
        Path truststorePath = writeDefaultTruststore(rawDist, "self-signed.p12", "PKCS12", null);

        CLIResult cliResult = runner.run("--verbose", "start", "--features=fips", "--fips-mode=non-strict",
                "--truststore-paths=" + truststorePath);
        cliResult.assertStarted();
        cliResult.assertMessage("FIPS1402Provider created");
    }

    private void installBcFips(RawKeycloakDistribution rawDist) {
        rawDist.copyProvider("org.bouncycastle", "bc-fips");
        rawDist.copyProvider("org.bouncycastle", "bctls-fips");
        rawDist.copyProvider("org.bouncycastle", "bcpkix-fips");
        rawDist.copyProvider("org.bouncycastle", "bcutil-fips");
    }

    /**
     * Writes a cacerts-style keystore holding the {@code /self-signed.pem} certificate into {@code conf/}. The
     * keystore type ({@code JKS} or {@code PKCS12}) and the store password (may be {@code null} for a no-MAC
     * PKCS12) are chosen by the caller. Returns the absolute path to the written file.
     */
    private Path writeDefaultTruststore(RawKeycloakDistribution rawDist, String fileName, String keystoreType,
            char[] password) throws Exception {
        X509Certificate certificate;
        try (InputStream pem = getClass().getResourceAsStream("/self-signed.pem")) {
            certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(pem);
        }
        KeyStore keyStore = KeyStore.getInstance(keystoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("self-signed", certificate);
        Path path = rawDist.getDistPath().resolve("conf").resolve(fileName);
        Files.createDirectories(path.getParent());
        try (var out = Files.newOutputStream(path)) {
            keyStore.store(out, password);
        }
        return path.toAbsolutePath();
    }
}
