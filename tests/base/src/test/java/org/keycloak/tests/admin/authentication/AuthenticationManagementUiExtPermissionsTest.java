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
package org.keycloak.tests.admin.authentication;

import java.io.IOException;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.AuthenticationFlowBuilder;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The "ui-ext/authentication-management" endpoints only require permission to view authentication flows, so the
 * clients and identity providers they report as using a flow must be filtered by the caller's client and identity
 * provider view permissions.
 */
@KeycloakIntegrationTest
public class AuthenticationManagementUiExtPermissionsTest {

    private static final String CLIENT_FLOW = "client-flow";
    private static final String IDP_FLOW = "idp-flow";
    private static final String HIDDEN_CLIENT = "hidden-client";
    private static final String HIDDEN_IDP = "hidden-idp";

    @InjectRealm(config = AuthenticationUsageRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectAdminClient(ref = "flowViewer", mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "flow-viewer")
    Keycloak flowViewer;

    @InjectAdminClient(ref = "fullViewer", mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "full-viewer")
    Keycloak fullViewer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    private String clientFlowId;
    private String idpFlowId;

    @BeforeEach
    public void bindClientToFlow() {
        clientFlowId = findFlow(CLIENT_FLOW).getId();
        idpFlowId = findFlow(IDP_FLOW).getId();

        ClientResource client = AdminApiUtil.findClientByClientId(managedRealm.admin(), HIDDEN_CLIENT);
        ClientRepresentation rep = client.toRepresentation();
        rep.setAuthenticationFlowBindingOverrides(Map.of("browser", clientFlowId));
        client.update(rep);
    }

    @Test
    public void flowSummaryHidesClientsAndIdpsWithoutViewPermission() throws IOException {
        JsonNode flows = getFlows(flowViewer);

        assertFalse(findFlow(flows, CLIENT_FLOW).has("usedBy"), "client usage must be hidden without view-clients");
        assertFalse(findFlow(flows, IDP_FLOW).has("usedBy"), "identity provider usage must be hidden without view-identity-providers");
    }

    @Test
    public void flowSummaryShowsClientsAndIdpsWithViewPermission() throws IOException {
        JsonNode flows = getFlows(fullViewer);

        JsonNode clientUsage = findFlow(flows, CLIENT_FLOW).get("usedBy");
        assertEquals("SPECIFIC_CLIENTS", clientUsage.get("type").asText());
        assertEquals(HIDDEN_CLIENT, clientUsage.get("values").get(0).asText());

        JsonNode idpUsage = findFlow(flows, IDP_FLOW).get("usedBy");
        assertEquals("SPECIFIC_PROVIDERS", idpUsage.get("type").asText());
        assertEquals(HIDDEN_IDP, idpUsage.get("values").get(0).asText());
    }

    @Test
    public void usedByListHidesClientsAndIdpsWithoutViewPermission() throws IOException {
        try (Response response = getUsedBy(flowViewer, "clients", clientFlowId)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals(0, readJson(response).size());
        }

        try (Response response = getUsedBy(flowViewer, "idp", idpFlowId)) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void usedByListShowsClientsAndIdpsWithViewPermission() throws IOException {
        try (Response response = getUsedBy(fullViewer, "clients", clientFlowId)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode clients = readJson(response);
            assertEquals(1, clients.size());
            assertEquals(HIDDEN_CLIENT, clients.get(0).get("label").asText());
        }

        try (Response response = getUsedBy(fullViewer, "idp", idpFlowId)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode idps = readJson(response);
            assertEquals(1, idps.size());
            assertEquals(HIDDEN_IDP, idps.get(0).get("label").asText());
        }
    }

    private AuthenticationFlowRepresentation findFlow(String alias) {
        return managedRealm.admin().flows().getFlows().stream()
                .filter(f -> alias.equals(f.getAlias()))
                .findFirst().orElseThrow();
    }

    private static JsonNode findFlow(JsonNode flows, String alias) {
        for (JsonNode flow : flows) {
            if (alias.equals(flow.get("alias").asText())) {
                return flow;
            }
        }
        throw new AssertionError("Flow not found: " + alias);
    }

    private JsonNode getFlows(Keycloak admin) throws IOException {
        try (Response response = request(admin, "flows")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode flows = readJson(response);
            assertTrue(flows.isArray());
            return flows;
        }
    }

    private Response getUsedBy(Keycloak admin, String type, String flowId) {
        return request(admin, type + "/" + flowId);
    }

    private Response request(Keycloak admin, String subPath) {
        Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true);
        Response response = httpClient.target(keycloakUrls.getBaseUrl().toString())
                .path("admin").path("realms").path(managedRealm.getName())
                .path("ui-ext").path("authentication-management").path(subPath)
                .register(new BearerAuthFilter(admin.tokenManager()))
                .request(MediaType.APPLICATION_JSON)
                .get();
        response.bufferEntity();
        httpClient.close();
        return response;
    }

    private static JsonNode readJson(Response response) throws IOException {
        return JsonSerialization.readValue(response.readEntity(String.class), JsonNode.class);
    }

    public static class AuthenticationUsageRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.authenticationFlows(
                    AuthenticationFlowBuilder.create(CLIENT_FLOW, "Bound to a client", "basic-flow", true, false)
                            .authenticationExecutions(),
                    AuthenticationFlowBuilder.create(IDP_FLOW, "Bound to an identity provider", "basic-flow", true, false)
                            .authenticationExecutions());

            realm.identityProviders(IdentityProviderBuilder.create()
                    .providerId("oidc")
                    .alias(HIDDEN_IDP)
                    .postBrokerLoginFlowAlias(IDP_FLOW));

            realm.clients(ClientBuilder.create("myclient")
                            .secret("mysecret")
                            .directAccessGrantsEnabled(true),
                    ClientBuilder.create(HIDDEN_CLIENT));

            realm.users(UserBuilder.create("flow-viewer")
                            .password("password")
                            .name("Flow", "Viewer")
                            .email("flow-viewer@localhost")
                            .emailVerified(true)
                            .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_REALM),
                    UserBuilder.create("full-viewer")
                            .password("password")
                            .name("Full", "Viewer")
                            .email("full-viewer@localhost")
                            .emailVerified(true)
                            .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_REALM,
                                    AdminRoles.VIEW_CLIENTS, AdminRoles.VIEW_IDENTITY_PROVIDERS));

            return realm;
        }
    }
}
