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

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.DryRun;
import org.keycloak.it.utils.KeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DistributionTest(keepAlive = true, requestPort = 9000, containerExposedPorts = {8080, 9000})
@Tag(DistributionTest.SLOW)
public class OpenApiDistTest {

  private static final String OPENAPI_ENDPOINT = "/openapi";
  private static final String OPENAPI_UI_ENDPOINT = "/openapi/ui";

  // Cannot use defaultOptions as we want to test it being absent too
  private static final String FEATURES_OPTION = "--features=openapi,client-admin-api:v2";

  @Test
  @Launch({"start-dev", FEATURES_OPTION})
  void testOpenApiEndpointNotEnabled(KeycloakDistribution distribution) {
    assertThrows(IOException.class, () -> when().get(OPENAPI_ENDPOINT), "Connection refused must be thrown");
    assertThrows(IOException.class, () -> when().get(OPENAPI_UI_ENDPOINT), "Connection refused must be thrown");

    distribution.setRequestPort(8080);

    when().get(OPENAPI_ENDPOINT).then()
        .statusCode(404);
    when().get(OPENAPI_UI_ENDPOINT).then()
        .statusCode(404);
  }

  @Test
  @Launch({"start-dev", "--openapi-enabled=true", FEATURES_OPTION})
  void testOpenApiEndpointEnabled(KeycloakDistribution distribution) {
    when().get(OPENAPI_ENDPOINT)
        .then()
        .statusCode(200);
  }

  @Test
  @Launch({"start-dev", "--openapi-ui-enabled=true", "--openapi-enabled=true", FEATURES_OPTION})
  void testOpenApiUiEndpointEnabled(KeycloakDistribution distribution) {
    when().get(OPENAPI_UI_ENDPOINT)
        .then()
        .statusCode(200);
  }

  @DryRun
  @Test
  @Launch({ "start-dev", "--openapi-ui-enabled=true", FEATURES_OPTION})
  void testOpenApiUiFailsWhenOpenApiIsNotEnabled(CLIResult cliResult) {
    cliResult.assertError("Disabled option: '--openapi-ui-enabled'. Available only when OpenAPI Endpoint is enabled");
  }

  @DryRun
  @Test
  void testOpenApiRequiresFeatures(KeycloakDistribution dist) {
    CLIResult cliResult = dist.run("start-dev", "--openapi-enabled=true", "--features=openapi");
    cliResult.assertError("ERROR: Feature openapi depends on disabled feature client-admin-api-v2");

    cliResult = dist.run("start-dev", "--openapi-enabled=true", "--features=client-admin-api:v2");
    cliResult.assertError("Disabled option: '--openapi-enabled'. Available only when OpenAPI feature is enabled");
  }
}
