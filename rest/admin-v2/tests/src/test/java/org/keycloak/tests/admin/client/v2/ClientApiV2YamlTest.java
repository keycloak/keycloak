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

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.admin.api.PatchTypeNames;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = ClientApiV2Test.AdminV2Config.class)
public class ClientApiV2YamlTest extends AbstractClientApiV2Test {

    private static final YAMLMapper YAML = new YAMLMapper();

    @InjectHttpClient
    CloseableHttpClient client;

    private JsonNode send(HttpEntityEnclosingRequestBase request, String contentType, String body, int expectedStatus) throws Exception {
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
        request.setHeader(HttpHeaders.ACCEPT, PatchTypeNames.APPLICATION_YAML);
        request.setEntity(new StringEntity(body));
        try (var response = client.execute(request)) {
            String entity = EntityUtils.toString(response.getEntity());
            assertEquals(expectedStatus, response.getStatusLine().getStatusCode(), entity);
            if (expectedStatus >= 300) {
                return null;
            }
            assertTrue(response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue().startsWith(PatchTypeNames.APPLICATION_YAML), response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue() + " " + entity);
            return YAML.readTree(entity);
        }
    }

    private JsonNode get(String url, int expectedStatus) throws Exception {
        HttpGet request = new HttpGet(url);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.ACCEPT, PatchTypeNames.APPLICATION_YAML);
        try (var response = client.execute(request)) {
            String entity = EntityUtils.toString(response.getEntity());
            assertEquals(expectedStatus, response.getStatusLine().getStatusCode(), entity);
            assertTrue(response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue().startsWith(PatchTypeNames.APPLICATION_YAML), response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue() + " " + entity);
            return YAML.readTree(entity);
        }
    }

    @Test
    public void getClientsAsYaml() throws Exception {
        JsonNode clients = get(getClientsApiUrl(), 200);
        assertTrue(clients.isArray(), clients.toString());
        assertTrue(clients.size() > 0);
        assertTrue(clients.get(0).has("clientId"));
    }

    @Test
    public void getClientAsYaml() throws Exception {
        JsonNode client = get(getClientApiUrl("account"), 200);
        assertEquals("account", client.get("clientId").asText());
        assertEquals("openid-connect", client.get("protocol").asText());
    }

    @Test
    public void postPutPatchYaml() throws Exception {
        String clientId = "yaml-client";

        JsonNode created = send(new HttpPost(getClientsApiUrl()), PatchTypeNames.APPLICATION_YAML,
                "protocol: openid-connect\nclientId: %s\nenabled: true\ndescription: from yaml\n".formatted(clientId), 201);
        assertEquals(clientId, created.get("clientId").asText());
        assertEquals("from yaml", created.get("description").asText());

        JsonNode updated = send(new HttpPut(getClientApiUrl(clientId)), PatchTypeNames.APPLICATION_YAML,
                "protocol: openid-connect\nclientId: %s\nenabled: true\ndescription: updated from yaml\n".formatted(clientId), 200);
        assertEquals("updated from yaml", updated.get("description").asText());

        JsonNode patched = send(new HttpPatch(getClientApiUrl(clientId)), PatchTypeNames.YAML_MERGE,
                "description: patched from yaml\n", 200);
        assertEquals("patched from yaml", patched.get("description").asText());
        assertEquals(clientId, patched.get("clientId").asText());

        // the JSON api view of the same client reflects the yaml changes
        OIDCClientRepresentation viaJson = (OIDCClientRepresentation) getClientApi(clientId).getClient();
        assertEquals("patched from yaml", viaJson.getDescription());
    }

    @Test
    public void putCreatesFromYaml() throws Exception {
        String clientId = "yaml-put-client";
        JsonNode created = send(new HttpPut(getClientApiUrl(clientId)), PatchTypeNames.APPLICATION_YAML,
                "protocol: openid-connect\nclientId: %s\nenabled: true\n".formatted(clientId), 201);
        assertEquals(clientId, created.get("clientId").asText());
    }

    @Test
    public void yamlPatchInvalid() throws Exception {
        String url = getClientApiUrl("account");
        send(new HttpPatch(url), PatchTypeNames.YAML_MERGE, "a: [unclosed", 400);
        send(new HttpPatch(url), PatchTypeNames.YAML_MERGE, "just a string", 400);
        send(new HttpPatch(url), PatchTypeNames.YAML_MERGE, "invalid: nothing", 400);
        send(new HttpPatch(url), PatchTypeNames.YAML_MERGE, "", 400);
        // additional documents are rejected rather than silently ignored
        send(new HttpPatch(url), PatchTypeNames.YAML_MERGE, "description: first\n---\ndescription: second\n", 400);
    }

    @Test
    public void yamlPostInvalid() throws Exception {
        send(new HttpPost(getClientsApiUrl()), PatchTypeNames.APPLICATION_YAML, "a: [unclosed", 400);
    }

    @Test
    public void unsupportedPatchType() throws Exception {
        send(new HttpPatch(getClientApiUrl("account")), "application/yaml", "description: x", 415);
    }
}
