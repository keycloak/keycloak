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

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.WithEnvVars;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
public class BootstrapAdminDistTest {

    @Test
    @Launch({ "bootstrap-admin", "user", "--db=dev-file", "--no-prompt" })
    void failNoPassword(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("No password provided"),
                () -> "The Output:\n" + result.getErrorOutput() + "doesn't contains the expected string.");
    }

    /**
    @Test
    @Launch({ "bootstrap-admin", "user", "--db=dev-file", "--expiration=tomorrow" })
    void failBadExpiration(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("Invalid value for option '--expiration': 'tomorrow' is not an int"),
                () -> "The Output:\n" + result.getErrorOutput() + "doesn't contains the expected string.");
    }*/

    @Test
    @Launch({ "bootstrap-admin", "user", "--db=dev-file", "--username=admin", "--password:env=MY_PASSWORD" })
    void failEnvNotSet(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("Environment variable MY_PASSWORD not found"),
                () -> "The Output:\n" + result.getErrorOutput() + "doesn't contains the expected string.");
    }

    @Test
    @WithEnvVars({"MY_PASSWORD", "admin123"})
    @Launch({ "bootstrap-admin", "user", "--db=dev-file", "--username=admin", "--password:env=MY_PASSWORD" })
    void createAdmin(LaunchResult result) {
        assertTrue(result.getErrorOutput().isEmpty(), result.getErrorOutput());
    }

    @Test
    @Launch({ "start-dev", "--bootstrap-admin-password=MY_PASSWORD" })
    void createAdminWithCliOptions(LaunchResult result) {
        assertTrue(result.getErrorOutput().isEmpty(), result.getErrorOutput());
        result.getOutput().contains("Created temporary admin user with username temp-admin");
    }

    @Test
    @Launch({ "bootstrap-admin", "service", "--db=dev-file", "--no-prompt" })
    void failServiceAccountNoSecret(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("No client secret provided"),
                () -> "The Output:\n" + result.getErrorOutput() + "doesn't contains the expected string.");
    }

    @Test
    @Launch({ "bootstrap-admin", "service", "--db=dev-file", "--client-id=admin", "--client-secret:env=MY_SECRET" })
    void failServiceAccountEnvNotSet(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("Environment variable MY_SECRET not found"),
                () -> "The Output:\n" + result.getErrorOutput() + "doesn't contains the expected string.");
    }

    @Test
    @WithEnvVars({"MY_SECRET", "admin123"})
    void createAndUseSericeAccountAdmin(KeycloakDistribution dist) throws Exception {
        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        CLIResult result = rawDist.run("bootstrap-admin", "service", "--db=dev-file", "--client-id=admin", "--client-secret:env=MY_SECRET");

        assertTrue(result.getErrorOutput().isEmpty(), result.getErrorOutput());

        rawDist.setManualStop(true);
        rawDist.run("start-dev");

        CLIResult adminResult = rawDist.kcadm("get", "clients", "--server", "http://localhost:8080", "--realm", "master", "--client", "admin", "--secret", "admin123");

        assertEquals(0, adminResult.exitCode());
        assertTrue(adminResult.getOutput().contains("clientId"));
    }

}
