/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package test.org.keycloak.quarkus.services.health;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestProfile(MetricsEnabledProfile.class)
public class KeycloakReadyHealthCheckTest {

    @BeforeEach
    void setUp() {
        RestAssured.port = 9001;
    }

    @Test
    public void testLivenessUp() {
        given()
            .when().get("/health/live")
            .then()
                .statusCode(200)
                .body(Matchers.containsString("UP"));
    }

    @Test
    public void testReadinessUp() {
        given()
            .when().get("/health/ready")
            .then()
                .statusCode(200)
                .body(Matchers.containsString("UP"));
    }
}
