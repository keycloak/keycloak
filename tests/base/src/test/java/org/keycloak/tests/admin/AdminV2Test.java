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

package org.keycloak.tests.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.api.client.ClientApi;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

@KeycloakIntegrationTest()
public class AdminV2Test {

    private static final String HOSTNAME_LOCAL_ADMIN = "http://localhost:8080/admin/api/v2";

    @InjectHttpClient
    private HttpClient client;

    @Test
    public void testGetClient() throws Exception {
        HttpGet request = new HttpGet(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients/account");
        HttpResponse response = client.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        ObjectMapper mapper = new ObjectMapper();
        ClientRepresentation client = mapper.createParser(response.getEntity().getContent()).readValueAs(ClientRepresentation.class);
        assertEquals("account", client.getClientId());
    }

    @Test
    public void testJsonPatchClient() throws Exception {
        HttpPatch request = new HttpPatch(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients/account");
        request.setEntity(new StringEntity("not json"));
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_PATCH_JSON);
        HttpResponse response = client.execute(request);
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(400, response.getStatusLine().getStatusCode());

        request.setEntity(new StringEntity(
                """
                [{"op": "add", "path": "/description", "value": "I'm a description"}]
                """));

        response = client.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());

        ObjectMapper mapper = new ObjectMapper();
        ClientRepresentation client = mapper.createParser(response.getEntity().getContent()).readValueAs(ClientRepresentation.class);
        assertEquals("I'm a description", client.getDescription());
    }

    @Disabled
    @Test
    public void testJsonMergePatchClient() throws Exception {
        HttpPatch request = new HttpPatch(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients/account");
        request.setHeader(HttpHeaders.CONTENT_TYPE, ClientApi.CONENT_TYPE_MERGE_PATCH);

        ClientRepresentation patch = new ClientRepresentation();
        patch.setDescription("I'm also a description");

        ObjectMapper mapper = new ObjectMapper();

        request.setEntity(new StringEntity(mapper.writeValueAsString(patch)));

        HttpResponse response = client.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());

        ClientRepresentation client = mapper.createParser(response.getEntity().getContent()).readValueAs(ClientRepresentation.class);
        assertEquals("I'm also a description", client.getDescription());
    }

}
