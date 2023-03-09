/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import java.util.List;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.quarkus.runtime.cli.command.Export;
import org.keycloak.quarkus.runtime.cli.command.Import;
import org.keycloak.quarkus.runtime.cli.command.Start;

@DistributionTest
@RawDistOnly(reason = "Verifying the verbose flag output doesn't need long spin-up of docker dist tests.")
public class VerboseCommandDistTest {

    // Test function of 'start' command followed by '-v' / '--verbose' flag
    @Test
    @Launch({ Start.NAME, "-v" })
    void testStartVerboseShort(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Unknown option:");
    }

    @Test
    @Launch({ Start.NAME, "--verbose" })
    void testStartVerbose(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Unknown option:");
    }

    // Test function of '-v' / '--verbose' flag specified in reverse order too
    @Test
    @Launch({ "-v",  Start.NAME })
    void testVerboseShortStart(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Unknown option:");
    }

    @Test
    @Launch({ "--verbose",  Start.NAME })
    void testVerboseStart(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Unknown option:");
    }

    // Check -v / --verbose flag called prior to 'export' command
    @Test
    @Launch({ "-v",  Export.NAME })
    void testVerboseShortExport(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Unknown option:");
    }

    @Test
    @Launch({ "--verbose",  Export.NAME })
    void testVerboseExport(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Unknown option:");
    }

    // Check -v / --verbose flag called after the 'export' command
    @Test
    @Launch({ Export.NAME, "-v" })
    void testExportVerboseShort(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Unknown option:");
    }

    @Test
    @Launch({ Export.NAME, "--verbose" })
    void testExportVerbose(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Unknown option:");
    }

    // Check -v / --verbose flag called prior to 'import' command
    @Test
    @Launch({ "-v",  Import.NAME })
    void testVerboseShortImport(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Unknown option:");
    }

    @Test
    @Launch({ "--verbose",  Import.NAME })
    void testVerboseImport(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Unknown option:");
    }

    // Check -v / --verbose flag called after the 'import' command
    @Test
    @Launch({ Import.NAME, "-v" })
    void testImportVerboseShort(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Unknown option:");
    }

    @Test
    @Launch({ Import.NAME, "--verbose" })
    void testImportVerbose(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Unknown option:");
    }

    // Finally check verbose flag is listed as available in the help message output of all subcommands
    @Test
    void testVerboseFlagInHelpMessageOfAllCommands(KeycloakDistribution dist) {
        for (String helpCmd : List.of("-h", "--help")) {
            for (String cmd : List.of("", "build", "start", "start-dev", "export", "import", "show-config", "tools", "tools completion")) {
                String debugOption = "--debug";

                if (OS.WINDOWS.isCurrentOs()) {
                    debugOption = "--debug=8787";
                }

                CLIResult cliResult = dist.run(debugOption, cmd, helpCmd);
                // Confirm -v / --verbose flag is listed in help message of a particular cmd
                cliResult.assertMessage("-v, --verbose");
                assertSingleJvmStarted(cliResult);
            }
        }
    }

    private void assertSingleJvmStarted(CLIResult run) {
        assertThat(run.getOutputStream().stream().filter(s -> s.contains("Listening for transport dt_socket")).count(), equalTo(Long.valueOf(1)));
    }
}
