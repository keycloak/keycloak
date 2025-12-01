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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
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

import io.quarkus.test.junit.main.Launch;
import org.apache.commons.io.FileUtils;
import org.approvaltests.Approvals;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

import static org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG;

@DistributionTest
@RawDistOnly(reason = "Verifying the help message output doesn't need long spin-up of docker dist tests.")
public class HelpCommandDistTest {

    public static final String REPLACE_EXPECTED = "KEYCLOAK_REPLACE_EXPECTED";

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

                if (OS.WINDOWS.isCurrentOs()) {
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

        String osName = System.getProperty("os.name");
        if(osName.toLowerCase(Locale.ROOT).contains("windows")) {
            // On Windows, all output should have at least one "kc.bat" in it.
            MatcherAssert.assertThat(output, Matchers.containsString("kc.bat"));
            output = output.replaceAll("kc.bat", "kc.sh");
            output = output.replaceAll(Pattern.quote("data\\log\\"), "data/log/");
            // line wrap which looks differently due to ".bat" vs. ".sh"
            output = output.replaceAll("including\nbuild ", "including build\n");
        }

        try {
            Approvals.verify(output);
        } catch (Error cause) {
            if ("true".equals(System.getenv(REPLACE_EXPECTED))) {
                try {
                    FileUtils.write(Approvals.createApprovalNamer().getApprovedFile(".txt"), output,
                            StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to assert help, and could not replace expected", cause);
                }
            } else {
                throw cause;
            }
        }
    }
}
