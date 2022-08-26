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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.keycloak.it.cli.HelpCommandTest;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;

@DistributionTest
@RawDistOnly(reason = "Verifying the help message output doesn't need long spin-up of docker dist tests.")
public class HelpCommandDistTest extends HelpCommandTest {

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
}
