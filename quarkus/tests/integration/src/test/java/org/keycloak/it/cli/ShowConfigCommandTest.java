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
import org.keycloak.it.junit5.extension.CLITest;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.keycloak.quarkus.runtime.cli.command.ShowConfig;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import static org.keycloak.quarkus.runtime.cli.command.Main.CONFIG_FILE_LONG_NAME;

@CLITest
public class ShowConfigCommandTest {

    @Test
    @Launch({ ShowConfig.NAME })
    void testShowConfigCommandShowsRuntimeConfig(LaunchResult result) {
        Assertions.assertTrue(result.getOutput()
                .contains("Current Configuration"));
    }

    @Test
    @Launch({ ShowConfig.NAME, "all" })
    void testShowConfigCommandWithAllShowsAllProfiles(LaunchResult result) {
        Assertions.assertTrue(result.getOutput()
                .contains("Current Configuration"));
        Assertions.assertTrue(result.getOutput()
                .contains("Quarkus Configuration"));
    }

    @Test
    @Launch({ CONFIG_FILE_LONG_NAME+"=src/test/resources/ShowConfigCommandTest/keycloak.conf", ShowConfig.NAME, "all" })
    void testShowConfigCommandHidesCredentialsInProfiles(LaunchResult result) {
        String output = result.getOutput();
        Assertions.assertFalse(output.contains("testpw1"));
        Assertions.assertFalse(output.contains("testpw2"));
        Assertions.assertFalse(output.contains("testpw3"));
        Assertions.assertTrue(output.contains("kc.db-password =  " + PropertyMappers.VALUE_MASK));
    }
}
