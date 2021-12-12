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

package org.keycloak.it.cli;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.CLITest;
import org.keycloak.quarkus.runtime.cli.command.Main;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

@CLITest
public class HelpCommandTest {

    @Test
    @Launch({})
    void testDefaultToHelp(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertHelp("kc.sh");
    }

    @Test
    @Launch({ "--help" })
    void testHelpCommand(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertHelp("kc.sh");
    }

    @Test
    @Launch({ "start", "--help" })
    void testStartHelpCommand(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertHelp("start");
    }

    @Test
    @Launch({ "start-dev", "--help" })
    void testStartDevCommand(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertHelp("start-dev");
    }

    @Test
    @Launch({ "build", "--help" })
    void testBuildCommand(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertHelp("build");
    }
}
