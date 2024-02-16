/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StartDevCommandDistTest {

    @Test
    @Launch({ "start-dev" })
    void testDevModeWarning(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev", "--db=dev-mem" })
    void testBuildPropertyAvailable(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev", "--debug" })
    void testStartDevShouldStartTwoJVMs(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessageWasShownExactlyNumberOfTimes("Listening for transport dt_socket at address:", 2);
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "build", "--debug" })
    void testBuildMustNotRunTwoJVMs(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessageWasShownExactlyNumberOfTimes("Listening for transport dt_socket at address:", 1);
        cliResult.assertBuild();
    }

    @Test
    @Launch({ "start-dev", "--verbose" })
    void testVerboseAfterCommand(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
    }

}
