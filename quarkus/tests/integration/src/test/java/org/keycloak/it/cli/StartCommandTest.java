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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.CLITest;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

@CLITest
public class StartCommandTest {

    @Test
    @Launch({ "start", "--hostname-strict=false" })
    void failNoTls(LaunchResult result) {
        assertTrue(result.getOutput().contains("Key material not provided to setup HTTPS"),
                () -> "The Output:\n" + result.getOutput() + "doesn't contains the expected string.");
    }

    @Test
    @Launch({ "--profile=dev", "start" })
    void failUsingDevProfile(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("ERROR: You can not 'start' the server in development mode. Please re-build the server first, using 'kc.sh build' for the default production mode."),
                () -> "The Output:\n" + result.getErrorOutput() + "doesn't contains the expected string.");
    }

    @Test
    @Launch({ "-v", "start", "--http-enabled=true", "--hostname-strict=false" })
    void testHttpEnabled(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStarted();
    }

    @Test
    @Launch({ "-v", "start", "--db=dev-mem" })
    void failBuildPropertyNotAvailable(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("Unknown option: '--db=dev-mem'");
    }
}
