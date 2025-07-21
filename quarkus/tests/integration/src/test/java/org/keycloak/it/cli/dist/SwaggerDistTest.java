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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.utils.KeycloakDistribution;

import java.io.IOException;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DistributionTest(keepAlive = true,
    requestPort = 9000,
    containerExposedPorts = {8080, 9000})
@Tag(DistributionTest.SLOW)
public class SwaggerDistTest {

  private static final String SWAGGER_UI_ENDPOINT = "/swagger-ui";

  @Test
  @Launch({"start-dev"})
  void testSwaggerEndpointNotEnabled(KeycloakDistribution distribution) {
    assertThrows(IOException.class, () -> when().get(SWAGGER_UI_ENDPOINT), "Connection refused must be thrown");

    distribution.setRequestPort(8080);

    when().get(SWAGGER_UI_ENDPOINT).then()
        .statusCode(404);
  }

  @Test
  @Launch({"start-dev", "--swagger-enabled=true", "--openapi-enabled=true"})
  void testSwaggerEndpointEnabled(KeycloakDistribution distribution) {
    when().get(SWAGGER_UI_ENDPOINT)
        .then()
        .statusCode(200);
  }
}
