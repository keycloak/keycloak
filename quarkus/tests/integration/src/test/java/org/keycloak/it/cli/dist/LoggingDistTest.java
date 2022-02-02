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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

@DistributionTest(reInstall = DistributionTest.ReInstall.NEVER)
@RawDistOnly(reason = "Too verbose for docker and enough to check raw dist")
@TestMethodOrder(OrderAnnotation.class)
public class LoggingDistTest {

    @Test
    @Launch({ "start-dev", "--log-level=debug" })
    void testSetRootLevel(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertTrue(cliResult.getOutput().contains("DEBUG [org.hibernate"));
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev", "--log-level=org.keycloak:debug" })
    void testSetCategoryLevel(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertFalse(cliResult.getOutput().contains("DEBUG [org.hibernate"));
        assertTrue(cliResult.getOutput().contains("DEBUG [org.keycloak"));
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev", "--log-level=off,org.keycloak:debug,org.infinispan:info" })
    void testRootAndCategoryLevels(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertFalse(cliResult.getOutput().contains("INFO  [io.quarkus"));
        assertTrue(cliResult.getOutput().contains("DEBUG [org.keycloak"));
        assertTrue(cliResult.getOutput().contains("INFO  [org.infinispan.CONTAINER]"));
    }

    @Test
    @Launch({ "start-dev", "--log-level=off,org.keycloak:warn,debug" })
    void testSetLastRootLevelIfMultipleSet(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertTrue(cliResult.getOutput().contains("DEBUG [org.hibernate"));
        assertFalse(cliResult.getOutput().contains("INFO  [org.keycloak"));
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev", "--log-format=\"%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{1.}] %s%e%n\"" })
    void testSetLogFormat(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertFalse(cliResult.getOutput().contains("(keycloak-cache-init)"));
        cliResult.assertStartedDevMode();
    }
}