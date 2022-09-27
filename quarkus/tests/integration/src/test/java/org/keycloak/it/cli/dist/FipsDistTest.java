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

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

@DistributionTest(reInstall = DistributionTest.ReInstall.BEFORE_TEST)
@RawDistOnly(reason = "Containers are immutable")
public class FipsDistTest {

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--fips-mode=enabled", "--cache=local", "--log-level=org.keycloak.common.crypto.CryptoIntegration:trace" })
    @BeforeStartDistribution(FipsDistTest.InstallBcFipsDependencies.class)
    void testFipsNonApprovedMode(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStarted();
        cliResult.assertMessage("Java security providers: [ \n"
                + " KC(BCFIPS version 1.000203) version 1.0 - class org.keycloak.crypto.fips.KeycloakFipsSecurityProvider");
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--fips-mode=strict", "--cache=local", "--log-level=org.keycloak.common.crypto.CryptoIntegration:trace" })
    @BeforeStartDistribution(FipsDistTest.InstallBcFipsDependencies.class)
    void testFipsApprovedMode(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStarted();
        cliResult.assertMessage("org.bouncycastle.crypto.fips.FipsUnapprovedOperationError: password must be at least 112 bits");
        cliResult.assertMessage("Java security providers: [ \n"
                + " KC(BCFIPS version 1.000203 Approved Mode) version 1.0 - class org.keycloak.crypto.fips.KeycloakFipsSecurityProvider");
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--fips-mode=enabled", "--cache=local", "--log-level=org.keycloak.common.crypto.CryptoIntegration:trace" })
    void failStartDueToMissingFipsDependencies(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("Failed to configure FIPS. Make sure you have added the Bouncy Castle FIPS dependencies to the 'providers' directory.");
    }

    public static class InstallBcFipsDependencies implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            RawKeycloakDistribution rawDist = (RawKeycloakDistribution) distribution;
            rawDist.copyProvider("org.bouncycastle", "bc-fips");
            rawDist.copyProvider("org.bouncycastle", "bctls-fips");
            rawDist.copyProvider("org.bouncycastle", "bcpkix-fips");
        }
    }
}
