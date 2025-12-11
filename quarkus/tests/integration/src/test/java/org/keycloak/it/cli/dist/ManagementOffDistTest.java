/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DistributionTest(keepAlive = true,
        requestPort = 9000,
        containerExposedPorts = {9000, 8080})
public class ManagementOffDistTest {

    @Test
    @Launch({"start-dev"})
    public void notOccupied(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Management interface listening on");

        assertThrows(IOException.class, () -> when().get("/"), "Connection refused must be thrown");
        assertThrows(IOException.class, () -> when().get("/health"), "Connection refused must be thrown");
    }
}
