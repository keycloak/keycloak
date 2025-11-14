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
import org.keycloak.it.junit5.extension.DistributionType;
import org.keycloak.it.utils.KeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DistributionTest(keepAlive = true,
        defaultOptions = {"--db=dev-file", "--health-enabled=true", "--metrics-enabled=true"},
        requestPort = 9000,
        containerExposedPorts = {9000, 8080, 9005})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(DistributionTest.SLOW)
public class ManagementDistTest {

    @Test
    @Order(1)
    @Launch({"start", "--hostname=hostname", "--http-enabled=false"})
    void testManagementNoHttps(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Management interface listening on");
        cliResult.assertError("Key material not provided to setup HTTPS.");
    }

    @Test
    @Order(2)
    @Launch({"start-dev", "--legacy-observability-interface=true"})
    void testManagementDisabled(LaunchResult result, KeycloakDistribution distribution) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Management interface listening on");

        assertThrows(IOException.class, () -> when().get("/"), "Connection refused must be thrown");
        assertThrows(IOException.class, () -> when().get("/health"), "Connection refused must be thrown");

        distribution.setRequestPort(8080);

        when().get("/health").then()
                .statusCode(200);
    }

    @Test
    @Order(3)
    @Launch({"start-dev"})
    void testManagementEnabled(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Management interface listening on http://0.0.0.0:9000");

        when().get("/").then()
                .statusCode(200)
                .and()
                .body(is("Keycloak Management Interface"));
        when().get("/health").then()
                .statusCode(200);
        when().get("/health/live").then()
                .statusCode(200);
        when().get("/health/ready").then()
                .statusCode(200);
        when().get("/metrics").then()
                .statusCode(200);
    }

    @Test
    @Launch({"start-dev", "--http-management-port=9005"})
    void testManagementDifferentPort(LaunchResult result, KeycloakDistribution distribution) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Management interface listening on http://0.0.0.0:9005");

        distribution.setRequestPort(9005);

        when().get("/").then()
                .statusCode(200)
                .and()
                .body(is("Keycloak Management Interface"));
        when().get("/health").then()
                .statusCode(200);
        when().get("/health/live").then()
                .statusCode(200);
        when().get("/health/ready").then()
                .statusCode(200);
        when().get("/metrics").then()
                .statusCode(200);
    }

    @Test
    @Launch({"start-dev", "--http-relative-path=/management2"})
    void testManagementInheritedRelativePath(LaunchResult result) {
        assertRelativePath(result, "/management2");
    }

    @Test
    @Launch({"start-dev", "--http-management-relative-path=/management"})
    void testManagementDifferentRelativePath(LaunchResult result) {
        assertRelativePath(result, "/management");
    }

    @Test
    @Launch({"start-dev", "--http-relative-path=/auth", "--http-management-relative-path=/management"})
    void testManagementRootRedirects(LaunchResult result, KeycloakDistribution distribution) {
        assertRelativePath(result, "/management");

        distribution.setRequestPort(8080);

        given().redirects().follow(false).when().get("/").then().statusCode(302).header("Location", is("/auth"));
        when().get("/").then().statusCode(200).body(containsString("Welcome to Keycloak"));
        when().get("/auth").then().statusCode(200).body(containsString("Welcome to Keycloak"));
    }

    private void assertRelativePath(LaunchResult result, String relativePath) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Management interface listening on http://0.0.0.0:9000");

        given().redirects().follow(false).when().get("/").then()
                .statusCode(302)
                .and()
                .header("Location", is(relativePath));
        when().get("/").then()
                .statusCode(200)
                .and()
                .body(is("Keycloak Management Interface"));
        when().get(relativePath).then()
                .statusCode(200)
                .and()
                .body(is("Keycloak Management Interface"));
        when().get(relativePath + "/health").then()
                .statusCode(200);
        when().get("/health").then()
                .statusCode(404);
        when().get(relativePath + "/health/live").then()
                .statusCode(200);
        when().get(relativePath + "/health/ready").then()
                .statusCode(200);
        when().get(relativePath + "/metrics").then()
                .statusCode(200);
        when().get("/metrics").then()
                .statusCode(404);
    }

    @Test
    @Launch({"start-dev", "--http-management-host=localhost"})
    void testManagementDifferentHost(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Management interface listening on http://localhost:9000");

        // If running in container, we cannot access the localhost due to network host settings
        if (DistributionType.isContainerDist()) {
            return;
        }

        when().get("/").then()
                .statusCode(200)
                .and()
                .body(is("Keycloak Management Interface"));
        when().get("/health").then()
                .statusCode(200);
        when().get("/health/live").then()
                .statusCode(200);
        when().get("/health/ready").then()
                .statusCode(200);
        when().get("/metrics").then()
                .statusCode(200);
    }
}
