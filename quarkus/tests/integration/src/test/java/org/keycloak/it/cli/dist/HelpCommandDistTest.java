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

import java.util.List;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.command.BootstrapAdmin;
import org.keycloak.quarkus.runtime.cli.command.BootstrapAdminService;
import org.keycloak.quarkus.runtime.cli.command.BootstrapAdminUser;
import org.keycloak.quarkus.runtime.cli.command.Build;
import org.keycloak.quarkus.runtime.cli.command.Export;
import org.keycloak.quarkus.runtime.cli.command.Import;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.quarkus.runtime.cli.command.StartDev;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibility;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibilityCheck;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibilityMetadata;

import com.spun.util.io.FileUtils;
import io.quarkus.test.junit.main.Launch;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.approvaltests.core.VerifyResult;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG;

@DistributionTest
@RawDistOnly(reason = "Verifying the help message output doesn't need long spin-up of docker dist tests.")
public class HelpCommandDistTest {

    @Test
    @Launch({})
    void testDefaultToHelp(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({"--help"})
    void testHelp(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ "-h" })
    void testHelpShort(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Start.NAME, "--help", OPTIMIZED_BUILD_OPTION_LONG})
    void testStartOptimizedHelp(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Start.NAME, "--help" })
    void testStartHelp(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Start.NAME, "--optimized", "--help-all" })
    void testStartOptimizedHelpAll(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ StartDev.NAME, "--help" })
    void testStartDevHelp(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ StartDev.NAME, "--help-all" })
    void testStartDevHelpAll(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Start.NAME, "--help-all" })
    void testStartHelpAll(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Build.NAME, "--help" })
    void testBuildHelp(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Export.NAME, "--help" })
    void testExportHelp(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Export.NAME, "--help-all" })
    void testExportHelpAll(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Import.NAME, "--help" })
    void testImportHelp(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ Import.NAME, "--help-all" })
    void testImportHelpAll(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ BootstrapAdmin.NAME, "--help" })
    void testBootstrapAdmin(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ BootstrapAdmin.NAME, BootstrapAdminUser.NAME, "--help" })
    void testBootstrapAdminUser(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ BootstrapAdmin.NAME, BootstrapAdminService.NAME, "--help" })
    void testBootstrapAdminService(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ UpdateCompatibility.NAME, "--help" })
    void testUpdateCompatibilityHelp(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, "--help" })
    void testUpdateCompatibilityMetadataHelp(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, "--help-all" })
    void testUpdateCompatibilityMetadataHelpAll(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, "--help" })
    void testUpdateCompatibilityCheckHelp(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    @Launch({ UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, "--help-all" })
    void testUpdateCompatibilityCheckHelpAll(CLIResult cliResult) {
        assertHelp(cliResult);
    }

    @Test
    public void testHelpDoesNotStartReAugJvm(KeycloakDistribution dist) {
        for (String helpCmd : List.of("-h", "--help", "--help-all")) {
            for (String cmd : List.of("", "start", "start-dev", "build")) {
                String debugOption = "--debug";

                if (Environment.isWindows()) {
                    debugOption = "--debug=8787";
                }

                CLIResult run = dist.run(debugOption, cmd, helpCmd);
                assertSingleJvmStarted(run);
            }
        }
    }

    private void assertSingleJvmStarted(CLIResult cliResult) {
        cliResult.assertMessageWasShownExactlyNumberOfTimes("Listening for transport dt_socket", 1);
    }

    private void assertHelp(CLIResult cliResult) {
        // normalize the output to prevent changes around the feature toggles or events to mark the output to differ
        String output = cliResult.getOutput()
                .replaceAll("((Disables|Enables) a set of one or more features. Possible values are: )[^.]{30,}", "$1<...>")
                .replaceAll("(create a metric.\\s+Possible values are:)[^.]{30,}.[^.]*.", "$1<...>");

        if (Environment.isWindows()) {
            MatcherAssert.assertThat(output, Matchers.containsString("kc.bat"));
            output = output
                    .replace("kc.bat", "kc.sh")
                    .replace("data\\log\\", "data/log/")
                    .replace("including\nbuild ", "including build\n");
        }

        // Custom comparator that strips Windows-specific lines from the approved file on non-Windows platforms
        Options options = new Options().withComparator((receivedFile, approvedFile) -> {
            String received = FileUtils.readFile(receivedFile);
            String approved = FileUtils.readFile(approvedFile);

            if (!Environment.isWindows()) {
                approved = stripWindowsServiceLines(approved);
            }
            return VerifyResult.from(approved.equals(received));
        });

        Approvals.verify(output, options);
    }

    private String stripWindowsServiceLines(String text) {
        return text
                .replaceAll("(?m)^ {4}windows-service\\s+Manage Keycloak as a Windows service\\.\\R", "")
                .replaceAll("(?m)^ {6}install\\s+Install Keycloak as a Windows service\\.\\R", "")
                .replaceAll("(?m)^ {6}uninstall\\s+Uninstall Keycloak Windows service\\.\\R", "");
    }
}
