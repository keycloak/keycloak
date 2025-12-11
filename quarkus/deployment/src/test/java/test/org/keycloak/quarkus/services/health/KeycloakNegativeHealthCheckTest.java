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

import org.keycloak.quarkus.runtime.services.health.KeycloakReadyHealthCheck;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import io.quarkus.agroal.runtime.health.DataSourceHealthCheck;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestProfile(MetricsEnabledProfile.class)
public class KeycloakNegativeHealthCheckTest {

    @InjectMock
    AgroalDataSource agroalDataSource;
    @InjectMock
    DataSourceHealthCheck dataSourceHealthCheck;

    @Test
    public void testReadinessDown() {
        AgroalDataSourceMetrics metrics = Mockito.mock(AgroalDataSourceMetrics.class);
        Mockito.when(agroalDataSource.getMetrics()).thenReturn(metrics);
        Mockito.when(dataSourceHealthCheck.call()).thenReturn(HealthCheckResponse.down("down"));

        RestAssured.port = 9001;
        given()
                .when().get("/health/ready")
                .then()
                .statusCode(503)
                .body(Matchers.allOf(Matchers.containsString("DOWN"), Matchers.containsString(KeycloakReadyHealthCheck.FAILING_SINCE)));

        // now have an active connection, failing since should be cleared
        Mockito.when(metrics.activeCount()).thenReturn(2L);
        given()
                .when().get("/health/ready")
                .then()
                .statusCode(200)
                .body(Matchers.not(Matchers.containsString(KeycloakReadyHealthCheck.FAILING_SINCE)));
    }
}
