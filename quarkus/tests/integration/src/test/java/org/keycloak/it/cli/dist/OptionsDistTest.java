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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.keycloak.quarkus.runtime.cli.command.Main.CONFIG_FILE_LONG_NAME;

@DistributionTest
public class OptionsDistTest {

    @Test
    @Launch({"build", "--db=invalid"})
    public void failInvalidOptionValue(LaunchResult result) {
        Assertions.assertTrue(result.getErrorOutput().contains("Invalid value for option '--db': invalid. Expected values are: dev-file, dev-mem, mariadb, mssql, mysql, oracle, postgres"));
    }

    @Test
    @Launch({"start-dev", "--test=invalid"})
    public void testServerDoesNotStartIfValidationFailDuringReAugStartDev(LaunchResult result) {
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Unknown option: '--test'")).count());
    }

    @Test
    @Launch({"start", "--test=invalid"})
    public void testServerDoesNotStartIfValidationFailDuringReAugStart(LaunchResult result) {
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Unknown option: '--test'")).count());
    }

    @Test
    @RawDistOnly(reason = "Raw is enough and we avoid issues with including custom conf file in the container")
    public void testExpressionsInConfigFile(KeycloakDistribution distribution) {
        distribution.setEnvVar("MY_LOG_LEVEL", "debug");
        CLIResult result = distribution.run(CONFIG_FILE_LONG_NAME + "=" + Paths.get("src/test/resources/OptionsDistTest/keycloak.conf").toAbsolutePath().normalize(), "start-dev");
        result.assertMessage("DEBUG [org.keycloak");
    }
}
