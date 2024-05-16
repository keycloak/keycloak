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
import org.keycloak.it.utils.KeycloakDistribution;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@DistributionTest(keepAlive = true,
        requestPort = 9000,
        containerExposedPorts = {8080, 9000})
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
        when().get("/lb-check").then()
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
        when().get("/lb-check").then()
                .statusCode(404);
    }

    @Test
    @Launch({ "start-dev", "--health-enabled=true", "--metrics-enabled=true" })
    void testNonBlockingProbes() {
        when().get("/health/live").then()
                .statusCode(200);
        when().get("/health/ready").then()
                .statusCode(200)
                .body("checks[0].name", equalTo("Keycloak database connections async health check"))
                .body("checks.size()", equalTo(1));
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
            CompletableFuture future = CompletableFuture.completedFuture(null);

            for (int i = 0; i < 3; i++) {
                future = CompletableFuture.allOf(CompletableFuture.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 200; i++) {
                            String healthPath = "health";

                            if (!relativePath.endsWith("/")) {
                                healthPath = "/" + healthPath;
                            }

                            when().get(relativePath + healthPath).then().statusCode(200);
                        }
                    }
                }), future);
            }

            future.get(5, TimeUnit.MINUTES);

            distribution.stop();
        }
    }

    @Test
    @Launch({ "start-dev", "--features=multi-site" })
    void testLoadBalancerCheck(KeycloakDistribution distribution) {
        distribution.setRequestPort(8080);

        when().get("/lb-check").then()
                .statusCode(200);
    }
}
