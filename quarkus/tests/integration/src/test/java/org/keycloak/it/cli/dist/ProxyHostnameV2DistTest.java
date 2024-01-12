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
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import org.apache.http.HttpHeaders;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.WithEnvVars;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

@DistributionTest(keepAlive = true, enableTls = true)
@WithEnvVars({"KEYCLOAK_ADMIN", "admin123", "KEYCLOAK_ADMIN_PASSWORD", "admin123"})
@RawDistOnly(reason = "Containers are immutable")
public class ProxyHostnameV2DistTest {

    @BeforeAll
    public static void onBeforeAll() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssuredConfig config = RestAssured.config;
        RestAssured.config = config.redirect(RedirectConfig.redirectConfig().followRedirects(false));
    }

    @Test
    @Launch({ "start-dev", "--hostname-strict=false" })
    public void testSchemeAndPortFromRequestWhenNoProxySet() {
        assertFrontEndUrl("http://mykeycloak.org:8080", "http://mykeycloak.org:8080/");
        assertFrontEndUrl("http://localhost:8080", "http://localhost:8080/");
        assertFrontEndUrl("https://localhost:8443", "https://localhost:8443/");
        assertForwardedHeaderIsIgnored();
        assertXForwardedHeadersAreIgnored();
    }

    @Test
    @Launch({ "start-dev", "--hostname-strict=false", "--proxy-headers=forwarded" })
    public void testForwardedProxyHeaders() {
        assertForwardedHeader();
        assertXForwardedHeadersAreIgnored();
    }

    @Test
    @Launch({ "start-dev", "--hostname-strict=false", "--proxy-headers=xforwarded" })
    public void testXForwardedProxyHeaders() {
        assertForwardedHeaderIsIgnored();
        assertXForwardedHeaders();
    }

    @Test
    @Launch({ "start-dev", "--hostname-strict=false", "--proxy-headers=xforwarded", "--proxy=reencrypt" })
    public void testProxyHeadersTakePrecedenceOverProxyReencryptOption() {
        assertForwardedHeaderIsIgnored();
        assertXForwardedHeaders();
    }

    @Test
    @Launch({ "start-dev", "--hostname-strict=false", "--proxy-headers=xforwarded", "--proxy=none" })
    public void testProxyHeadersTakePrecedenceOverProxyNoneOption() {
        assertForwardedHeaderIsIgnored();
        assertXForwardedHeaders();
    }

    @Test
    @Launch({ "start-dev", "--hostname=mykeycloak.org", "--proxy-headers=forwarded", "--proxy=none" })
    public void testExplicitlySetHostnameTakesPrecedenceOverProxyHeaders() {
        assertForwardedHeader("https://mykeycloak.org:1234/admin");
    }

    @Test
    @Launch({ "start-dev", "--hostname=http://mykeycloak.org:8080", "--proxy-headers=forwarded", "--proxy=none" })
    public void testExplicitlySetHostnameUrlTakesPrecedenceOverProxyHeaders() {
        assertForwardedHeader("http://mykeycloak.org:8080/admin");
    }

    private void assertForwardedHeader() {
        assertForwardedHeader("https://test:1234/admin");
    }

    private void assertForwardedHeader(String expectedUrl) {
        given()
                .header("Forwarded", "for=12.34.56.78;host=test:1234;proto=https, for=23.45.67.89")
                .when().get("http://mykeycloak.org:8080")
                .then().header(HttpHeaders.LOCATION, containsString(expectedUrl));
    }

    private void assertForwardedHeaderIsIgnored() {
        given().header("Forwarded", "for=12.34.56.78;host=test:1234;proto=https, for=23.45.67.89").when().get("http://localhost:8080").then().header(HttpHeaders.LOCATION, containsString("http://localhost:8080"));
    }

    private void assertXForwardedHeaders() {
        given().header("X-Forwarded-Host", "test").when().get("http://mykeycloak.org:8080").then().header(HttpHeaders.LOCATION, containsString("http://test:8080/admin"));
        given().header("X-Forwarded-Host", "test").when().get("http://localhost:8080").then().header(HttpHeaders.LOCATION, containsString("http://test:8080/admin"));
        given().header("X-Forwarded-Host", "test").when().get("https://localhost:8443").then().header(HttpHeaders.LOCATION, containsString("https://test:8443/admin"));
        given().header("X-Forwarded-Proto", "https").when().get("http://localhost:8080").then().header(HttpHeaders.LOCATION, containsString("https://localhost/admin"));
        given().header("X-Forwarded-Proto", "https").header("X-Forwarded-Port", "8443").when().get("http://localhost:8080").then().header(HttpHeaders.LOCATION, containsString("https://localhost:8443/admin"));
    }

    private void assertXForwardedHeadersAreIgnored() {
        given().header("X-Forwarded-Host", "test").when().get("http://mykeycloak.org:8080").then().header(HttpHeaders.LOCATION, containsString("http://mykeycloak.org:8080/admin"));
        given().header("X-Forwarded-Host", "test").when().get("http://localhost:8080").then().header(HttpHeaders.LOCATION, containsString("http://localhost:8080/admin"));
        given().header("X-Forwarded-Host", "test").when().get("https://localhost:8443").then().header(HttpHeaders.LOCATION, containsString("https://localhost:8443/admin"));
        given().header("X-Forwarded-Proto", "https").when().get("http://localhost:8080").then().header(HttpHeaders.LOCATION, containsString("http://localhost:8080/admin"));
        given().header("X-Forwarded-Proto", "https").header("X-Forwarded-Port", "8443").when().get("http://localhost:8080").then().header(HttpHeaders.LOCATION, containsString("http://localhost:8080/admin"));
    }

    private OIDCConfigurationRepresentation getServerMetadata(String baseUrl) {
        return when().get(baseUrl + "/realms/master/.well-known/openid-configuration").as(OIDCConfigurationRepresentation.class);
    }

    private void assertFrontEndUrl(String requestBaseUrl, String expectedBaseUrl) {
        Assert.assertEquals(expectedBaseUrl + "realms/master/protocol/openid-connect/auth", getServerMetadata(requestBaseUrl)
                .getAuthorizationEndpoint());
    }
}