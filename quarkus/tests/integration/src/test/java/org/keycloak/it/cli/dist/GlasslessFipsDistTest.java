/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.StopServer.Mode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@DistributionTest(stopServer = Mode.MANUAL, defaultOptions = { "--db=dev-file", "--features=fips", "--http-enabled=true", "--hostname-strict=false" })
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SLOW)
public class GlasslessFipsDistTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_25)
    void testBundledGlasslessIsSelectedAutomatically(KeycloakRunner runner) {
        runner.setEnvVar("JAVA_OPTS_APPEND", "--enable-native-access=ALL-UNNAMED");
        runner.setEnvVar("KC_BOOTSTRAP_ADMIN_USERNAME", "admin");
        runner.setEnvVar("KC_BOOTSTRAP_ADMIN_PASSWORD", "glasslessAdminPassword25");

        CLIResult cliResult = runner.run("start", "--fips-mode=non-strict");
        cliResult.assertStarted();
        cliResult.assertMessage("Automatically selected FIPS provider: glassless");
        cliResult.assertMessage("GlasslessCryptoProvider created: KC(GlaSSLess version 0.13.0");
        cliResult.assertMessage("Glassless provider configuration:");
        cliResult.assertMessage("Cipher (");
        cliResult.assertMessage("AES/GCM/NoPadding");

        when().get("/realms/master/.well-known/openid-configuration").then().statusCode(200);
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("client_id", "admin-cli")
                .formParam("username", "admin")
                .formParam("password", "glasslessAdminPassword25")
                .formParam("grant_type", "password")
                .when().post("/realms/master/protocol/openid-connect/token")
                .then().statusCode(200)
                .body("token_type", equalTo("Bearer"))
                .body("access_token", not(emptyString()));
    }
}
