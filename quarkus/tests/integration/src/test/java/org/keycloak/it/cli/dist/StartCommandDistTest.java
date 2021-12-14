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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.keycloak.it.cli.StartCommandTest;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

@DistributionTest
public class StartCommandDistTest extends StartCommandTest {

    @Test
    @Launch({ "-pf=dev", "start", "--auto-build", "--http-enabled=true", "--hostname-strict=false" })
    void failIfAutoBuildUsingDevProfile(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("ERROR: You can not 'start' the server using the 'dev' configuration profile. Please re-build the server first, using 'kc.sh build' for the default production profile, or using 'kc.sh build --profile=<profile>' with a profile more suitable for production."),
                () -> "The Output:\n" + result.getErrorOutput() + "doesn't contains the expected string.");
        assertEquals(4, result.getErrorStream().size());
    }

    @Test
    @Launch({ "start", "--http-enabled=true" })
    void failNoHostnameNotSet(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("ERROR: Strict hostname resolution configured but no hostname was set"),
                () -> "The Output:\n" + result.getOutput() + "doesn't contains the expected string.");
    }

    @Test
    @Launch({ "start", "--auto-build", "--db-password=secret", "--https-key-store-password=secret"})
    void testStartWithAutoBuildDoesntShowCredentialsInConsole(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertTrue(cliResult.getOutput().contains("--db-password=" + PropertyMappers.VALUE_MASK));
        assertTrue(cliResult.getOutput().contains("--https-key-store-password=" + PropertyMappers.VALUE_MASK));
    }
}
