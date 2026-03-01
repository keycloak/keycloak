/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import java.nio.file.Path;

import org.keycloak.crypto.fips.FIPS1402Provider;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DistributionTest(keepAlive = true, defaultOptions = { "--db=dev-file", "--features=fips", "--http-enabled=true", "--hostname-strict=false" })
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SLOW)
public class FipsDistTest {

    private static final String BCFIPS_VERSION = "BCFIPS version 2.0102";

    @Test
    void testFipsNonApprovedMode(KeycloakDistribution dist) {
        runOnFipsEnabledDistribution(dist, () -> {
            CLIResult cliResult = dist.run("start");
            cliResult.assertStarted();
            // Not shown as FIPS is not a preview anymore
            cliResult.assertMessageWasShownExactlyNumberOfTimes("Preview features enabled: fips:v1", 0);
            cliResult.assertMessage("FIPS1402Provider created: KC(" + BCFIPS_VERSION + ", FIPS-JVM: " + FIPS1402Provider.isSystemFipsEnabled() + ")");
        });
    }

    @Test
    void testFipsApprovedMode(KeycloakDistribution dist) {
        runOnFipsEnabledDistribution(dist, () -> {
            dist.setEnvVar("KC_BOOTSTRAP_ADMIN_USERNAME", "admin");
            dist.setEnvVar("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin");

            CLIResult cliResult = dist.run("start", "--fips-mode=strict");
            cliResult.assertMessage("password must be at least 112 bits");
            cliResult.assertMessage("FIPS1402Provider created: KC(" + BCFIPS_VERSION + " Approved Mode, FIPS-JVM: " + FIPS1402Provider.isSystemFipsEnabled() + ")");

            dist.setEnvVar("KC_BOOTSTRAP_ADMIN_PASSWORD", "adminadminadmin");
            cliResult = dist.run("start", "--fips-mode=strict");
            cliResult.assertStarted();
            cliResult.assertMessage("Created temporary admin user with username admin");
        });
    }

    @Test
    @Launch({ "start", "--fips-mode=non-strict" })
    void failStartDueToMissingFipsDependencies(CLIResult cliResult) {
        cliResult.assertError("Failed to configure FIPS. Make sure you have added the Bouncy Castle FIPS dependencies to the 'providers' directory.");
    }

    @Test
    void testUnsupportedHttpsJksKeyStoreInStrictMode(KeycloakDistribution dist) {
        runOnFipsEnabledDistribution(dist, () -> {
            dist.copyOrReplaceFileFromClasspath("/server.keystore", Path.of("conf", "server.keystore"));
            CLIResult cliResult = dist.run("start", "--fips-mode=strict");
            cliResult.assertMessage("ERROR: java.lang.IllegalArgumentException: malformed sequence");
        });
    }

    @Test
    void testHttpsBcfksKeyStoreInStrictMode(KeycloakDistribution dist) {
        runOnFipsEnabledDistribution(dist, () -> {
            dist.copyOrReplaceFileFromClasspath("/server.keystore.bcfks", Path.of("conf", "server.keystore"));
            CLIResult cliResult = dist.run("start", "--fips-mode=strict", "--https-key-store-password=passwordpassword");
            cliResult.assertStarted();
        });
    }

    @Test
    void testHttpsBcfksTrustStoreInStrictMode(KeycloakDistribution dist) {
        runOnFipsEnabledDistribution(dist, () -> {
            dist.copyOrReplaceFileFromClasspath("/server.keystore.bcfks", Path.of("conf", "server.keystore"));

            RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
            Path truststorePath = rawDist.getDistPath().resolve("conf").resolve("server.keystore").toAbsolutePath();

            // https-trust-store-type should be automatically set to bcfks in fips-mode=strict
            CLIResult cliResult = dist.run("--verbose", "start", "--fips-mode=strict", "--https-key-store-password=passwordpassword",
                    "--https-trust-store-file=" + truststorePath, "--https-trust-store-password=passwordpassword");
            cliResult.assertStarted();
        });
    }

    @Test
    void testUnencryptedPkcs12TrustStoreInStrictMode(KeycloakDistribution dist) {
        runOnFipsEnabledDistribution(dist, () -> {
            String truststoreName = "keycloak-truststore.p12";
            dist.copyOrReplaceFileFromClasspath("/" + truststoreName, Path.of("conf", truststoreName));

            RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
            Path truststorePath = rawDist.getDistPath().resolve("conf").resolve(truststoreName).toAbsolutePath();

            CLIResult cliResult = dist.run("--verbose", "start", "--fips-mode=strict", "--truststore-paths=" + truststorePath);
            cliResult.assertStarted();
        });
    }

    @Test
    void testUnsupportedHttpsPkcs12KeyStoreInStrictMode(KeycloakDistribution dist) {
        runOnFipsEnabledDistribution(dist, () -> {
            dist.copyOrReplaceFileFromClasspath("/server.keystore.pkcs12", Path.of("conf", "server.keystore"));
            CLIResult cliResult = dist.run("start", "--fips-mode=strict", "--https-key-store-password=passwordpassword");
            cliResult.assertMessage("ERROR: java.lang.IllegalArgumentException: malformed sequence");
        });
    }

    @Test
    void testHttpsPkcs12KeyStoreInNonApprovedMode(KeycloakDistribution dist) {
        runOnFipsEnabledDistribution(dist, () -> {
            dist.copyOrReplaceFileFromClasspath("/server.keystore.pkcs12", Path.of("conf", "server.keystore"));
            CLIResult cliResult = dist.run("start", "--fips-mode=non-strict", "--https-key-store-password=passwordpassword");
            cliResult.assertStarted();
        });
    }

    @Test
    void testHttpsPkcs12TrustStoreInNonApprovedMode(KeycloakDistribution dist) {
        runOnFipsEnabledDistribution(dist, () -> {
            dist.copyOrReplaceFileFromClasspath("/server.keystore.pkcs12", Path.of("conf", "server.keystore"));

            RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
            Path truststorePath = rawDist.getDistPath().resolve("conf").resolve("server.keystore").toAbsolutePath();

            CLIResult cliResult = dist.run("--verbose", "start", "--fips-mode=non-strict", "--https-key-store-password=passwordpassword",
                    "--https-trust-store-file=" + truststorePath, "--https-trust-store-password=passwordpassword");
            cliResult.assertMessage("Unable to determine 'https-trust-store-type' automatically. Adjust the file extension or specify the property.");
            dist.stop();

            dist.copyOrReplaceFileFromClasspath("/server.keystore.pkcs12", Path.of("conf", "server.p12"));

            rawDist = dist.unwrap(RawKeycloakDistribution.class);
            truststorePath = rawDist.getDistPath().resolve("conf").resolve("server.p12").toAbsolutePath();

            cliResult = dist.run("--verbose", "start", "--fips-mode=non-strict", "--https-key-store-password=passwordpassword",
                    "--https-trust-store-file=" + truststorePath, "--https-trust-store-password=passwordpassword");
            cliResult.assertStarted();
        });
    }

    private void runOnFipsEnabledDistribution(KeycloakDistribution dist, Runnable runnable) {
        installBcFips(dist);
        runnable.run();
    }

    private void installBcFips(KeycloakDistribution dist) {
        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        rawDist.copyProvider("org.bouncycastle", "bc-fips");
        rawDist.copyProvider("org.bouncycastle", "bctls-fips");
        rawDist.copyProvider("org.bouncycastle", "bcpkix-fips");
        rawDist.copyProvider("org.bouncycastle", "bcutil-fips");
    }

}
