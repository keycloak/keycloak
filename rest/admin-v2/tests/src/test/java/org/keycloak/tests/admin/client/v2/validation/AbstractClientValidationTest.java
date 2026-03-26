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

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.common.Profile;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.services.error.ViolationExceptionResponse;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
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
                case HttpPatch.METHOD_NAME -> {
                    assertThat(response.getStatusLine().getStatusCode(), is(200));
                }
                case HttpPut.METHOD_NAME -> {
                    assertThat(response.getStatusLine().getStatusCode(), is(400));
                    var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
                    assertThat(body.error(), is("Field 'clientId' in payload does not match the provided 'clientId'"));
                }
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
            switch (getHttpMethod()) {
                case HttpPatch.METHOD_NAME -> assertThat(response.getStatusLine().getStatusCode(), is(200));
                case HttpPost.METHOD_NAME, HttpPut.METHOD_NAME -> {
                    assertThat(response.getStatusLine().getStatusCode(), is(400));
                    assertThat(EntityUtils.toString(response.getEntity()), containsString("Cannot parse the JSON"));
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
            switch (getHttpMethod()) {
                case HttpPatch.METHOD_NAME ->
                        assertThat(responseBody, containsString("Invalid values for these fields: protocol"));
                case HttpPost.METHOD_NAME, HttpPut.METHOD_NAME ->
                        // happening on the JAX-RS level implicitly
                        assertThat(responseBody, containsString("Cannot parse the JSON"));
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
            // Should have violations for the invalid URLs
            assertThat(body.violations().size(), greaterThanOrEqualTo(1));
            assertThat(body.violations(), hasItem("redirectUris[].<iterable element>: Each redirect URL must be valid"));
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
            assertThat(body.violations(), hasItem("redirectUris[].<iterable element>: Each redirect URL must be valid"));
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

        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "appUrl": "invalid-url",
                    "redirectUris": ["also-invalid"]
                }
                """.formatted(protocol, getPayloadClientId(isOidc))));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            // Should have violations for both appUrl and redirectUris
            assertThat(body.violations(), hasItem("appUrl: must be a valid URL"));
            assertThat(body.violations(), hasItem(containsString("Each redirect URL must be valid")));
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
            switch (getHttpMethod()){
                case HttpPut.METHOD_NAME -> assertThat(EntityUtils.toString(response.getEntity()), CoreMatchers.containsString("protocol cannot be changed for an existing client"));
                case HttpPatch.METHOD_NAME -> assertThat(EntityUtils.toString(response.getEntity()), CoreMatchers.containsString("Invalid values for these fields: protocol"));
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
            // always only the first one is recorded
            switch (getHttpMethod()){
                case HttpPut.METHOD_NAME ->  assertThat(EntityUtils.toString(response.getEntity()), CoreMatchers.containsString("Cannot parse the JSON"));
                case HttpPatch.METHOD_NAME ->  assertThat(EntityUtils.toString(response.getEntity()), CoreMatchers.containsString("Invalid values for these fields: enabled"));
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
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId("test-client-oidc")
                    .enabled(true)
                    .protocol("openid-connect");
        }
    }

    public static class TestSamlClient implements ClientConfig {
        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId("test-client-saml")
                    .enabled(true)
                    .protocol("saml");
        }
    }
}
