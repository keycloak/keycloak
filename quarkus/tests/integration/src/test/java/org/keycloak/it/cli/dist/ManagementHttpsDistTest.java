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

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.restassured.RestAssured;
import io.restassured.config.RedirectConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

@DistributionTest(keepAlive = true,
        enableTls = true,
        defaultOptions = {"--db=dev-file", "--health-enabled=true", "--metrics-enabled=true"},
        requestPort = 9000)
@RawDistOnly(reason = "We do not test TLS in containers")
public class ManagementHttpsDistTest {

    @BeforeEach
    public void setRestAssuredHttps() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config.redirect(RedirectConfig.redirectConfig().followRedirects(false));
    }

    @Test
    @Launch({"start-dev"})
    public void simpleHttpsStartDev(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        var url = "https://localhost:9000";
        cliResult.assertMessage("Management interface listening on https://0.0.0.0:9000");

        when().get(url).then()
                .statusCode(200)
                .and()
                .body(is("Keycloak Management Interface"));
        when().get(url + "/health").then()
                .statusCode(200);
        when().get(url + "/health/live").then()
                .statusCode(200);
        when().get(url + "/health/ready").then()
                .statusCode(200);
        when().get(url + "/metrics").then()
                .statusCode(200);
    }

    @Test
    @Launch({"start-dev", "--http-management-scheme=http"})
    public void simpleHttpStartDev(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        var url = "http://localhost:9000";
        cliResult.assertMessage("Management interface listening on http://0.0.0.0:9000");

        when().get(url).then()
                .statusCode(200)
                .and()
                .body(is("Keycloak Management Interface"));
        when().get(url + "/health").then()
                .statusCode(200);
        when().get(url + "/health/live").then()
                .statusCode(200);
        when().get(url + "/health/ready").then()
                .statusCode(200);
        when().get(url + "/metrics").then()
                .statusCode(200);
    }
}
