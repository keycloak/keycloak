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

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;

import io.quarkus.test.junit.main.Launch;
import io.restassured.RestAssured;

@DistributionTest(keepAlive = true, enableTls = true)
@RawDistOnly(reason = "Containers are immutable")
public class ProxyDistTest {

    @BeforeAll
    public static void onBeforeAll() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    @Launch({ "start-dev", "--hostname=mykeycloak.org" })
    public void testSchemeAndPortFromRequestWhenNoProxySet() {
        assertFrontEndUrl("http://mykeycloak.org:8080", "http://mykeycloak.org:8080/");
        assertFrontEndUrl("http://localhost:8080", "http://mykeycloak.org:8080/");
        assertFrontEndUrl("https://localhost:8443", "https://mykeycloak.org:8443/");
        given().header("X-Forwarded-Host", "test").when().get("http://localhost:8080").then().body(containsString("http://localhost:8080/admin"));
    }

    @Test
    @Launch({ "start-dev", "--hostname=mykeycloak.org", "--proxy=edge" })
    public void testXForwardedHeadersWithEdge() {
        assertXForwardedHeaders();
    }

    @Test
    @Launch({ "start-dev", "--hostname=mykeycloak.org", "--proxy=reencrypt" })
    public void testXForwardedHeadersWithReencrypt() {
        assertXForwardedHeaders();
    }

    @Test
    @Launch({ "start-dev", "--hostname-url=http://mykeycloak.org:1234", "--hostname-admin-url=http://mykeycloakadmin.127.0.0.1.nip.io:1234", "--proxy=edge" })
    public void testIgnoreForwardedHeadersWhenFrontendUrlSet() {
        given().header("X-Forwarded-Host", "test").when().get("http://mykeycloak.org:8080").then().body(containsString("http://mykeycloakadmin.127.0.0.1.nip.io:1234/admin"));
        given().header("X-Forwarded-Proto", "https").when().get("http://localhost:8080").then().body(containsString("http://mykeycloakadmin.127.0.0.1.nip.io:1234/admin"));
    }

    private void assertXForwardedHeaders() {
        given().header("X-Forwarded-Host", "test").when().get("http://mykeycloak.org:8080").then().body(containsString("http://test:8080/admin"));
        given().header("X-Forwarded-Host", "test").when().get("http://localhost:8080").then().body(containsString("http://test:8080/admin"));
        given().header("X-Forwarded-Host", "test").when().get("https://localhost:8443").then().body(containsString("https://test:8443/admin"));
        //given().header("X-Forwarded-Host", "mykeycloak.org").when().get("https://localhost:8443/admin/master/console").then().body(containsString("<script src=\"/js/keycloak.js?version="));
        given().header("X-Forwarded-Proto", "https").when().get("http://localhost:8080").then().body(containsString("https://localhost/admin"));
        given().header("X-Forwarded-Proto", "https").header("X-Forwarded-Port", "8443").when().get("http://localhost:8080").then().body(containsString("https://localhost:8443/admin"));
    }

    private OIDCConfigurationRepresentation getServerMetadata(String baseUrl) {
        return when().get(baseUrl + "/realms/master/.well-known/openid-configuration").as(OIDCConfigurationRepresentation.class);
    }

    private void assertFrontEndUrl(String requestBaseUrl, String expectedBaseUrl) {
        Assert.assertEquals(expectedBaseUrl + "realms/master/protocol/openid-connect/auth", getServerMetadata(requestBaseUrl)
                .getAuthorizationEndpoint());
    }
}