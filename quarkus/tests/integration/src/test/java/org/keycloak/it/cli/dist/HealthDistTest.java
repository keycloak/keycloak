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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.utils.KeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DistributionTest(keepAlive = true,
        requestPort = 9000,
        containerExposedPorts = {8080, 9000})
@Tag(DistributionTest.SLOW)
public class HealthDistTest {

    @Test
    @Launch({ "start-dev" })
    void testHealthEndpointNotEnabled(KeycloakDistribution distribution) {
        assertThrows(IOException.class, () -> when().get("/health"), "Connection refused must be thrown");
        distribution.setRequestPort(8080);
        when().get("/health").then()
                .statusCode(404);
    }

    @Test
    @Launch({ "start-dev", "--health-enabled=true" })
    void testHealthEndpoint(KeycloakDistribution distribution) {
        when().get("/health").then()
                .statusCode(200);
        when().get("/health/live").then()
                .statusCode(200);
        when().get("/health/ready").then()
                .statusCode(200);
        // Metrics should not be enabled
        when().get("/metrics").then()
                .statusCode(404);
        when().get("/lb-check").then()
                .statusCode(404);

        // still nothing on main
        distribution.setRequestPort(8080);
        when().get("/health/ready").then()
                .statusCode(404);
    }

    @Test
    @Launch({ "start-dev", "--health-enabled=true", "--http-management-health-enabled=false" })
    void testHealthEndpointOnMain(KeycloakDistribution distribution) {
        distribution.setRequestPort(8080);
        when().get("/health/ready").then().statusCode(200);
    }

    @Test
    @Launch({ "start-dev", "--health-enabled=true", "--metrics-enabled=true", "--cache=ispn" })
    void testNonBlockingProbes() {
        when().get("/health/live").then()
                .statusCode(200);
        when().get("/health/ready").then()
                .statusCode(200)
                .body("checks.size()", equalTo(2));
        when().get("/lb-check").then()
                .statusCode(404);
    }

    @Test
    void testUsingRelativePath(KeycloakDistribution distribution) {
        for (String relativePath : List.of("/auth", "/auth/", "auth")) {
            distribution.run("start-dev", "--health-enabled=true", "--http-management-relative-path=" + relativePath);
            if (!relativePath.endsWith("/")) {
                relativePath = relativePath + "/";
            }
            when().get(relativePath + "health").then().statusCode(200);
            distribution.stop();
        }
    }

    @Test
    void testMultipleRequests(KeycloakDistribution distribution) throws Exception {
        for (String relativePath : List.of("/", "/auth/", "auth")) {
            distribution.run("start-dev", "--health-enabled=true", "--http-management-relative-path=" + relativePath);
            CompletableFuture<?> future = CompletableFuture.completedFuture(null);

            for (int i = 0; i < 3; i++) {
                future = CompletableFuture.allOf(CompletableFuture.runAsync(() -> {
                    for (int i1 = 0; i1 < 200; i1++) {
                        String healthPath = "health";

                        if (!relativePath.endsWith("/")) {
                            healthPath = "/" + healthPath;
                        }

                        when().get(relativePath + healthPath).then().statusCode(200);
                    }
                }), future);
            }

            future.get(5, TimeUnit.MINUTES);

            distribution.stop();
        }
    }
}
