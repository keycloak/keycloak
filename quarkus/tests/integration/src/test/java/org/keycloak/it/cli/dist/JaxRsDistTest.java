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

import java.util.concurrent.TimeUnit;

import org.keycloak.it.jaxrs.filter.TestFilterTestProvider;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.TestProvider;
import org.keycloak.it.utils.KeycloakDistribution;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DistributionTest(keepAlive = true)
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SMOKE)
public class JaxRsDistTest {

    @Test
    @TestProvider(TestFilterTestProvider.class)
    public void requestFilterTest(KeycloakDistribution dist) {
        CLIResult cliResult = dist.run("start-dev");

        cliResult.assertStartedDevMode();

        assertEquals(200, when().get("/").getStatusCode());

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(
                () -> cliResult.assertMessage("Request GET / has context request true has keycloaksession true"));
    }
}
