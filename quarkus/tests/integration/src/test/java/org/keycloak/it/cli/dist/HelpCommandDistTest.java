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

import static org.junit.Assert.assertEquals;
import static org.keycloak.it.cli.dist.GelfRemovedTest.INCLUDE_GELF_PROPERTY;
import static org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG;

import java.util.List;

import org.approvaltests.Approvals;
import org.approvaltests.namer.NamedEnvironment;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.keycloak.it.approvaltests.KcNamerFactory;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.quarkus.runtime.cli.command.Build;
import org.keycloak.quarkus.runtime.cli.command.Export;
import org.keycloak.quarkus.runtime.cli.command.Import;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.quarkus.runtime.cli.command.StartDev;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

@DistributionTest
@RawDistOnly(reason = "Verifying the help message output doesn't need long spin-up of docker dist tests.")
public class HelpCommandDistTest {

    @BeforeAll
    public static void assumeGelfEnabled() {
        Assumptions.assumeTrue(Boolean.getBoolean(INCLUDE_GELF_PROPERTY), "Assume GELF support is given in order to simplify these test cases");
    }

    @Test
    @Launch({})
    void testDefaultToHelp(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertHelp(cliResult);
    }

    @Test
    @Launch({"--help"})
    void testHelp(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertHelp(cliResult);
    }

    @Test
    @Launch({ "-h" })
    void testHelpShort(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Start.NAME, "--help", OPTIMIZED_BUILD_OPTION_LONG})
    void testStartOptimizedHelp(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Start.NAME, "--help" })
    void testStartHelp(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Start.NAME, "--optimized", "--help-all" })
    void testStartOptimizedHelpAll(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertHelp(cliResult);
    }

    @Test
    @Launch({ StartDev.NAME, "--help" })
    void testStartDevHelp(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertHelp(cliResult);
    }

    @Test
    @Launch({ StartDev.NAME, "--help-all" })
    void testStartDevHelpAll(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Start.NAME, "--help-all" })
    void testStartHelpAll(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Build.NAME, "--help" })
    void testBuildHelp(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Export.NAME, "--help" })
    void testExportHelp(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Export.NAME, "--help-all" })
    void testExportHelpAll(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Import.NAME, "--help" })
    void testImportHelp(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Import.NAME, "--help-all" })
    void testImportHelpAll(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertHelp(cliResult);
    }

    @Test
    public void testHelpDoesNotStartReAugJvm(KeycloakDistribution dist) {
        for (String helpCmd : List.of("-h", "--help", "--help-all")) {
            for (String cmd : List.of("", "start", "start-dev", "build")) {
                String debugOption = "--debug";

                if (OS.WINDOWS.isCurrentOs()) {
                    debugOption = "--debug=8787";
                }

                CLIResult run = dist.run(debugOption, cmd, helpCmd);
                assertSingleJvmStarted(run);
            }
        }
    }

    private void assertSingleJvmStarted(CLIResult run) {
        assertEquals(1, run.getOutputStream().stream().filter(s -> s.contains("Listening for transport dt_socket")).count());
    }

    private void assertHelp(CLIResult result) {
        try (NamedEnvironment env = KcNamerFactory.asWindowsOsSpecificTest()) {
            Approvals.verify(result.getOutput());
        } catch (Exception cause) {
            throw new RuntimeException("Failed to assert help", cause);
        }
    }
}
