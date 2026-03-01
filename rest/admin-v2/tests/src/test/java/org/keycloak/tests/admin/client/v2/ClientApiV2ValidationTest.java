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

package org.keycloak.tests.admin.client.v2;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.services.error.ViolationExceptionResponse;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpMessage;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
@KeycloakIntegrationTest(config = ClientApiV2ValidationTest.AdminV2Config.class)
public class ClientApiV2ValidationTest {

    public static final String HOSTNAME_LOCAL_ADMIN = "http://localhost:8080/admin/api/master/clients/v2";
    private static ObjectMapper mapper;

    @InjectHttpClient
    CloseableHttpClient client;

    @InjectAdminClient
    Keycloak adminClient;

    @BeforeAll
    public static void setupMapper() {
        mapper = new ObjectMapper();
    }

    // ===========================================
    // clientId validation tests
    // ===========================================

    @Test
    public void createClientWithoutClientIdFails() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "displayName": "Test Client",
                    "enabled": true
                }
                """));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("clientId: must not be blank"));
        }
    }

    @Test
    public void createClientWithBlankClientIdFails() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "   ",
                    "enabled": true
                }
                """));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("clientId: must not be blank"));
        }
    }

    @Test
    public void putClientWithoutClientIdFails() throws Exception {
        HttpPut request = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/new-client-no-id");
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "displayName": "Test Client",
                    "enabled": true
                }
                """));

        try (var response = client.execute(request)) {
            // PUT should fail because clientId in payload doesn't match path
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    // ===========================================
    // URL validation tests
    // ===========================================

    @Test
    public void createClientWithInvalidAppUrlFails() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "test-invalid-appurl",
                    "appUrl": "not-a-valid-url",
                    "enabled": true
                }
                """));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("appUrl: must be a valid URL"));
        }
    }

    @Test
    public void createClientWithInvalidRedirectUriFails() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "test-invalid-redirect",
                    "redirectUris": ["http://valid.com", "not-a-url", "also-invalid"],
                    "enabled": true
                }
                """));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            // Should have violations for the invalid URLs
            assertThat(body.violations().size(), greaterThanOrEqualTo(1));
            assertThat(body.violations(), hasItem("redirectUris[].<iterable element>: Each redirect URL must be valid"));
        }
    }

    @Test
    public void createClientWithBlankRedirectUriFails() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "test-blank-redirect",
                    "redirectUris": ["http://valid.com", "", "   "],
                    "enabled": true
                }
                """));

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

    @Test
    public void createClientWithBlankRoleFails() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "test-blank-role",
                    "roles": ["valid-role", "", "   "],
                    "enabled": true
                }
                """));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations().size(), greaterThanOrEqualTo(1));
            assertThat(body.violations(), hasItem("roles[].<iterable element>: must not be blank"));
        }
    }

    @Test
    public void createClientWithBlankWebOriginFails() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "test-blank-origin",
                    "webOrigins": ["http://valid.com", "", "   "],
                    "enabled": true
                }
                """));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations().size(), greaterThanOrEqualTo(1));
            assertThat(body.violations(), hasItem("webOrigins[].<iterable element>: must not be blank"));
        }
    }

    @Test
    public void createClientWithBlankServiceAccountRoleFails() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "test-blank-sa-role",
                    "loginFlows": ["SERVICE_ACCOUNT"],
                    "serviceAccountRoles": ["valid-role", "", "   "],
                    "enabled": true
                }
                """));

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

    @Test
    public void createClientWithUnknownFieldFails() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "test-unknown-field",
                    "enabled": true,
                    "unknownField": "some value",
                    "anotherUnknown": 123
                }
                """));

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

    @Test
    public void createClientWithMultipleValidationErrorsReturnsAll() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        // Note: clientId must be provided, otherwise validation stops at CreateClient group
        // and doesn't validate Default group (appUrl, redirectUris)
        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "test-multiple-errors",
                    "appUrl": "invalid-url",
                    "redirectUris": ["also-invalid"]
                }
                """));

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
    // SAML client validation tests
    // ===========================================

    @Test
    public void createSAMLClientWithoutClientIdFails() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "saml",
                    "displayName": "SAML Test Client",
                    "enabled": true
                }
                """));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("clientId: must not be blank"));
        }
    }

    @Test
    public void createSAMLClientWithInvalidAppUrlFails() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "saml",
                    "clientId": "saml-invalid-appurl",
                    "appUrl": "not-a-valid-url",
                    "enabled": true
                }
                """));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("appUrl: must be a valid URL"));
        }
    }

    @Test
    public void createValidSAMLClientSucceeds() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "saml",
                    "clientId": "saml-valid-client",
                    "displayName": "Valid SAML Client",
                    "enabled": true,
                    "nameIdFormat": "email",
                    "signDocuments": true
                }
                """));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(201));
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    // ===========================================
    // Valid client creation (positive test)
    // ===========================================

    @Test
    public void createValidOIDCClientSucceeds() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "valid-oidc-client",
                    "displayName": "Valid OIDC Client",
                    "description": "A properly configured client",
                    "enabled": true,
                    "appUrl": "http://localhost:3000",
                    "redirectUris": ["http://localhost:3000/callback", "http://localhost:3000/silent-refresh"],
                    "webOrigins": ["http://localhost:3000"],
                    "roles": ["user", "admin"],
                    "loginFlows": ["STANDARD", "DIRECT_GRANT"]
                }
                """));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(201));
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    private void setAuthHeader(HttpMessage request) {
        String token = adminClient.tokenManager().getAccessTokenString();
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    public static class AdminV2Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }
}
