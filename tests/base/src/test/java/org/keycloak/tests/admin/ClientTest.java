/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.common.util.Time;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.TestAvailabilityAction;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.TestApp;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectTestApp;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;

import static org.keycloak.models.Constants.defaultClients;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class ClientTest {

    @InjectRealm(ref = "default", config = ClientTestRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm managedMasterRealm;

    @InjectOAuthClient(realmRef = "default", kcAdmin = true)
    OAuthClient oauth;

    @InjectTestApp
    TestApp testApp;

    @InjectAdminEvents(realmRef = "default")
    AdminEvents adminEvents;

    @Test
    public void getClients() {
        Assert.assertNames(managedRealm.admin().clients().findAll(), "account", "account-console", "realm-management", "security-admin-console", "broker", "test-app", Constants.ADMIN_CLI_CLIENT_ID);
    }

    @Test
    public void getRealmClients() {
        assertTrue(managedRealm.admin().clients().findAll().stream().filter(client -> client.getAttributes().get(Constants.REALM_CLIENT).equals("true"))
                .map(ClientRepresentation::getClientId)
                .allMatch(clientId -> clientId.equals(Constants.REALM_MANAGEMENT_CLIENT_ID) || clientId.equals(Constants.BROKER_SERVICE_CLIENT_ID) || clientId.endsWith("-realm")));
    }

    private ClientRepresentation createClient() {
        return createClient(null);
    }

    private ClientRepresentation createClient(String protocol) {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("my-app");
        rep.setDescription("my-app description");
        rep.setEnabled(true);
        rep.setPublicClient(true);
        if (protocol != null) {
            rep.setProtocol(protocol);
        }
        Response response = managedRealm.admin().clients().create(rep);
        String id = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.clients().get(id).remove());
        ClientRepresentation found = AdminApiUtil.findClientByClientId(managedRealm.admin(), "my-app").toRepresentation();

        assertEquals("my-app", found.getClientId());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientResourcePath(id), rep, ResourceType.CLIENT);

        rep.setId(id);

        return rep;
    }

    private ClientRepresentation createClientNonPublic() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("my-app");
        rep.setDescription("my-app description");
        rep.setEnabled(true);
        rep.setPublicClient(false);
        Response response = managedRealm.admin().clients().create(rep);
        String id = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.clients().get(id).remove());
        ClientRepresentation found = AdminApiUtil.findClientByClientId(managedRealm.admin(), "my-app").toRepresentation();

        assertEquals("my-app", found.getClientId());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientResourcePath(id), rep, ResourceType.CLIENT);

        rep.setId(id);

        return rep;
    }

    @Test
    public void createClientVerifyWithSecret() {
        String id = createClientNonPublic().getId();

        ClientResource client = managedRealm.admin().clients().get(id);
        assertNotNull(client);
        assertNotNull(client.toRepresentation().getSecret());
        Assert.assertNames(managedRealm.admin().clients().findAll(), "account", "account-console", "realm-management", "security-admin-console", "broker", "my-app", "test-app", Constants.ADMIN_CLI_CLIENT_ID);
    }

    @Test
    public void createClientVerify() {
        String id = createClient().getId();
        ClientResource client = managedRealm.admin().clients().get(id);
        assertNotNull(client);
        assertNull(client.toRepresentation().getSecret());
        Assert.assertNames(managedRealm.admin().clients().findAll(), "account", "account-console", "realm-management", "security-admin-console", "broker", "my-app", "test-app", Constants.ADMIN_CLI_CLIENT_ID);
    }

    @Test
    public void testCreateClientWithBlankClientId() {
        ClientRepresentation rep = ClientConfigBuilder.create()
                .clientId("")
                .description("blank")
                .enabled(true)
                .publicClient(true)
                .build();
        try (Response response = managedRealm.admin().clients().create(rep)) {
            if (response.getStatus() != 400) {
                response.bufferEntity();
                String body = response.readEntity(String.class);
                fail("expect 400 Bad request response code but receive: " + response.getStatus() + "\n" + body);
            }
        }
    }


    @Test
    public void testInvalidLengthClientIdValidation() {
        ClientRepresentation rep = ClientConfigBuilder.create()
                .id("test-long-invalid-client-id-validation-400-bad-request")
                .clientId("test-long-invalid-client-id-validation-400-bad-request")
                .description("invalid-client-id-app description")
                .enabled(true)
                .publicClient(true)
                .build();
        try (Response response = managedRealm.admin().clients().create(rep)) {
            if (response.getStatus() != 400) {
                response.bufferEntity();
                String body = response.readEntity(String.class);
                fail("expect 400 Bad request response code but receive: " + response.getStatus() + "\n" + body);
            }
        }
    }

    @Test
    public void testInvalidUrlClientValidation() {
        testClientUriValidation("Root URL is not a valid URL",
                "Base URL is not a valid URL",
                "Backchannel logout URL is not a valid URL",
                null,
                "invalid", "myapp://some-fake-app");
    }

    @Test
    public void testIllegalSchemeClientValidation() {
        testClientUriValidation("Root URL uses an illegal scheme",
                "Base URL uses an illegal scheme",
                "Backchannel logout URL uses an illegal scheme",
                "A redirect URI uses an illegal scheme",
                "data:text/html;base64,PHNjcmlwdD5jb25maXJtKGRvY3VtZW50LmRvbWFpbik7PC9zY3JpcHQ+",
                "javascript:confirm(document.domain)/*"
        );
    }

    // KEYCLOAK-3421
    @Test
    public void testFragmentProhibitedClientValidation() {
        testClientUriValidation("Root URL must not contain an URL fragment",
                null,
                null,
                "Redirect URIs must not contain an URI fragment",
                "http://redhat.com/abcd#someFragment"
        );
    }

    @Test
    public void testSamlSpecificUrls() {
        testSamlSpecificUrls(true, "javascript:alert('TEST')", "data:text/html;base64,PHNjcmlwdD5jb25maXJtKGRvY3VtZW50LmRvbWFpbik7PC9zY3JpcHQ+");
        testSamlSpecificUrls(false, "javascript:alert('TEST')", "data:text/html;base64,PHNjcmlwdD5jb25maXJtKGRvY3VtZW50LmRvbWFpbik7PC9zY3JpcHQ+");
    }

    private void testSamlSpecificUrls(boolean create, String... testUrls) {
        ClientRepresentation rep;
        if (create) {
            rep = new ClientRepresentation();
            rep.setClientId("my-app2");
            rep.setEnabled(true);
            rep.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        } else {
            rep = createClient(SamlProtocol.LOGIN_PROTOCOL);
        }
        rep.setAttributes(new HashMap<>());

        Map<String, String> attrs = Map.of(
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "Assertion Consumer Service POST Binding URL",
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, "Assertion Consumer Service Redirect Binding URL",
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE, "Artifact Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "Logout Service POST Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, "Logout Service ARTIFACT Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "Logout Service Redirect Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE, "Logout Service SOAP Binding URL",
                SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE, "Artifact Resolution Service");

        for (String testUrl : testUrls) {
            // admin url
            rep.setAdminUrl(testUrl);
            createOrUpdateClientExpectingValidationErrors(rep, create, "Master SAML Processing URL uses an illegal scheme");
            rep.setAdminUrl(null);
            // attributes
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                rep.getAttributes().put(entry.getKey(), testUrl);
                createOrUpdateClientExpectingValidationErrors(rep, create, entry.getValue() + " uses an illegal scheme");
                rep.getAttributes().remove(entry.getKey());
            }
        }
    }

    private void testClientUriValidation(String expectedRootUrlError, String expectedBaseUrlError, String expectedBackchannelLogoutUrlError, String expectedRedirectUrisError, String... testUrls) {
        testClientUriValidation(false, expectedRootUrlError, expectedBaseUrlError, expectedBackchannelLogoutUrlError, expectedRedirectUrisError, testUrls);
        testClientUriValidation(true, expectedRootUrlError, expectedBaseUrlError, expectedBackchannelLogoutUrlError, expectedRedirectUrisError, testUrls);
    }

    private void testClientUriValidation(boolean create, String expectedRootUrlError, String expectedBaseUrlError, String expectedBackchannelLogoutUrlError, String expectedRedirectUrisError, String... testUrls) {
        ClientRepresentation rep;
        if (create) {
            rep = new ClientRepresentation();
            rep.setClientId("my-app2");
            rep.setEnabled(true);
        } else {
            rep = createClient();
        }

        for (String testUrl : testUrls) {
            if (expectedRootUrlError != null) {
                rep.setRootUrl(testUrl);
                createOrUpdateClientExpectingValidationErrors(rep, create, expectedRootUrlError);
            }
            rep.setRootUrl(null);

            if (expectedBaseUrlError != null) {
                rep.setBaseUrl(testUrl);
                createOrUpdateClientExpectingValidationErrors(rep, create, expectedBaseUrlError);
            }
            rep.setBaseUrl(null);

            if (expectedBackchannelLogoutUrlError != null) {
                OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).setBackchannelLogoutUrl(testUrl);
                createOrUpdateClientExpectingValidationErrors(rep, create, expectedBackchannelLogoutUrlError);
            }
            OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).setBackchannelLogoutUrl(null);

            if (expectedRedirectUrisError != null) {
                rep.setRedirectUris(List.of(testUrl));
                createOrUpdateClientExpectingValidationErrors(rep, create, expectedRedirectUrisError);
            }
            rep.setRedirectUris(null);

            if (expectedRootUrlError != null) rep.setRootUrl(testUrl);
            if (expectedBaseUrlError != null) rep.setBaseUrl(testUrl);
            if (expectedRedirectUrisError != null) rep.setRedirectUris(List.of(testUrl));
            createOrUpdateClientExpectingValidationErrors(rep, create, expectedRootUrlError, expectedBaseUrlError, expectedRedirectUrisError);

            rep.setRootUrl(null);
            rep.setBaseUrl(null);
            rep.setRedirectUris(null);
        }
    }

    private void createOrUpdateClientExpectingValidationErrors(ClientRepresentation rep, boolean create, String... expectedErrors) {
        Response response = null;
        if (create) {
            response = managedRealm.admin().clients().create(rep);
        } else {
            try {
                managedRealm.admin().clients().get(rep.getId()).update(rep);
                fail("Expected exception");
            } catch (BadRequestException e) {
                response = e.getResponse();
            }
        }

        expectedErrors = Arrays.stream(expectedErrors).filter(Objects::nonNull).toArray(String[]::new);

        assertEquals(400, response.getStatus());
        OAuth2ErrorRepresentation errorRep = response.readEntity(OAuth2ErrorRepresentation.class);
        List<String> actualErrors = asList(errorRep.getErrorDescription().split("; "));
        assertThat(actualErrors, containsInAnyOrder(expectedErrors));
        assertEquals("invalid_input", errorRep.getError());
    }

    @Test
    public void removeClient() {
        Response response = managedRealm.admin().clients().create(ClientConfigBuilder.create().clientId("my-app").build());
        String id = ApiUtil.getCreatedId(response);
        adminEvents.skip();

        assertNotNull(AdminApiUtil.findClientByClientId(managedRealm.admin(), "my-app"));
        managedRealm.admin().clients().get(id).remove();
        assertNull(AdminApiUtil.findClientResourceById(managedRealm.admin(), "my-app"));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.clientResourcePath(id), ResourceType.CLIENT);
    }

    @Test
    public void removeClientWithDependentCompositeRoles() {
        ClientRepresentation clientRep = ClientConfigBuilder.create().clientId("my-app").build();
        String id = ApiUtil.getCreatedId(managedRealm.admin().clients().create(clientRep));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientResourcePath(id), clientRep, ResourceType.CLIENT);
        ClientResource clientRsc = managedRealm.admin().clients().get(id);

        RoleRepresentation roleB = RoleConfigBuilder.create().name("role-b").build();
        clientRsc.roles().create(roleB);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(id, "role-b"), roleB, ResourceType.CLIENT_ROLE);

        RoleRepresentation roleA = RoleConfigBuilder.create().name("role-a").build();
         clientRsc.roles().create(roleA);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(id, "role-a"), roleA, ResourceType.CLIENT_ROLE);

        List<RoleRepresentation> composites = List.of( clientRsc.roles().get("role-b").toRepresentation());
        clientRsc.roles().get("role-a").addComposites(composites);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourceCompositesPath(id, "role-a"), composites, ResourceType.CLIENT_ROLE);

        clientRsc.remove();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.clientResourcePath(id), ResourceType.CLIENT);
    }

    @Test
    public void removeInternalClientExpectingBadRequestException() {
        final String testRealmClientId = AdminApiUtil.findClientByClientId(managedMasterRealm.admin(), managedRealm.getName() + "-realm")
                .toRepresentation().getId();

        assertThrows(BadRequestException.class,
                () -> managedMasterRealm.admin().clients().get(testRealmClientId).remove());

        defaultClients.forEach(defaultClient -> {
            final String defaultClientId = AdminApiUtil.findClientByClientId(managedRealm.admin(), defaultClient)
                    .toRepresentation().getId();

            assertThrows(BadRequestException.class,
                    () -> managedRealm.admin().clients().get(defaultClientId).remove());
        });
    }

    @Test
    public void getClientRepresentation() {
        String id = createClient().getId();

        ClientRepresentation rep = managedRealm.admin().clients().get(id).toRepresentation();
        assertEquals(id, rep.getId());
        assertEquals("my-app", rep.getClientId());
        assertTrue(rep.isEnabled());
    }

    /**
     * See <a href="https://issues.jboss.org/browse/KEYCLOAK-1918">KEYCLOAK-1918</a>
     */
    @Test
    public void getClientDescription() {
        String id = createClient().getId();

        ClientRepresentation rep = managedRealm.admin().clients().get(id).toRepresentation();
        assertEquals(id, rep.getId());
        assertEquals("my-app description", rep.getDescription());
    }

    @Test
    public void getClientSessions() {
        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertEquals(200, response.getStatusCode());

        AuthorizationEndpointResponse codeResponse = oauth.doLogin("test-user@localhost", "password");

        AccessTokenResponse response2 = oauth.doAccessTokenRequest(codeResponse.getCode());
        assertEquals(200, response2.getStatusCode());

        ClientResource app = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app");

        assertEquals(2, (long) app.getApplicationSessionCount().get("count"));

        List<UserSessionRepresentation> userSessions = app.getUserSessions(0, 100);
        assertEquals(2, userSessions.size());
        assertEquals(1, userSessions.get(0).getClients().size());
    }

    @Test
    public void getAllClients() {
        List<ClientRepresentation> allClients = managedRealm.admin().clients().findAll();
        assertNotNull(allClients);
        assertFalse(allClients.isEmpty());
    }

    @Test
    public void getAllClientsSearchAndPagination() {
        for (int i = 1; i <= 10; i++) {
            ClientRepresentation c = ClientConfigBuilder.create().clientId("ccx-" + (i < 10 ? "0" + i : i)).build();
            Response response = managedRealm.admin().clients().create(c);
            String id = ApiUtil.getCreatedId(response);
            managedRealm.cleanup().add(r -> r.clients().get(id).remove());
        }

        assertPaginatedClients(1, 10, managedRealm.admin().clients().findAll("ccx-", null, true, 0, 100));
        assertPaginatedClients(1, 5, managedRealm.admin().clients().findAll("ccx-", null, true, 0, 5));
        assertPaginatedClients(6, 10, managedRealm.admin().clients().findAll("ccx-", null, true, 5, 5));
    }

    private void assertPaginatedClients(int start, int end, List<ClientRepresentation> actual) {
        List<String> expected = new LinkedList<>();
        for (int i = start; i <= end; i++) {
            expected.add("ccx-" + (i < 10 ? "0" + i : i));
        }
        List<String> a = actual.stream().map(ClientRepresentation::getClientId).collect(Collectors.toList());
        assertThat(a, is(expected));

    }

    @Test
    public void getClientById() {
        createClient();
        ClientRepresentation rep = AdminApiUtil.findClientByClientId(managedRealm.admin(), "my-app").toRepresentation();
        ClientRepresentation gotById = managedRealm.admin().clients().get(rep.getId()).toRepresentation();
        assertClient(rep, gotById);
    }

    @Test
    // KEYCLOAK-1110
    public void deleteDefaultRole() {
        ClientRepresentation rep = createClient();
        String id = rep.getId();

        RoleRepresentation role = new RoleRepresentation("test", "test", false);
        managedRealm.admin().clients().get(id).roles().create(role);

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(id, "test"), role, ResourceType.CLIENT_ROLE);

        role = managedRealm.admin().clients().get(id).roles().get("test").toRepresentation();

        final String DEFAULT_ROLE = Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName();
        managedRealm.admin().roles().get(DEFAULT_ROLE).addComposites(List.of(role));

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourceCompositesPath(DEFAULT_ROLE), List.of(role), ResourceType.REALM_ROLE);

        assertThat(managedRealm.admin().roles().get(DEFAULT_ROLE).getRoleComposites().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()),
                hasItem(role.getName()));

        managedRealm.admin().clients().get(id).roles().deleteRole("test");

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.clientRoleResourcePath(id, "test"), ResourceType.CLIENT_ROLE);

        assertThat(managedRealm.admin().roles().get(DEFAULT_ROLE).getRoleComposites().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()),
                not(hasItem(role)));
    }

    @Test
    public void testProtocolMappers() {
        String clientDbId = createClient().getId();
        ProtocolMappersResource mappersResource = AdminApiUtil.findClientByClientId(managedRealm.admin(), "my-app").getProtocolMappers();

        protocolMappersTest(clientDbId, mappersResource);
    }

    @Test
    public void updateClient() {
        ClientRepresentation client = createClient();

        ClientRepresentation newClient = ClientConfigBuilder.create()
                .id(client.getId())
                .clientId(client.getClientId())
                .baseUrl("http://baseurl")
                .build();

        ClientResource clientRes = managedRealm.admin().clients().get(client.getId());
        clientRes.update(newClient);

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientResourcePath(client.getId()), newClient, ResourceType.CLIENT);

        ClientRepresentation storedClient = clientRes.toRepresentation();

        assertNull(storedClient.getSecret());
        assertNull(clientRes.getSecret().getValue());
        assertClient(client, storedClient);

        client.setPublicClient(false);
        newClient.setPublicClient(client.isPublicClient());
        client.setSecret("new-secret");
        newClient.setSecret(client.getSecret());

        clientRes.update(newClient);

        newClient.setSecret("**********"); // secrets are masked in events

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientResourcePath(client.getId()), newClient, ResourceType.CLIENT);

        storedClient = clientRes.toRepresentation();
        assertClient(client, storedClient);

        storedClient.setSecret(null);
        storedClient.getAttributes().put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, "");

        clientRes.update(storedClient);
        storedClient = clientRes.toRepresentation();

        assertFalse(storedClient.getAttributes().containsKey(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL));
        assertClient(client, storedClient);
    }

    @Test
    public void serviceAccount() {
        Response response = managedRealm.admin().clients().create(ClientConfigBuilder.create().clientId("serviceClient").serviceAccountsEnabled(true).build());
        String id = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.clients().get(id).remove());
        UserRepresentation userRep = managedRealm.admin().clients().get(id).getServiceAccountUser();
        MatcherAssert.assertThat("service-account-serviceclient", Matchers.equalTo(userRep.getUsername()));
        // KEYCLOAK-11197 service accounts are no longer created with a placeholder e-mail.
        assertNull(userRep.getEmail());
    }

    @Test
    public void pushRevocation() throws InterruptedException {
        testApp.kcAdmin().clear();

        ClientResource client = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app");
        String id = client.toRepresentation().getId();

        client.pushRevocation();

        PushNotBeforeAction pushNotBefore = testApp.kcAdmin().getAdminPushNotBefore();
        assertEquals(client.toRepresentation().getNotBefore().intValue(), pushNotBefore.getNotBefore());

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.clientPushRevocationPath(id), ResourceType.CLIENT);
    }

    @Test
    public void testAddNodeWithReservedCharacter() {
        testApp.kcAdmin().clear();
        String id = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app").toRepresentation().getId();
        assertThrows(BadRequestException.class,
                () -> managedRealm.admin().clients().get(id).registerNode(Collections.singletonMap("node", "foo#"))
        );
    }

    @Test
    public void nodes() throws MalformedURLException, InterruptedException {
        testApp.kcAdmin().clear();

        String id = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app").toRepresentation().getId();

        String myhost = new URL(managedRealm.getBaseUrl()).getHost();
        managedRealm.admin().clients().get(id).registerNode(Collections.singletonMap("node", myhost));
        managedRealm.admin().clients().get(id).registerNode(Collections.singletonMap("node", "invalid"));

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientNodePath(id, myhost), ResourceType.CLUSTER_NODE);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientNodePath(id, "invalid"), ResourceType.CLUSTER_NODE);

        GlobalRequestResult result = managedRealm.admin().clients().get(id).testNodesAvailable();
        assertEquals(1, result.getSuccessRequests().size());
        assertEquals(1, result.getFailedRequests().size());

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.clientTestNodesAvailablePath(id), result, ResourceType.CLUSTER_NODE);

        TestAvailabilityAction testAvailable = testApp.kcAdmin().getTestAvailable();
        assertEquals("test-app", testAvailable.getResource());

        assertEquals(2, managedRealm.admin().clients().get(id).toRepresentation().getRegisteredNodes().size());

        managedRealm.admin().clients().get(id).unregisterNode("invalid");

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.clientNodePath(id, "invalid"), ResourceType.CLUSTER_NODE);

        assertEquals(1, managedRealm.admin().clients().get(id).toRepresentation().getRegisteredNodes().size());
        managedRealm.admin().clients().get(id).unregisterNode(myhost);
    }

    @Test
    public void offlineUserSessions() {
        ClientRepresentation client = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app").toRepresentation();
        String id = client.getId();

        Response response = managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").password("password").email("testuser@localhost").name("test", "user").build());
        String userId = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.users().delete(userId).close());

        Map<String, Long> offlineSessionCount = managedRealm.admin().clients().get(id).getOfflineSessionCount();
        assertEquals(Long.valueOf(0), offlineSessionCount.get("count"));

        List<UserSessionRepresentation> userSessions = managedRealm.admin().users().get(userId).getOfflineSessions(id);
        assertEquals(0, userSessions.size(), "There should be no offline sessions");

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.doLogin("testuser", "password");
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        assertEquals(200, accessTokenResponse.getStatusCode());

        offlineSessionCount = managedRealm.admin().clients().get(id).getOfflineSessionCount();
        assertEquals(Long.valueOf(1), offlineSessionCount.get("count"));

        List<UserSessionRepresentation> offlineUserSessions = managedRealm.admin().clients().get(id).getOfflineUserSessions(0, 100);
        assertEquals(1, offlineUserSessions.size());
        assertEquals("testuser", offlineUserSessions.get(0).getUsername());
        org.hamcrest.MatcherAssert.assertThat(offlineUserSessions.get(0).getLastAccess(),
                allOf(greaterThan(Time.currentTimeMillis() - 10000L), lessThan(Time.currentTimeMillis())));

        userSessions = managedRealm.admin().users().get(userId).getOfflineSessions(id);
        assertEquals(1, userSessions.size(), "There should be one offline session");
        assertOfflineSession(offlineUserSessions.get(0), userSessions.get(0));
        //  reset for other tests
        oauth.scope(null);
    }

    private void assertOfflineSession(UserSessionRepresentation expected, UserSessionRepresentation actual) {
        assertEquals(expected.getId(), actual.getId(), "id");
        assertEquals(expected.getUserId(), actual.getUserId(), "userId");
        assertEquals(expected.getUsername(), actual.getUsername(), "userName");
        assertEquals(expected.getClients(), actual.getClients(), "clients");
    }

    @Test
    public void scopes() {
        Response response = managedRealm.admin().clients().create(ClientConfigBuilder.create().clientId("client").fullScopeEnabled(false).build());
        String id = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.clients().get(id).remove());
        adminEvents.skip();

        RoleMappingResource scopesResource = managedRealm.admin().clients().get(id).getScopeMappings();

        RoleRepresentation roleRep1 = createRealmRole("realm-composite");
        RoleRepresentation roleRep2 = createRealmRole("realm-child");

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath("realm-composite"), roleRep1, ResourceType.REALM_ROLE);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath("realm-child"), roleRep2, ResourceType.REALM_ROLE);

        roleRep1 = managedRealm.admin().roles().get("realm-composite").toRepresentation();
        roleRep2 = managedRealm.admin().roles().get("realm-child").toRepresentation();

        managedRealm.admin().roles().get("realm-composite").addComposites(List.of(roleRep2));

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourceCompositesPath("realm-composite"), List.of(roleRep2), ResourceType.REALM_ROLE);

        String accountMgmtId = managedRealm.admin().clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewAccountRoleRep = managedRealm.admin().clients().get(accountMgmtId).roles().get(AccountRoles.VIEW_PROFILE).toRepresentation();

        scopesResource.realmLevel().add(List.of(roleRep1));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientScopeMappingsRealmLevelPath(id), List.of(roleRep1), ResourceType.REALM_SCOPE_MAPPING);

        scopesResource.clientLevel(accountMgmtId).add(List.of(viewAccountRoleRep));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientScopeMappingsClientLevelPath(id, accountMgmtId), List.of(viewAccountRoleRep), ResourceType.CLIENT_SCOPE_MAPPING);

        Assert.assertNames(scopesResource.realmLevel().listAll(), "realm-composite");
        Assert.assertNames(scopesResource.realmLevel().listEffective(), "realm-composite", "realm-child");
        Assert.assertNames(scopesResource.realmLevel().listAvailable(), "realm-child", "offline_access",
                Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());

        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listAll(), AccountRoles.VIEW_PROFILE);
        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listEffective(), AccountRoles.VIEW_PROFILE);

        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listAvailable(), AccountRoles.MANAGE_ACCOUNT, AccountRoles.MANAGE_ACCOUNT_LINKS, AccountRoles.VIEW_APPLICATIONS, AccountRoles.VIEW_CONSENT, AccountRoles.MANAGE_CONSENT, AccountRoles.DELETE_ACCOUNT, AccountRoles.VIEW_GROUPS);

        Assert.assertNames(scopesResource.getAll().getRealmMappings(), "realm-composite");
        Assert.assertNames(scopesResource.getAll().getClientMappings().get(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).getMappings(), AccountRoles.VIEW_PROFILE);

        scopesResource.realmLevel().remove(List.of(roleRep1));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.clientScopeMappingsRealmLevelPath(id), List.of(roleRep1), ResourceType.REALM_SCOPE_MAPPING);

        scopesResource.clientLevel(accountMgmtId).remove(List.of(viewAccountRoleRep));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.clientScopeMappingsClientLevelPath(id, accountMgmtId), List.of(viewAccountRoleRep), ResourceType.CLIENT_SCOPE_MAPPING);

        Assert.assertNames(scopesResource.realmLevel().listAll());
        Assert.assertNames(scopesResource.realmLevel().listEffective());
        Assert.assertNames(scopesResource.realmLevel().listAvailable(), "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION, "realm-composite", "realm-child", Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());
        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listAll());

        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listAvailable(), AccountRoles.VIEW_PROFILE, AccountRoles.MANAGE_ACCOUNT, AccountRoles.MANAGE_ACCOUNT_LINKS, AccountRoles.VIEW_APPLICATIONS, AccountRoles.VIEW_CONSENT, AccountRoles.MANAGE_CONSENT, AccountRoles.DELETE_ACCOUNT, AccountRoles.VIEW_GROUPS);

        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listEffective());
    }

    /**
     * Test for KEYCLOAK-10603.
     */
    @Test
    public void rolesCanBeAddedToScopeEvenWhenTheyAreAlreadyIndirectlyAssigned() {
        Response response =
                managedRealm.admin().clients().create(ClientConfigBuilder.create().clientId("test-client").fullScopeEnabled(false).build());
        String testedClientUuid = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.clients().get(testedClientUuid).remove());

        createRealmRole("realm-composite");
        createRealmRole("realm-child");
        managedRealm.admin().roles().get("realm-composite")
                .addComposites(List.of(managedRealm.admin().roles().get("realm-child").toRepresentation()));

        response = managedRealm.admin().clients().create(ClientConfigBuilder.create().clientId("role-container-client").build());
        String roleContainerClientUuid = ApiUtil.getCreatedId(response);
        RolesResource roleContainerClientRolesRsc = managedRealm.admin().clients().get(roleContainerClientUuid).roles();
        managedRealm.cleanup().add(r -> r.clients().get(roleContainerClientUuid).remove());

        roleContainerClientRolesRsc.create(RoleConfigBuilder.create().name("client-composite").build());
        roleContainerClientRolesRsc.create(RoleConfigBuilder.create().name("client-child").build());
        roleContainerClientRolesRsc.get("client-composite").addComposites(List.of(
                roleContainerClientRolesRsc.get("client-child").toRepresentation()));

        // Make indirect assignments: assign composite roles
        RoleMappingResource scopesResource = managedRealm.admin().clients().get(testedClientUuid).getScopeMappings();
        scopesResource.realmLevel().add(
                List.of(managedRealm.admin().roles().get("realm-composite").toRepresentation()));
        scopesResource.clientLevel(roleContainerClientUuid).add(
                List.of(roleContainerClientRolesRsc.get("client-composite").toRepresentation()));

        // check state before making the direct assignments
        Assert.assertNames(scopesResource.realmLevel().listAll(), "realm-composite");
        Assert.assertNames(scopesResource.realmLevel().listAvailable(), "realm-child", "offline_access",
                Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());
        Assert.assertNames(scopesResource.realmLevel().listEffective(), "realm-composite", "realm-child");

        Assert.assertNames(scopesResource.clientLevel(roleContainerClientUuid).listAll(), "client-composite");
        Assert.assertNames(scopesResource.clientLevel(roleContainerClientUuid).listAvailable(), "client-child");
        Assert.assertNames(scopesResource.clientLevel(roleContainerClientUuid).listEffective(), "client-composite",
                "client-child");

        // Make direct assignments for roles which are already indirectly assigned
        scopesResource.realmLevel().add(List.of(managedRealm.admin().roles().get("realm-child").toRepresentation()));
        scopesResource.clientLevel(roleContainerClientUuid).add(
                List.of(roleContainerClientRolesRsc.get("client-child").toRepresentation()));

        // List realm roles
        Assert.assertNames(scopesResource.realmLevel().listAll(), "realm-composite", "realm-child");
        Assert.assertNames(scopesResource.realmLevel().listAvailable(), "offline_access",
                Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());
        Assert.assertNames(scopesResource.realmLevel().listEffective(), "realm-composite", "realm-child");

        // List client roles
        Assert.assertNames(scopesResource.clientLevel(roleContainerClientUuid).listAll(), "client-composite",
                "client-child");
        Assert.assertNames(scopesResource.clientLevel(roleContainerClientUuid).listAvailable());
        Assert.assertNames(scopesResource.clientLevel(roleContainerClientUuid).listEffective(), "client-composite",
                "client-child");
    }

    @Test
    public void scopesRoleRemoval() {
        // clientA to test scope mappings
        Response response = managedRealm.admin().clients().create(ClientConfigBuilder.create().clientId("clientA").fullScopeEnabled(false).build());
        String idA = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.clients().get(idA).remove());

        // clientB to create a client role for clientA
        response = managedRealm.admin().clients().create(ClientConfigBuilder.create().clientId("clientB").fullScopeEnabled(false).build());
        String idB = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.clients().get(idB).remove());

        adminEvents.skip(2);

        RoleMappingResource scopesResource = managedRealm.admin().clients().get(idA).getScopeMappings();

        // create a realm role and a role in clientB
        RoleRepresentation realmRoleRep = RoleConfigBuilder.create().name("realm-role").build();
        managedRealm.admin().roles().create(realmRoleRep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(realmRoleRep.getName()), realmRoleRep, ResourceType.REALM_ROLE);
        RoleRepresentation clientBRoleRep = RoleConfigBuilder.create().name("clientB-role").build();
        managedRealm.admin().clients().get(idB).roles().create(clientBRoleRep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(idB, clientBRoleRep.getName()), clientBRoleRep, ResourceType.CLIENT_ROLE);

        // assing to clientA both roles to the scope mappings
        realmRoleRep = managedRealm.admin().roles().get(realmRoleRep.getName()).toRepresentation();
        clientBRoleRep = managedRealm.admin().clients().get(idB).roles().get(clientBRoleRep.getName()).toRepresentation();
        scopesResource.realmLevel().add(List.of(realmRoleRep));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientScopeMappingsRealmLevelPath(idA), List.of(realmRoleRep), ResourceType.REALM_SCOPE_MAPPING);
        scopesResource.clientLevel(idB).add(List.of(clientBRoleRep));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientScopeMappingsClientLevelPath(idA, idB), List.of(clientBRoleRep), ResourceType.CLIENT_SCOPE_MAPPING);

        // assert the roles are there
        Assert.assertNames(scopesResource.realmLevel().listAll(), realmRoleRep.getName());
        Assert.assertNames(scopesResource.clientLevel(idB).listAll(), clientBRoleRep.getName());

        // delete realm role and check everything is refreshed ok
        managedRealm.admin().roles().deleteRole(realmRoleRep.getName());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.roleResourcePath(realmRoleRep.getName()), ResourceType.REALM_ROLE);
        Assert.assertNames(scopesResource.realmLevel().listAll());
        Assert.assertNames(scopesResource.clientLevel(idB).listAll(), clientBRoleRep.getName());

        // delete client role and check everything is refreshed ok
        managedRealm.admin().clients().get(idB).roles().deleteRole(clientBRoleRep.getName());
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.clientRoleResourcePath(idB, clientBRoleRep.getName()), ResourceType.CLIENT_ROLE);
        Assert.assertNames(scopesResource.realmLevel().listAll());
        Assert.assertNames(scopesResource.clientLevel(idB).listAll());
    }

    public void protocolMappersTest(String clientDbId, ProtocolMappersResource mappersResource) {
        // assert default mappers found
        List<ProtocolMapperRepresentation> protocolMappers = mappersResource.getMappers();

        String emailMapperId = null;
        String usernameMapperId = null;
        String fooMapperId = null;
        for (ProtocolMapperRepresentation mapper : protocolMappers) {
            if (mapper.getName().equals(OIDCLoginProtocolFactory.EMAIL)) {
                emailMapperId = mapper.getId();
            } else if (mapper.getName().equals(OIDCLoginProtocolFactory.USERNAME)) {
                usernameMapperId = mapper.getId();
            } else if (mapper.getName().equals("foo")) {
                fooMapperId = mapper.getId();
            }
        }

        // Builtin mappers are not here
        assertNull(emailMapperId);
        assertNull(usernameMapperId);

        assertNull(fooMapperId);

        // Create foo mapper
        ProtocolMapperRepresentation fooMapper = new ProtocolMapperRepresentation();
        fooMapper.setName("foo");
        fooMapper.setProtocol("openid-connect");
        fooMapper.setProtocolMapper("oidc-hardcoded-claim-mapper");
        Response response = mappersResource.createMapper(fooMapper);
        String location = response.getLocation().toString();
        fooMapperId = location.substring(location.lastIndexOf("/") + 1);
        response.close();

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(clientDbId, fooMapperId), fooMapper, ResourceType.PROTOCOL_MAPPER);

        fooMapper = mappersResource.getMapperById(fooMapperId);
        assertEquals("foo", fooMapper.getName());

        // Update foo mapper
        mappersResource.update(fooMapperId, fooMapper);

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientProtocolMapperPath(clientDbId, fooMapperId), fooMapper, ResourceType.PROTOCOL_MAPPER);

        fooMapper = mappersResource.getMapperById(fooMapperId);

        // Remove foo mapper
        mappersResource.delete(fooMapperId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.clientProtocolMapperPath(clientDbId, fooMapperId), ResourceType.PROTOCOL_MAPPER);
        String finalFooMapperId = fooMapperId;
        assertThrows(NotFoundException.class, () -> mappersResource.getMapperById(finalFooMapperId), "Not expected to find deleted mapper");
    }

    @Test
    public void updateClientWithProtocolMapper() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("my-app");

        ProtocolMapperRepresentation fooMapper = new ProtocolMapperRepresentation();
        fooMapper.setName("foo");
        fooMapper.setProtocol("openid-connect");
        fooMapper.setProtocolMapper("oidc-hardcoded-claim-mapper");
        rep.setProtocolMappers(Collections.singletonList(fooMapper));

        Response response = managedRealm.admin().clients().create(rep);
        String id = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.clients().get(id).remove());

        ClientResource clientResource = managedRealm.admin().clients().get(id);
        assertNotNull(clientResource);
        ClientRepresentation client = clientResource.toRepresentation();
        List<ProtocolMapperRepresentation> protocolMappers = client.getProtocolMappers();
        assertEquals(1, protocolMappers.size());
        ProtocolMapperRepresentation mapper = protocolMappers.get(0);
        assertEquals("foo", mapper.getName());

        ClientRepresentation newClient = new ClientRepresentation();
        newClient.setId(client.getId());
        newClient.setClientId(client.getClientId());

        ProtocolMapperRepresentation barMapper = new ProtocolMapperRepresentation();
        barMapper.setName("bar");
        barMapper.setProtocol("openid-connect");
        barMapper.setProtocolMapper("oidc-hardcoded-role-mapper");
        protocolMappers.add(barMapper);
        newClient.setProtocolMappers(protocolMappers);

        managedRealm.admin().clients().get(client.getId()).update(newClient);

        ClientRepresentation storedClient = managedRealm.admin().clients().get(client.getId()).toRepresentation();
        assertClient(client, storedClient);
    }

    public static void assertClient(ClientRepresentation client, ClientRepresentation storedClient) {
        if (client.getClientId() != null) Assert.assertEquals(client.getClientId(), storedClient.getClientId());
        if (client.getName() != null) Assert.assertEquals(client.getName(), storedClient.getName());
        if (client.isEnabled() != null) Assert.assertEquals(client.isEnabled(), storedClient.isEnabled());
        if (client.isAlwaysDisplayInConsole() != null)
            Assert.assertEquals(client.isAlwaysDisplayInConsole(), storedClient.isAlwaysDisplayInConsole());
        if (client.isBearerOnly() != null) Assert.assertEquals(client.isBearerOnly(), storedClient.isBearerOnly());
        if (client.isPublicClient() != null)
            Assert.assertEquals(client.isPublicClient(), storedClient.isPublicClient());
        if (client.isFullScopeAllowed() != null)
            Assert.assertEquals(client.isFullScopeAllowed(), storedClient.isFullScopeAllowed());
        if (client.getRootUrl() != null) Assert.assertEquals(client.getRootUrl(), storedClient.getRootUrl());
        if (client.getAdminUrl() != null) Assert.assertEquals(client.getAdminUrl(), storedClient.getAdminUrl());
        if (client.getBaseUrl() != null) Assert.assertEquals(client.getBaseUrl(), storedClient.getBaseUrl());
        if (client.isSurrogateAuthRequired() != null)
            Assert.assertEquals(client.isSurrogateAuthRequired(), storedClient.isSurrogateAuthRequired());
        if (client.getClientAuthenticatorType() != null)
            Assert.assertEquals(client.getClientAuthenticatorType(), storedClient.getClientAuthenticatorType());
        if (client.getSecret() != null) Assert.assertEquals(client.getSecret(), storedClient.getSecret());

        if (client.getNotBefore() != null) {
            Assertions.assertEquals(client.getNotBefore(), storedClient.getNotBefore());
        }
        if (client.getDefaultRoles() != null) {
            Set<String> set = Set.of(client.getDefaultRoles());
            Set<String> storedSet = Set.of(storedClient.getDefaultRoles());

            Assertions.assertEquals(set, storedSet);
        }

        List<String> redirectUris = client.getRedirectUris();
        if (redirectUris != null) {
            Set<String> set = new HashSet<>(client.getRedirectUris());
            Set<String> storedSet = new HashSet<>(storedClient.getRedirectUris());

            Assertions.assertEquals(set, storedSet);
        }

        List<String> webOrigins = client.getWebOrigins();
        if (webOrigins != null) {
            Set<String> set = new HashSet<>(client.getWebOrigins());
            Set<String> storedSet = new HashSet<>(storedClient.getWebOrigins());

            Assertions.assertEquals(set, storedSet);
        }

        List<ProtocolMapperRepresentation> protocolMappers = client.getProtocolMappers();
        if (protocolMappers != null) {
            Set<String> set = protocolMappers.stream()
                    .map(ProtocolMapperRepresentation::getName)
                    .collect(Collectors.toSet());
            Set<String> storedSet = storedClient.getProtocolMappers().stream()
                    .map(ProtocolMapperRepresentation::getName)
                    .collect(Collectors.toSet());

            Assertions.assertEquals(set, storedSet);
        }
    }

    private RoleRepresentation createRealmRole(String roleName) {
        RoleRepresentation role = RoleConfigBuilder.create().name(roleName).build();
        managedRealm.admin().roles().create(role);

        String createdId = managedRealm.admin().roles().get(role.getName()).toRepresentation().getId();
        managedRealm.cleanup().add(r -> r.rolesById().deleteRole(createdId));

        return role;
    }

    private static class ClientTestRealmConfig implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {

            realm.addUser("test-user@localhost")
                    .enabled(true)
                    .email("test-user@localhost")
                    .name("Tom", "Brady")
                    .password("password");
            return realm;
        }
    }
}
