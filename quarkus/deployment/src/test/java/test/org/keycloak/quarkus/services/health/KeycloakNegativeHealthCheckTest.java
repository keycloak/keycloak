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

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusUnitTest;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;

import static io.restassured.RestAssured.given;

public class KeycloakNegativeHealthCheckTest {

    @Inject
    AgroalDataSource agroalDataSource;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("keycloak.conf", "META-INF/keycloak.conf"));

    @Test
    public void testReadinessDown() {
        agroalDataSource.close();
        given()
                .when().get("/health/ready")
                .then()
                .statusCode(503)
                .body(Matchers.containsString("DOWN"));
    }
}
