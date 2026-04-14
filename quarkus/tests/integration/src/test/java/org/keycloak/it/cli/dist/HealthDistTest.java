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
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                .body("checks.size()", equalTo(4));
        when().get("/lb-check").then()
                .statusCode(404);
    }

    private static final String LISTENING_ON_HTTP = "Listening on: http://";
    private static final String BOOTSTRAP_COMPLETED = "Bootstrap completed";

    @Test
    @Launch({ "start", "--health-enabled=true", "--http-enabled=true", "--hostname-strict=false" })
    void testAsyncStartupEnabled(LaunchResult result) {
        when().get("/health/live").then()
                .statusCode(200);
        when().get("/health/ready").then()
                .statusCode(200)
                .body("checks.size()", equalTo(3))
                .body(containsString("\"Keycloak Initialized\""));
        assertTrue(result.getOutput().indexOf(LISTENING_ON_HTTP) < result.getOutput().indexOf(BOOTSTRAP_COMPLETED),
                () -> "Should first listen, then bootstrap");
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--server-async-bootstrap=true" })
    void testAsyncStartupEnabledAsNoHealthIsPresentButUserAsksForIt(LaunchResult result) {
        assertTrue(result.getOutput().indexOf(LISTENING_ON_HTTP) < result.getOutput().indexOf(BOOTSTRAP_COMPLETED),
                () -> "Should listen, then bootstrap");
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false" })
    void testAsyncStartupDisabledAsNoHealthIsPresent(LaunchResult result) {
        assertTrue(result.getOutput().indexOf(LISTENING_ON_HTTP) > result.getOutput().indexOf(BOOTSTRAP_COMPLETED),
                () -> "Should bootstrap, then listen");
    }

    @Test
    @Launch({ "start", "--health-enabled=true", "--http-enabled=true", "--hostname-strict=false", "--server-async-bootstrap=false" })
    void testAsyncStartupDisabledViaCLI(LaunchResult result) {
        when().get("/health/ready").then()
                .statusCode(200);
        assertTrue(result.getOutput().indexOf(LISTENING_ON_HTTP) > result.getOutput().indexOf(BOOTSTRAP_COMPLETED),
                () -> "Should first bootstrap, then listen");
    }

    @Test
    @Launch({ "start", "--health-enabled=true", "--http-enabled=true", "--hostname-strict=false", "--server-async-bootstrap=false" })
    void testAsyncStartupDisabledAsNoHealthEndpoint(LaunchResult result) {
        when().get("/health/ready").then()
                .statusCode(200);
        assertTrue(result.getOutput().indexOf(LISTENING_ON_HTTP) > result.getOutput().indexOf(BOOTSTRAP_COMPLETED),
                () -> "Should first bootstrap, then listen");
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
