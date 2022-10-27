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
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.it.cli.dist.util.CopyTLSKeystore;
import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

@DistributionTest(keepAlive = true, reInstall = DistributionTest.ReInstall.BEFORE_TEST)
@BeforeStartDistribution(CopyTLSKeystore.class)
@RawDistOnly(reason = "Containers are immutable")
public class TestKubeTest {

    static String url="http://mykeycloak.127.0.0.1.nip.io:8080/";
    static String tokenUrl="realms/master/protocol/openid-connect/token";

    @BeforeAll
    public static void onBeforeAll() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    @Launch({ "start-dev", "--hostname=mykeycloak.127.0.0.1.nip.io" })
    public void validateStatusCode() {
        Assert.assertEquals(200, when().get(url).getStatusCode());
    }

    @Test
    @Launch({ "start-dev", "--hostname=mykeycloak.127.0.0.1.nip.io" })
    public void  validateWelcomeText() {
        given().when().get(url)
                .then().assertThat()
                .body("html.head.title", Matchers.containsString("Welcome to Keycloak"));
    }

}