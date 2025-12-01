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

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.TestProvider;
import org.keycloak.it.junit5.extension.WithEnvVars;
import org.keycloak.it.resource.realm.TestRealmResourceTestProvider;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.restassured.RestAssured;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import org.apache.http.HttpHeaders;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

@DistributionTest(keepAlive = true, enableTls = true)
@WithEnvVars({"KC_BOOTSTRAP_ADMIN_USERNAME", "admin123", "KC_BOOTSTRAP_ADMIN_PASSWORD", "admin123"})
@RawDistOnly(reason = "Containers are immutable")
public class ProxyHostnameV2DistTest {

    private static final String ADDRESS = "12.23.45.67";
    private static final String NOT_ADDRESS = "notaddress";

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
    void testTrustedProxiesWithoutProxyHeaders(KeycloakDistribution distribution) {
        CLIResult result = distribution.run("start-dev", "--proxy-trusted-addresses=1.0.0.0");
        result.assertError("proxy-trusted-addresses available only when proxy-headers is set");
    }

    @Test
    void testTrustedProxiesWithInvalidAddress(KeycloakDistribution distribution) {
        CLIResult result = distribution.run("start-dev", "--proxy-headers=xforwarded", "--proxy-trusted-addresses=1.0.0.0:8080");
        result.assertError("1.0.0.0:8080 is not a valid IP address (IPv4 or IPv6) nor valid CIDR notation.");
    }

    @Test
    @Launch({ "start-dev", "--hostname-strict=false", "--proxy-headers=xforwarded", "--proxy-trusted-addresses=1.0.0.0" })
    @TestProvider(TestRealmResourceTestProvider.class)
    public void testProxyNotTrusted() {
        assertForwardedHeaderIsIgnored();
        assertForwardedHeaderIsIgnored();
        given().header("X-Forwarded-Host", "test:123").when().get("http://mykeycloak.org:8080/realms/master/test-resources/trusted").then().statusCode(204);
    }

    @Test
    @Launch({ "start-dev", "--hostname-strict=false", "--proxy-headers=xforwarded", "--proxy-trusted-addresses=127.0.0.1,0:0:0:0:0:0:0:1" })
    @TestProvider(TestRealmResourceTestProvider.class)
    public void testProxyTrusted() {
        given().header("X-Forwarded-Host", "test:123").when().get("http://mykeycloak.org:8080/realms/master/test-resources/trusted").then().statusCode(200);
    }

    @Test
    @Launch({ "start-dev", "--hostname-strict=false", "--proxy-headers=forwarded", "--spi-event-listener-provider=jboss-logging" })
    public void testForwardedProxyHeaders(LaunchResult result) {
        assertForwardedHeader();
        assertXForwardedHeadersAreIgnored();

        CLIResult cliResult = (CLIResult)result;
        cliResult.assertMessage(NOT_ADDRESS); // non-ip addresses are still reported as the client ip
        cliResult.assertMessage(ADDRESS);
    }

    @Test
    @Launch({ "start-dev", "--hostname=https://mykeycloak.org:8443/path", "--proxy-headers=forwarded", "--hostname-backchannel-dynamic=true" })
    public void testForwardedProxyHeadersWithPathAndDynamicBackchannel(LaunchResult result) {
        assertFrontEndUrl("https://mykeycloak.org:8443", "https://mykeycloak.org:8443/path/");
        // a backend url generated via the frontend protocol/host/port should be a front-end url
        assertBackEndUrl("https://mykeycloak.org:8443", "https://mykeycloak.org:8443/path/");
        // any other protocol/host/port will be the backend
        assertBackEndUrl("http://localhost:8080", "http://localhost:8080/");
    }

    @Test
    @Launch({ "start-dev", "--hostname-strict=false", "--proxy-headers=xforwarded" })
    public void testXForwardedProxyHeaders() {
        assertForwardedHeaderIsIgnored();
        assertXForwardedHeaders();
    }

    private void assertForwardedHeader() {
        // trigger a login error
        assertForwardedHeader("http://mykeycloak.org:8080/realms/master/protocol/openid-connect/auth?client_id=security-admin-console", "https://test:1234/admin", ADDRESS);
        assertForwardedHeader("http://mykeycloak.org:8080/realms/master/protocol/openid-connect/auth?client_id=security-admin-console", "https://test:1234/admin", NOT_ADDRESS);
    }

    private void assertForwardedHeader(String url, String expectedUrl, String forAddress) {
        given()
                .header("Forwarded", "for="+forAddress+";host=test:1234;proto=https, for=23.45.67.89")
                .when().get(url)
                .then().header(HttpHeaders.LOCATION, containsString(expectedUrl));
    }

    private void assertForwardedHeaderIsIgnored() {
        given().header("Forwarded", "for=12.34.56.78;host=test:1234;proto=https, for=23.45.67.89").when().get("http://localhost:8080").then().header(HttpHeaders.LOCATION, containsString("http://localhost:8080"));
    }

    private void assertXForwardedHeaders() {
        given().header("X-Forwarded-Host", "test:123").when().get("http://mykeycloak.org:8080").then().header(HttpHeaders.LOCATION, containsString("http://test:123/admin"));
        given().header("X-Forwarded-Host", "test:123").when().get("http://localhost:8080").then().header(HttpHeaders.LOCATION, containsString("http://test:123/admin"));
        given().header("X-Forwarded-Host", "test:123").when().get("https://localhost:8443").then().header(HttpHeaders.LOCATION, containsString("https://test:123/admin"));
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

    private void assertBackEndUrl(String requestBaseUrl, String expectedBaseUrl) {
        Assert.assertEquals(expectedBaseUrl + "realms/master/protocol/openid-connect/token", getServerMetadata(requestBaseUrl)
                .getTokenEndpoint());
    }

}
