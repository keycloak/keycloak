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

import static io.restassured.RestAssured.when;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;

import io.quarkus.test.junit.main.Launch;

@DistributionTest(keepAlive = true,
        requestPort = 8080,
        containerExposedPorts = {8080, 9000})
@Tag(DistributionTest.SLOW)
@RawDistOnly(reason = "Raw ensures binding to localhost")
public class LocalHealthDistTest {

    @Test
    @Launch({ "start-dev" })
    void testHealthEndpointNotEnabled(KeycloakDistribution distribution) {
        when().get("/health").then().statusCode(404);
    }

    @Test
    @Launch({ "start-dev", "--health-enabled=true" })
    void testHealthEndpoint() {
        //when().get("/health").then()
        //        .statusCode(200);
        //when().get("/health/live").then()
        //        .statusCode(200);
        when().get("/health/ready").then()
                .statusCode(200);
        // Metrics should not be enabled
        when().get("/metrics").then()
                .statusCode(404);
        when().get("/lb-check").then()
                .statusCode(404);
    }

}
