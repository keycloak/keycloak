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
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.DistributionTest;

import static io.restassured.RestAssured.when;

@DistributionTest(keepAlive =true)
public class HealthDistTest {

    @Test
    @Launch({ "start-dev" })
    void testHealthEndpointNotEnabled() {
        when().get("/health").then()
                .statusCode(404);
        when().get("/q/health").then()
                .statusCode(404);
        when().get("/health/live").then()
                .statusCode(404);
        when().get("/q/health/live").then()
                .statusCode(404);
        when().get("/health/ready").then()
                .statusCode(404);
        when().get("/q/health/ready").then()
                .statusCode(404);
    }

    @Test
    @Launch({ "start-dev", "--health-enabled=true" })
    void testHealthEndpoint() {
        when().get("/health").then()
                .statusCode(200);
        when().get("/health/live").then()
                .statusCode(200);
        when().get("/health/ready").then()
                .statusCode(200);
        // Metrics should not be enabled
        when().get("/metrics").then()
                .statusCode(404);
    }
}
