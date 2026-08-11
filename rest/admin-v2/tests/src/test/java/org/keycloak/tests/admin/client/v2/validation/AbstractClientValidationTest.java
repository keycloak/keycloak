/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.client.v2.validation;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.common.Profile;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.services.error.ViolationExceptionResponse;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.admin.client.v2.AbstractClientApiV2Test;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for Client API v2 validation.
 * <p>
 * Covers:
 * - Required field validation (clientId)
 * - URL validation (appUrl, redirectUris)
 * - Collection element validation (roles, webOrigins, serviceAccountRoles)
 * - Unknown fields rejection
 * - Multiple validation errors
 * - SAML client validation
 */
public abstract class AbstractClientValidationTest extends AbstractClientApiV2Test {

    @InjectRealm
    ManagedRealm realm;

    @InjectClient(config = PutClientValidationTest.TestOidcClient.class, ref = "test-oidc")
    ManagedClient testOidcClient;

    @InjectClient(config = PutClientValidationTest.TestSamlClient.class, ref = "test-saml")
    ManagedClient testSamlClient;

    @InjectHttpClient
    CloseableHttpClient client;

    @Override
    public String getRealmName() {
        return realm.getName();
    }

    public abstract String getHttpMethod();

    public abstract HttpEntityEnclosingRequestBase getRequest(boolean isOidc);

    public HttpEntityEnclosingRequestBase getRequest() {
        return getRequest(true);
    }

    public abstract String getPayloadClientId(boolean isOidc);

    public String getPayloadClientId() {
        return getPayloadClientId(true);
    }

    // ===========================================
    // clientId validation tests
    // ===========================================

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void clientWithoutClientIdFails(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "displayName": "Test Client",
                    "enabled": true
                }
                """.formatted(protocol)));

        try (var response = client.execute(request)) {
            switch (getHttpMethod()) {
                // the clientId is checked on the patched object, so the clientId is present
                case HttpPatch.METHOD_NAME -> assertThat(response.getStatusLine().getStatusCode(), is(200));
                default -> {
                    assertThat(response.getStatusLine().getStatusCode(), is(400));
                    var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
                    assertThat(body.error(), is("Provided data is invalid"));
                    assertThat(body.violations(), hasItem("clientId: must not be blank"));
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void clientWithBlankClientIdFails(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "   ",
                    "enabled": true
                }
                """.formatted(protocol)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            switch (getHttpMethod()) {
                case HttpPut.METHOD_NAME, HttpPatch.METHOD_NAME -> {
                    var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
                    assertThat(body.error(), is("Field 'clientId' in payload does not match the provided 'clientId'"));
                }
                default -> {
                    var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
                    assertThat(body.error(), is("Provided data is invalid"));
                    assertThat(body.violations(), hasItem("clientId: must not be blank"));
                }
            }
        }
    }

    // Tests that {@code @NotBlank} validation on {@code clientId} fires as first the path and payload clientIds are checked and throw error.
    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void clientWithBlankClientIdMatchingPathFails(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = switch (getHttpMethod()) {
            case HttpPost.METHOD_NAME -> getRequest(isOidc);
            case HttpPut.METHOD_NAME -> {
                var r = new HttpPut(getClientApiUrl("%20"));
                r.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                yield r;
            }
            default -> throw new IllegalStateException("Unexpected value: " + getHttpMethod());
        };
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": " ",
                    "enabled": true
                }
                """.formatted(protocol)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("clientId: must not be blank"));
        }
    }

    // ===========================================
    // Protocol validation tests
    // ===========================================

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void clientWithMissingProtocolFails(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "clientId": "%s",
                    "enabled": true
                }
                """.formatted(getPayloadClientId(isOidc))));

        try (var response = client.execute(request)) {
            var responseBody = EntityUtils.toString(response.getEntity());
            switch (getHttpMethod()) {
                case HttpPatch.METHOD_NAME -> assertThat(response.getStatusLine().getStatusCode(), is(200));
                case HttpPost.METHOD_NAME, HttpPut.METHOD_NAME -> {
                    assertThat(response.getStatusLine().getStatusCode(), is(400));
                    assertThat(responseBody, containsString("protocol is required"));
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid-protocol", "unknown"})
    public void clientWithInvalidProtocolFails(String protocol) throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "enabled": true
                }
                """.formatted(protocol, getPayloadClientId())));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var responseBody = EntityUtils.toString(response.getEntity());
            assertThat(responseBody, notNullValue());
            if (protocol.isBlank()) {
                assertThat(responseBody, containsString("protocol is required"));
            } else {
                assertThat(responseBody, anyOf(
                        containsString("unsupported client protocol"),
                        containsString("protocol cannot be changed for an existing client")));
            }
        }
    }

    // ===========================================
    // URL validation tests
    // ===========================================

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void clientWithInvalidAppUrlFails(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "appUrl": "not-a-valid-url",
                    "enabled": true
                }
                """.formatted(protocol, getPayloadClientId(isOidc))));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("appUrl: must be a valid URL"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void clientWithInvalidRedirectUriFails(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        // Without appUrl set, relative paths like "not-a-url" are invalid
        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "redirectUris": ["http://valid.com", "not-a-url", "also-invalid"],
                    "enabled": true
                }
                """.formatted(protocol, getPayloadClientId(isOidc))));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            // Should have violations for the invalid URLs (relative paths without root URL)
            assertThat(body.violations().size(), greaterThanOrEqualTo(1));
            assertThat(body.violations(), hasItem(containsString("redirectUris")));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void clientWithBlankRedirectUriFails(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "redirectUris": ["http://valid.com", "", "   "],
                    "enabled": true
                }
                """.formatted(protocol, getPayloadClientId(isOidc))));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations().size(), greaterThanOrEqualTo(1));
            // Blank strings are caught by @NotBlank on Set elements
            assertThat(body.violations(), hasItem("redirectUris[].<iterable element>: must not be blank"));
        }
    }

    // ===========================================
    // Collection element validation tests
    // ===========================================

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void clientWithBlankRoleFails(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "roles": ["valid-role", "", "   "],
                    "enabled": true
                }
                """.formatted(protocol, getPayloadClientId(isOidc))));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations().size(), greaterThanOrEqualTo(1));
            assertThat(body.violations(), hasItem("roles[].<iterable element>: must not be blank"));
        }
    }

    @Test
    public void clientWithBlankWebOriginFails() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "webOrigins": ["http://valid.com", "", "   "],
                    "enabled": true
                }
                """.formatted(getPayloadClientId())));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations().size(), greaterThanOrEqualTo(1));
            assertThat(body.violations(), hasItem("webOrigins[].<iterable element>: must not be blank"));
        }
    }

    @Test
    public void clientWithBlankServiceAccountRoleFails() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "loginFlows": ["SERVICE_ACCOUNT"],
                    "serviceAccountRoles": ["valid-role", "", "   "],
                    "enabled": true
                }
                """.formatted(getPayloadClientId())));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations().size(), greaterThanOrEqualTo(1));
            assertThat(body.violations(), hasItem("serviceAccountRoles[].<iterable element>: must not be blank"));
        }
    }

    // ===========================================
    // Unknown fields validation tests
    // ===========================================

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void clientWithUnknownFieldFails(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "enabled": true,
                    "unknownField": "some value",
                    "anotherUnknown": 123
                }
                """.formatted(protocol, getPayloadClientId(isOidc))));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            String responseBody = EntityUtils.toString(response.getEntity());
            assertThat(responseBody, notNullValue());
            // Should mention the unknown fields
            assertThat(responseBody.contains("unknown") || responseBody.contains("Unknown"), is(true));
        }
    }

    // ===========================================
    // Multiple validation errors
    // ===========================================

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void clientWithMultipleValidationErrorsReturnsAll(String protocol) throws Exception {
        var isOidc = OIDCClientRepresentation.PROTOCOL.equals(protocol);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        // Use wildcard in middle of path which is always invalid, plus invalid appUrl
        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "appUrl": "invalid-url",
                    "redirectUris": ["https://example.com/*/invalid"]
                }
                """.formatted(protocol, getPayloadClientId(isOidc))));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            // Should have violations for both appUrl and redirectUris
            assertThat(body.violations(), hasItem("appUrl: must be a valid URL"));
            assertThat(body.violations(), hasItem(containsString("redirectUris")));
        }
    }

    // ===========================================
    // Size constraint tests (BaseClientRepresentation)
    // ===========================================

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void testDisplayNameMaxLength(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        String longName = "n".repeat(256);
        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "displayName": "%s",
                    "enabled": true
                }
                """.formatted(protocol, getPayloadClientId(isOidc), longName)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("displayName: size must be between 0 and 255"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void testDescriptionMaxLength(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        String longDesc = "d".repeat(256);
        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "description": "%s",
                    "enabled": true
                }
                """.formatted(protocol, getPayloadClientId(isOidc), longDesc)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("description: size must be between 0 and 255"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void testAppUrlMaxLength(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        String longUrl = "http://example.com/" + "a".repeat(237);
        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "appUrl": "%s",
                    "enabled": true
                }
                """.formatted(protocol, getPayloadClientId(isOidc), longUrl)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("appUrl: size must be between 0 and 255"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void testRedirectUriElementMaxLength(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        String longUri = "http://example.com/" + "a".repeat(237);
        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "redirectUris": ["%s"],
                    "enabled": true
                }
                """.formatted(protocol, getPayloadClientId(isOidc), longUri)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("redirectUris[].<iterable element>: size must be between 0 and 255"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void testRedirectUrisMaxSize(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        String uris = IntStream.range(0, 101)
                .mapToObj(i -> "\"http://example.com/redirect" + i + "\"")
                .collect(Collectors.joining(", "));
        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "redirectUris": [%s],
                    "enabled": true
                }
                """.formatted(protocol, getPayloadClientId(isOidc), uris)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("redirectUris: size must be between 0 and 100"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void testRolesElementMaxLength(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        String longRole = "r".repeat(256);
        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "roles": ["%s"],
                    "enabled": true
                }
                """.formatted(protocol, getPayloadClientId(isOidc), longRole)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("roles[].<iterable element>: size must be between 0 and 255"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void testRolesMaxSize(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        String roles = IntStream.range(0, 301)
                .mapToObj(i -> "\"role-" + i + "\"")
                .collect(Collectors.joining(", "));
        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "roles": [%s],
                    "enabled": true
                }
                """.formatted(protocol, getPayloadClientId(isOidc), roles)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("roles: size must be between 0 and 300"));
        }
    }

    // ===========================================
    // Size constraint tests (OIDCClientRepresentation)
    // ===========================================

    @Test
    public void testWebOriginsElementMaxLength() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        String longOrigin = "http://example.com:" + "1".repeat(247);
        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "webOrigins": ["%s"],
                    "enabled": true
                }
                """.formatted(getPayloadClientId(), longOrigin)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("webOrigins[].<iterable element>: size must be between 0 and 255"));
        }
    }

    @Test
    public void testWebOriginsMaxSize() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        String origins = IntStream.range(0, 101)
                .mapToObj(i -> "\"http://origin" + i + ".example.com\"")
                .collect(Collectors.joining(", "));
        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "webOrigins": [%s],
                    "enabled": true
                }
                """.formatted(getPayloadClientId(), origins)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("webOrigins: size must be between 0 and 100"));
        }
    }

    @Test
    public void testWebOriginsInvalidFormat() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "webOrigins": ["not-an-origin"],
                    "enabled": true
                }
                """.formatted(getPayloadClientId())));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem(containsString("webOrigins")));
            assertThat(body.violations(), hasItem(containsString("must be a valid web origin")));
        }
    }

    @Test
    public void testServiceAccountRolesElementMaxLength() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        String longRole = "r".repeat(256);
        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "loginFlows": ["SERVICE_ACCOUNT"],
                    "auth": {"method": "client-secret", "secret": "my-test-secret-value"},
                    "serviceAccountRoles": ["%s"],
                    "enabled": true
                }
                """.formatted(getPayloadClientId(), longRole)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("serviceAccountRoles[].<iterable element>: size must be between 0 and 255"));
        }
    }

    @Test
    public void testServiceAccountRolesMaxSize() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        String roles = IntStream.range(0, 301)
                .mapToObj(i -> "\"sa-role-" + i + "\"")
                .collect(Collectors.joining(", "));
        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "loginFlows": ["SERVICE_ACCOUNT"],
                    "auth": {"method": "client-secret", "secret": "my-test-secret-value"},
                    "serviceAccountRoles": [%s],
                    "enabled": true
                }
                """.formatted(getPayloadClientId(), roles)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("serviceAccountRoles: size must be between 0 and 300"));
        }
    }

    // ===========================================
    // Auth field constraint tests (OIDC-only)
    // ===========================================

    @Test
    public void testAuthMethodNotBlank() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "auth": {"method": "   ", "secret": "my-test-secret-value"},
                    "enabled": true
                }
                """.formatted(getPayloadClientId())));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("auth.method: must not be blank"));
        }
    }

    @Test
    public void testAuthMethodInvalidProvider() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "auth": {"method": "nonexistent-provider", "secret": "my-test-secret-value"},
                    "enabled": true
                }
                """.formatted(getPayloadClientId())));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem(containsString("auth.method")));
            assertThat(body.violations(), hasItem(containsString("valid client authenticator type is required")));
        }
    }

    @Test
    public void testSecretMinLength() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "auth": {"method": "client-secret", "secret": "ab"},
                    "enabled": true
                }
                """.formatted(getPayloadClientId())));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("auth.secret: size must be between 6 and 255"));
        }
    }

    @Test
    public void testCertificateMaxLength() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        String longCert = "X".repeat(65537);
        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "auth": {"method": "client-jwt", "certificate": "%s"},
                    "enabled": true
                }
                """.formatted(getPayloadClientId(), longCert)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("auth.certificate: size must be between 0 and 65536"));
        }
    }

    // ===========================================
    // SAML field constraint tests (SAML-only)
    // ===========================================

    @Test
    public void testNameIdFormatInvalidValue() throws Exception {
        var request = getRequest(false);
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "saml",
                    "clientId": "%s",
                    "nameIdFormat": "foobar",
                    "enabled": true
                }
                """.formatted(getPayloadClientId(false))));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            String responseBody = EntityUtils.toString(response.getEntity());
            switch (getHttpMethod()) {
                case HttpPatch.METHOD_NAME -> assertThat(responseBody, containsString("Invalid values for these fields: nameIdFormat"));
                default -> assertThat(responseBody, containsString("Cannot parse the JSON"));
            }
        }
    }

    @Test
    public void testSignatureAlgorithmInvalidValue() throws Exception {
        var request = getRequest(false);
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "saml",
                    "clientId": "%s",
                    "signatureAlgorithm": "INVALID_ALGO",
                    "enabled": true
                }
                """.formatted(getPayloadClientId(false))));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            String responseBody = EntityUtils.toString(response.getEntity());
            if (getHttpMethod().equals(HttpPatch.METHOD_NAME)) {
                assertThat(responseBody, containsString("Invalid values for these fields: signatureAlgorithm"));
            } else {
                assertThat(responseBody, containsString("Cannot parse the JSON"));
            }
        }
    }

    @Test
    public void testCanonicalizationMethodInvalidValue() throws Exception {
        var request = getRequest(false);
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "saml",
                    "clientId": "%s",
                    "signatureCanonicalizationMethod": "http://example.com/invalid",
                    "enabled": true
                }
                """.formatted(getPayloadClientId(false))));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem(containsString("signatureCanonicalizationMethod")));
            assertThat(body.violations(), hasItem(containsString("must be a valid XML canonicalization method URI")));
        }
    }

    @Test
    public void testSigningCertificateMaxLength() throws Exception {
        var request = getRequest(false);
        setAuthHeader(request);

        String longCert = "X".repeat(65537);
        request.setEntity(new StringEntity("""
                {
                    "protocol": "saml",
                    "clientId": "%s",
                    "signingCertificate": "%s",
                    "enabled": true
                }
                """.formatted(getPayloadClientId(false), longCert)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("signingCertificate: size must be between 0 and 65536"));
        }
    }

    // ===========================================
    // Cross-field constraint tests (OIDC-only)
    // ===========================================

    @Test
    public void testServiceAccountRequiresConfidentialClient() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "loginFlows": ["SERVICE_ACCOUNT"],
                    "enabled": true
                }
                """.formatted(getPayloadClientId())));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem(containsString("SERVICE_ACCOUNT")));
            assertThat(body.violations(), hasItem(containsString("requires a confidential client")));
        }
    }

    @Test
    public void testTokenExchangeRequiresConfidentialClient() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "loginFlows": ["TOKEN_EXCHANGE"],
                    "enabled": true
                }
                """.formatted(getPayloadClientId())));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem(containsString("TOKEN_EXCHANGE")));
            assertThat(body.violations(), hasItem(containsString("requires a confidential client")));
        }
    }

    @Test
    public void testStandardFlowRequiresRedirectUris() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "loginFlows": ["STANDARD"],
                    "redirectUris": [],
                    "auth": {"method": "client-secret", "secret": "my-test-secret-value"},
                    "enabled": true
                }
                """.formatted(getPayloadClientId())));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem(containsString("redirectUris")));
            assertThat(body.violations(), hasItem(containsString("requires at least one redirect URI")));
        }
    }

    @Test
    public void testImplicitFlowRequiresRedirectUris() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "loginFlows": ["IMPLICIT"],
                    "redirectUris": [],
                    "auth": {"method": "client-secret", "secret": "my-test-secret-value"},
                    "enabled": true
                }
                """.formatted(getPayloadClientId())));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem(containsString("redirectUris")));
            assertThat(body.violations(), hasItem(containsString("requires at least one redirect URI")));
        }
    }

    @Test
    public void testServiceAccountRolesRequireServiceAccountFlow() throws Exception {
        var request = getRequest();
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "loginFlows": ["STANDARD"],
                    "auth": {"method": "client-secret", "secret": "my-test-secret-value"},
                    "redirectUris": ["http://localhost/callback"],
                    "serviceAccountRoles": ["some-role"],
                    "enabled": true
                }
                """.formatted(getPayloadClientId())));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem(containsString("serviceAccountRoles can only be set when SERVICE_ACCOUNT flow is enabled")));
        }
    }

    // ===========================================
    // Valid client creation (positive test)
    // ===========================================

    @Test
    public void validSAMLClientSucceeds() throws Exception {
        var randomClientId = "valid-random-saml-client";
        var request = isPutRequest() ?
                new HttpPut(getClientApiUrl(randomClientId)) :
                new HttpPost(getClientsApiUrl());
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "saml",
                    "clientId": "%s",
                    "displayName": "Valid SAML Client",
                    "enabled": true,
                    "nameIdFormat": "email",
                    "signDocuments": true
                }
                """.formatted(randomClientId)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(201));
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    // ===========================================
    // Valid client creation (positive test)
    // ===========================================

    @Test
    public void validOIDCClientSucceeds() throws Exception {
        var randomClientId = "valid-random-oidc-client";
        var request = isPutRequest() ?
                new HttpPut(getClientApiUrl(randomClientId)) :
                new HttpPost(getClientsApiUrl());
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "displayName": "Valid OIDC Client",
                    "description": "A properly configured client",
                    "enabled": true,
                    "appUrl": "http://localhost:3000",
                    "redirectUris": ["http://localhost:3000/callback", "http://localhost:3000/silent-refresh"],
                    "webOrigins": ["http://localhost:3000"],
                    "roles": ["user", "admin"],
                    "loginFlows": ["STANDARD", "DIRECT_GRANT"]
                }
                """.formatted(randomClientId)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(201));
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void clientIdMismatchBetweenPathAndPayloadFails(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var request = getRequest(isOidc);
        setAuthHeader(request);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "different-client-id",
                    "enabled": true
                }
                """.formatted(protocol)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Field 'clientId' in payload does not match the provided 'clientId'"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void clientWithTypeMismatchFails(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var otherProtocol = isOidc ? SAMLClientRepresentation.PROTOCOL : OIDCClientRepresentation.PROTOCOL;
        var request = getRequest(isOidc);
        setAuthHeader(request);

        // Cannot change the protocol for already existing client
        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s"
                }
                """.formatted(otherProtocol, getPayloadClientId(isOidc))));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = EntityUtils.toString(response.getEntity());
            switch (getHttpMethod()){
                case HttpPut.METHOD_NAME -> assertThat(body, CoreMatchers.containsString("protocol cannot be changed for an existing client"));
                case HttpPatch.METHOD_NAME -> assertThat(body, anyOf(
                        CoreMatchers.containsString("Invalid values for these fields: protocol"),
                        CoreMatchers.containsString("protocol cannot be changed for an existing client")));
            }
        }

        // check order
        request.setEntity(new StringEntity("""
                {
                    "enabled": "not-a-boolean",
                    "protocol": "unknown",
                    "clientId": "%s"
                }
                """.formatted(getPayloadClientId(isOidc))));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = EntityUtils.toString(response.getEntity());
            // Prefer the first structural error the stack records (varies with JSON vs. merge semantics).
            switch (getHttpMethod()){
                case HttpPut.METHOD_NAME -> assertThat(body, anyOf(
                        CoreMatchers.containsString("Cannot parse the JSON"),
                        CoreMatchers.containsString("Invalid values for these fields: enabled"),
                        CoreMatchers.containsString("unsupported client protocol"),
                        CoreMatchers.containsString("Mapper not found")));
                case HttpPatch.METHOD_NAME -> assertThat(body, CoreMatchers.containsString("Invalid values for these fields: enabled"));
            }
        }
    }

    protected boolean isPutRequest() {
        return getHttpMethod().equals(HttpPut.METHOD_NAME);
    }

    public static class AdminV2Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }

    public static class TestOidcClient implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("test-client-oidc")
                    .enabled(true)
                    .redirectUris("http://localhost/callback")
                    .protocol("openid-connect");
        }
    }

    public static class TestSamlClient implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("test-client-saml")
                    .enabled(true)
                    .protocol("saml");
        }
    }
}
