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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.cli.StartDevCommandTest;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;

import static org.junit.jupiter.api.Assertions.assertFalse;

@DistributionTest(reInstall = DistributionTest.ReInstall.NEVER)
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StartDevCommandDistTest extends StartDevCommandTest {

    private static final String DEFAULT_MESSAGE_WHEN_REAUGMENTATION_HAPPENS = "Updating the configuration and installing your custom providers, if any. Please wait.";

    @Test
    @Launch({ "start-dev", "--db=dev-mem" })
    @Order(1)
    void testStartDevShouldReAugWhenDBIsSet(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage(DEFAULT_MESSAGE_WHEN_REAUGMENTATION_HAPPENS);
        // assertFalse(cliResult.getOutput().contains(DEFAULT_MESSAGE_WHEN_REAUGMENTATION_HAPPENS));
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev" })
    @Order(2)
    void testStartDevAlwaysReAugEvenWithoutAnySet(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage(DEFAULT_MESSAGE_WHEN_REAUGMENTATION_HAPPENS);
        // assertFalse(cliResult.getOutput().contains(DEFAULT_MESSAGE_WHEN_REAUGMENTATION_HAPPENS));
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev", "--no-auto-build" })
    @Order(3)
    void testStartDevReAugIgnoringNoAutoBuildOption(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("The --no-auto-build' option has no effect for 'start-dev' command, which always run '--auto-build' by default.");
        cliResult.assertNoMessage(DEFAULT_MESSAGE_WHEN_REAUGMENTATION_HAPPENS);
        cliResult.assertError("Unknown option: '--no-auto-build'");
    }

}
