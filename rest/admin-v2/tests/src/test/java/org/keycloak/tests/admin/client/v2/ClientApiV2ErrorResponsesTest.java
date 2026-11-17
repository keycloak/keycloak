/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.common.Profile;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.services.error.ProblemDetail;
import org.keycloak.services.error.ProblemType;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.util.JsonSerialization;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = ClientApiV2ErrorResponsesTest.Config.class)
public class ClientApiV2ErrorResponsesTest extends AbstractClientApiV2Test {

    private static final String BASE = "http://localhost:8080/admin/api";
    private static final String CLIENTS_URL = BASE + "/master/clients/v2";
    private static final String SEED_CLIENT_ID = "error-response-seed";

    @InjectHttpClient
    CloseableHttpClient client;

    private static class GenericHttpRequest extends HttpEntityEnclosingRequestBase {
        private final String method;

        GenericHttpRequest(String method, String url) {
            this.method = method;
            setURI(URI.create(url));
        }

        @Override
        public String getMethod() {
            return method;
        }
    }

    @BeforeEach
    public void setupSeedClient() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId(SEED_CLIENT_ID);
        try (var response = getClientsApi().createClient(rep)) {
            assertEquals(201, response.getStatus());
        }
    }

    @AfterEach
    public void cleanupSeedClient() {
        try {
            getClientsApi().client(SEED_CLIENT_ID).deleteClient();
        } catch (Exception ignored) {
        }
    }

    private ResponseData execute(String method, String url, boolean auth, String accept, String contentType, String body) throws IOException {
        GenericHttpRequest req = new GenericHttpRequest(method, url);
        if (auth) {
            setAuthHeader(req);
        }
        if (accept != null) {
            req.setHeader(HttpHeaders.ACCEPT, accept);
        }
        if (body != null) {
            req.setEntity(new StringEntity(body, ContentType.create(contentType != null ? contentType : "application/json", StandardCharsets.UTF_8)));
            if (contentType != null) {
                req.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
            }
        } else if (contentType != null) {
            req.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
        }

        try (CloseableHttpResponse r = client.execute((HttpUriRequest) req)) {
            int status = r.getStatusLine().getStatusCode();
            Header contentTypeHeader = r.getFirstHeader("Content-Type");
            String ct = contentTypeHeader != null ? contentTypeHeader.getValue() : null;
            Header authHeader = r.getFirstHeader(HttpHeaders.WWW_AUTHENTICATE);
            String authenticate = authHeader != null ? authHeader.getValue() : null;
            Header allowHeader = r.getFirstHeader(HttpHeaders.ALLOW);
            String allow = allowHeader != null ? allowHeader.getValue() : null;
            String entity = r.getEntity() != null ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8) : null;
            ProblemDetail problem = entity != null && ct != null && ct.startsWith(ProblemDetail.MEDIA_TYPE)
                    ? JsonSerialization.readValue(entity, ProblemDetail.class)
                    : null;
            return new ResponseData(status, ct, authenticate, allow, entity, problem);
        }
    }

    private record ResponseData(int status, String contentType, String wwwAuthenticate, String allow, String rawBody, ProblemDetail problem) {}

    @Test
    public void validationFailedReturns400WithViolations() throws Exception {
        String body = "{\"protocol\":\"openid-connect\",\"clientId\":\"\"}";
        ResponseData res = execute("POST", CLIENTS_URL, true, "application/json", "application/json", body);

        assertEquals(400, res.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, res.contentType());
        assertNotNull(res.problem());
        assertEquals(ProblemType.VALIDATION_FAILED.getType(), res.problem().type());
        assertEquals(ProblemType.VALIDATION_FAILED.getTitle(), res.problem().title());
        assertEquals(400, res.problem().status());
        assertEquals("Provided data is invalid", res.problem().detail());
        assertNotNull(res.problem().violations());
        assertFalse(res.problem().violations().isEmpty());
        assertTrue(res.problem().violations().stream().anyMatch(v -> "clientId".equals(v.field())));
    }

    @Test
    public void malformedJsonReturns400() throws Exception {
        ResponseData res = execute("POST", CLIENTS_URL, true, "application/json", "application/json", "{invalid json");

        assertEquals(400, res.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, res.contentType());
        assertNotNull(res.problem());
        assertEquals(ProblemType.MALFORMED_BODY.getType(), res.problem().type());
        assertEquals("Malformed request body", res.problem().title());
        assertEquals("Cannot parse the JSON", res.problem().detail());
    }

    @Test
    public void emptyBodyReturns400MalformedBody() throws Exception {
        ResponseData postRes = execute("POST", CLIENTS_URL, true, "application/json", "application/json", "");
        assertEquals(400, postRes.status());
        assertEquals(ProblemType.MALFORMED_BODY.getType(), postRes.problem().type());
        assertEquals("A request body is required", postRes.problem().detail());

        ResponseData putRes = execute("PUT", CLIENTS_URL + "/" + SEED_CLIENT_ID, true, "application/json", "application/json", "");
        assertEquals(400, putRes.status());
        assertEquals(ProblemType.MALFORMED_BODY.getType(), putRes.problem().type());
        assertEquals("A request body is required", putRes.problem().detail());
    }

    private void assertViolation(ResponseData res, String field, String message) {
        assertEquals(400, res.status());
        assertEquals(ProblemType.VALIDATION_FAILED.getType(), res.problem().type());
        assertEquals(List.of(new ProblemDetail.Violation(field, message)), res.problem().violations());
    }

    @Test
    public void unknownFieldsInBodyReturns400WithViolations() throws Exception {
        String body = "{\"protocol\":\"openid-connect\",\"clientId\":\"unknown-prop-client\",\"unknownField\":\"bogus\"}";
        ResponseData res = execute("POST", CLIENTS_URL, true, "application/json", "application/json", body);

        assertViolation(res, "unknownField", "unknown field");
    }

    @Test
    public void identifierMismatchReturns400WithViolation() throws Exception {
        String body = "{\"protocol\":\"openid-connect\",\"clientId\":\"different-id\"}";
        ResponseData res = execute("PUT", CLIENTS_URL + "/" + SEED_CLIENT_ID, true, "application/json", "application/json", body);

        assertViolation(res, "clientId", "Field 'clientId' in payload does not match the provided 'clientId'");
    }

    @Test
    public void unsupportedProtocolReturns400WithViolation() throws Exception {
        String noProto = "{\"clientId\":\"no-proto-client\"}";
        assertViolation(execute("POST", CLIENTS_URL, true, "application/json", "application/json", noProto),
                "protocol", "protocol is required");

        String badProto = "{\"protocol\":\"unsupported\",\"clientId\":\"bad-proto-client\"}";
        assertViolation(execute("POST", CLIENTS_URL, true, "application/json", "application/json", badProto),
                "protocol", "unsupported client protocol: unsupported");
    }

    @Test
    public void invalidPaginationReturns400() throws Exception {
        ResponseData resNotNum = execute("GET", CLIENTS_URL + "?offset=abc", true, null, null, null);
        assertEquals(400, resNotNum.status());
        assertEquals(ProblemType.VALIDATION_FAILED.getType(), resNotNum.problem().type());
        assertEquals("offset and limit must be integers", resNotNum.problem().detail());

        assertViolation(execute("GET", CLIENTS_URL + "?offset=-1", true, null, null, null),
                "offset", "offset must be greater than or equal to 0");
        assertViolation(execute("GET", CLIENTS_URL + "?limit=0", true, null, null, null),
                "limit", "limit must be greater than or equal to 1");
    }

    @Test
    public void invalidSortReturns400() throws Exception {
        assertViolation(execute("GET", CLIENTS_URL + "?sort=clientId%7Cinvalid", true, null, null, null),
                "sort", "sort direction must be asc or desc");
        assertViolation(execute("GET", CLIENTS_URL + "?sort=unsupportedField", true, null, null, null),
                "sort", "unsupportedField is not a sortable field");
    }

    @Test
    public void invalidFieldSelectionReturns400() throws Exception {
        assertViolation(execute("GET", CLIENTS_URL + "?fields=unknownField", true, null, null, null),
                "fields", "unknownField is an unknown field");
    }

    @Test
    public void invalidQueryReturns400() throws Exception {
        assertViolation(execute("GET", CLIENTS_URL + "?q=%28%28", true, null, null, null),
                "q", "The 'q' filter expression is not valid");
        assertViolation(execute("GET", CLIENTS_URL + "?q=unknownProp%20eq%20%22val%22", true, null, null, null),
                "q", "Unknown query field: unknownProp");
    }

    @Test
    public void invalidPatchReturns400() throws Exception {
        String seedUrl = CLIENTS_URL + "/" + SEED_CLIENT_ID;
        ResponseData resNonObj = execute("PATCH", seedUrl, true, null, "application/merge-patch+json", "[1, 2]");
        assertEquals(400, resNonObj.status());
        assertEquals(ProblemType.INVALID_PATCH.getType(), resNonObj.problem().type());
        assertEquals("Cannot replace client resource with non-object", resNonObj.problem().detail());

        ResponseData resEmpty = execute("PATCH", seedUrl, true, null, "application/merge-patch+json", "");
        assertEquals(400, resEmpty.status());
        assertEquals(ProblemType.INVALID_PATCH.getType(), resEmpty.problem().type());
        assertEquals("Invalid patch", resEmpty.problem().detail());

        ResponseData resBadType = execute("PATCH", seedUrl, true, null, "application/merge-patch+json", "{\"redirectUris\":\"not-a-list\"}");
        assertEquals(400, resBadType.status());
        assertEquals(ProblemType.INVALID_PATCH.getType(), resBadType.problem().type());
        assertEquals("Invalid value for redirectUris", resBadType.problem().detail());
    }

    @Test
    public void unauthenticatedReturns401WithWwwAuthenticateHeader() throws Exception {
        ResponseData resNoToken = execute("GET", CLIENTS_URL, false, "application/json", null, null);
        assertEquals(401, resNoToken.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, resNoToken.contentType());
        assertEquals("Bearer", resNoToken.wwwAuthenticate());
        assertNotNull(resNoToken.problem());
        assertEquals(ProblemType.UNAUTHENTICATED.getType(), resNoToken.problem().type());
        assertEquals("Authentication required", resNoToken.problem().title());
        assertNull(resNoToken.problem().detail());

        // Test with Accept: text/html to verify problem+json is returned instead of HTML
        ResponseData resHtml = execute("GET", CLIENTS_URL, false, "text/html", null, null);
        assertEquals(401, resHtml.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, resHtml.contentType());
        assertEquals("Bearer", resHtml.wwwAuthenticate());
        assertNotNull(resHtml.problem());
        assertEquals(ProblemType.UNAUTHENTICATED.getType(), resHtml.problem().type());
    }

    @Test
    public void notFoundReturns404ProblemDetail() throws Exception {
        ResponseData resClient = execute("GET", CLIENTS_URL + "/non-existent-client-id", true, null, null, null);
        assertEquals(404, resClient.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, resClient.contentType());
        assertNotNull(resClient.problem());
        assertEquals(ProblemType.NOT_FOUND.getType(), resClient.problem().type());
        assertEquals("Cannot find the specified client", resClient.problem().detail());

        ResponseData resRealm = execute("GET", BASE + "/non-existent-realm/clients/v2", true, null, null, null);
        assertEquals(404, resRealm.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, resRealm.contentType());
        assertNotNull(resRealm.problem());
        assertEquals(ProblemType.NOT_FOUND.getType(), resRealm.problem().type());
        assertEquals("Realm not found.", resRealm.problem().detail());

        ResponseData resUnmatched = execute("GET", BASE, true, null, null, null);
        assertEquals(404, resUnmatched.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, resUnmatched.contentType());
        assertNotNull(resUnmatched.problem());
        assertEquals(ProblemType.NOT_FOUND.getType(), resUnmatched.problem().type());
    }

    @Test
    public void methodNotAllowedReturns405ProblemDetail() throws Exception {
        ResponseData resDeleteCollection = execute("DELETE", CLIENTS_URL, true, null, null, null);
        assertEquals(405, resDeleteCollection.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, resDeleteCollection.contentType());
        assertNotNull(resDeleteCollection.problem());
        assertEquals(ProblemType.METHOD_NOT_ALLOWED.getType(), resDeleteCollection.problem().type());
        assertEquals("Method not allowed", resDeleteCollection.problem().title());
        assertNotNull(resDeleteCollection.allow(), "Allow header must be present on 405");
        assertTrue(resDeleteCollection.allow().contains("GET"), "Allow header must list GET");
        assertTrue(resDeleteCollection.allow().contains("POST"), "Allow header must list POST");

        ResponseData resPutCollection = execute("PUT", CLIENTS_URL, true, null, "application/json", "{}");
        assertEquals(405, resPutCollection.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, resPutCollection.contentType());
        assertNotNull(resPutCollection.problem());
        assertEquals(ProblemType.METHOD_NOT_ALLOWED.getType(), resPutCollection.problem().type());
        assertNotNull(resPutCollection.allow(), "Allow header must be present on 405");
        assertTrue(resPutCollection.allow().contains("GET"), "Allow header must list GET");
        assertTrue(resPutCollection.allow().contains("POST"), "Allow header must list POST");
    }

    @Test
    public void notAcceptableReturns406FallbackProblemDetail() throws Exception {
        ResponseData res = execute("GET", CLIENTS_URL, true, "application/xml", null, null);
        assertEquals(406, res.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, res.contentType());
        assertNotNull(res.problem());
        assertEquals(ProblemType.fallbackType(406), res.problem().type());
        assertEquals("Not Acceptable", res.problem().title());
    }

    @Test
    public void duplicateClientReturns409Conflict() throws Exception {
        String body = "{\"protocol\":\"openid-connect\",\"clientId\":\"" + SEED_CLIENT_ID + "\"}";
        ResponseData res = execute("POST", CLIENTS_URL, true, null, "application/json", body);

        assertEquals(409, res.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, res.contentType());
        assertNotNull(res.problem());
        assertEquals(ProblemType.CONFLICT.getType(), res.problem().type());
        assertEquals("Conflict", res.problem().title());
        assertEquals("A resource with the same identifying value already exists", res.problem().detail());
    }

    @Test
    public void unsupportedMediaTypeReturns415ProblemDetail() throws Exception {
        ResponseData resPostPlain = execute("POST", CLIENTS_URL, true, null, "text/plain", "raw-content");
        assertEquals(415, resPostPlain.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, resPostPlain.contentType());
        assertNotNull(resPostPlain.problem());
        assertEquals(ProblemType.UNSUPPORTED_MEDIA_TYPE.getType(), resPostPlain.problem().type());

        ResponseData resPatchJson = execute("PATCH", CLIENTS_URL + "/" + SEED_CLIENT_ID, true, null, "application/json-patch+json", "[]");
        assertEquals(415, resPatchJson.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, resPatchJson.contentType());
        assertNotNull(resPatchJson.problem());
        assertEquals(ProblemType.UNSUPPORTED_MEDIA_TYPE.getType(), resPatchJson.problem().type());
    }

    @Test
    public void browserAcceptHeaderNeverReturnsHtmlForErrors() throws Exception {
        ResponseData res404 = execute("GET", CLIENTS_URL + "/non-existent", true, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", null, null);
        assertEquals(404, res404.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, res404.contentType());
        assertNotNull(res404.problem());
        assertEquals(ProblemType.NOT_FOUND.getType(), res404.problem().type());

        ResponseData res400 = execute("POST", CLIENTS_URL, true, "text/html", "application/json", "{invalid json");
        assertEquals(400, res400.status());
        assertEquals(ProblemDetail.MEDIA_TYPE, res400.contentType());
        assertNotNull(res400.problem());
        assertEquals(ProblemType.MALFORMED_BODY.getType(), res400.problem().type());
    }

    public static class Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }
}
