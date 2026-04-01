/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.securityprofile;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.Assert;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = LaxSecurityProfileTest.LaxSecurityProfileServerConfig.class)
public class LaxSecurityProfileTest {

    @InjectAdminClientFactory(lifecycle = LifeCycle.CLASS)
    private AdminClientFactory adminClientFactory;

    private RealmResource masterRealm;

    @BeforeEach
    public void setUp() {

        Keycloak keycloak = adminClientFactory.create()
                .realm("master")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .username("admin")
                .password("admin")
                .grantType(OAuth2Constants.PASSWORD)
                .autoClose()
                .build();
        masterRealm = keycloak.realm("master");
    }

    @Test
    public void testGlobalClientPolicies() {
        RealmResource realm = masterRealm;

        // test there are policies defined in the endpoint
        ClientPoliciesRepresentation policies = realm.clientPoliciesPoliciesResource().getPolicies(true);
        Assertions.assertNotNull(policies.getGlobalPolicies());
        MatcherAssert.assertThat(policies.getGlobalPolicies().stream().map(ClientPolicyRepresentation::getName).collect(Collectors.toList()),
                Matchers.containsInAnyOrder("Openid-connect OAuth 2.1 confidential client",
                        "Saml secure client (signatures, post, https)"));
        // try creating a global policy fails
        ClientPoliciesRepresentation policiesRep = new ClientPoliciesRepresentation();
        policiesRep.setPolicies(Collections.singletonList(policies.getGlobalPolicies().iterator().next()));
        BadRequestException e = Assert.assertThrows(BadRequestException.class,
                () -> realm.clientPoliciesPoliciesResource().updatePolicies(policiesRep));
        ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
        MatcherAssert.assertThat(error.getErrorMessage(), Matchers.containsString("duplicated as a global policy"));
    }

    @Test
    public void testUpdatingGlobalPoliciesNotAllowed() throws Exception {
        ClientPoliciesRepresentation clientPoliciesRep = getClientPolicies();
        List<ClientPolicyRepresentation> origGlobalPolicies = clientPoliciesRep.getGlobalPolicies();

        // Attempt to update description of some global policy. Should fail
        clientPoliciesRep = getClientPolicies();
        clientPoliciesRep.getGlobalPolicies().stream()
                .filter(clientPolicy -> "Saml secure client (signatures, post, https)".equals(clientPolicy.getName()))
                .forEach(clientPolicy -> clientPolicy.setDescription("some new description"));
        try {
            updatePolicies(clientPoliciesRep);
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals("update policies failed", cpe.getError());
        }

        // Attempt to add new global policy. Should fail
        clientPoliciesRep = getClientPolicies();
        ClientPolicyRepresentation newPolicy = new ClientPolicyRepresentation();
        newPolicy.setName("new-name");
        newPolicy.setDescription("desc");
        clientPoliciesRep.getGlobalPolicies().add(newPolicy);
        try {
            updatePolicies(clientPoliciesRep);
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals("update policies failed", cpe.getError());
        }

        // Attempt to update without global policies. Should be OK
        clientPoliciesRep = getClientPolicies();
        clientPoliciesRep.setGlobalPolicies(null);
        updatePolicies(clientPoliciesRep);

        // Attempt to update with global policies, but not change them. Should be OK
        clientPoliciesRep = getClientPolicies();
        updatePolicies(clientPoliciesRep);

        // Doublecheck global policies were not changed
        clientPoliciesRep = getClientPolicies();
        Assert.assertEquals(origGlobalPolicies, clientPoliciesRep.getGlobalPolicies());
    }

    @Test
    public void testCreateConfidentialOpenIdConnectClientSecure() {
        RealmResource realm = masterRealm;
        ClientRepresentation clientRep = ClientConfigBuilder.create()
                .name("test-client-policy-app")
                .clientId("test-client-policy-app")
                .protocol("openid-connect")
                .baseUrl("https://www.keycloak.org")
                .redirectUris("https://www.keycloak.org")
                .directAccessGrantsEnabled(true)
                .build();
        clientRep.setImplicitFlowEnabled(true);
        OIDCAdvancedConfigWrapper wrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
        wrapper.setPostLogoutRedirectUris(Collections.singletonList("https://www.keycloak.org"));
        wrapper.setPkceCodeChallengeMethod(OIDCLoginProtocol.PKCE_METHOD_PLAIN);
        wrapper.setUseMtlsHoKToken(false);

        // set the redirect uri to unsecure http
        clientRep.setRedirectUris(Collections.singletonList("http://www.keycloak.org"));
        Response resp = realm.clients().create(clientRep);
        Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        OAuth2ErrorRepresentation error = resp.readEntity(OAuth2ErrorRepresentation.class);
        Assertions.assertEquals(OAuthErrorException.INVALID_REQUEST, error.getError());
        Assertions.assertEquals("Invalid Redirect Uri: invalid uri", error.getErrorDescription());
        clientRep.setRedirectUris(Collections.singletonList("https://www.keycloak.org"));

        // create OK
        resp = realm.clients().create(clientRep);
        Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
        String id = ApiUtil.getCreatedId(resp);

        try {
            // check everything is auto-configure for security as the policy has auto-configure
            clientRep = realm.clients().get(id).toRepresentation();
            wrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            Assertions.assertEquals(OIDCLoginProtocol.PKCE_METHOD_S256, wrapper.getPkceCodeChallengeMethod());
            Assertions.assertTrue(wrapper.isUseMtlsHokToken());
            Assertions.assertFalse(clientRep.isImplicitFlowEnabled());
            Assertions.assertFalse(clientRep.isDirectAccessGrantsEnabled());
        } finally {
            masterRealm.clients().get(id).remove();
        }
    }

    @Test
    public void testCreateSamlClientSecure() {
        RealmResource realm = masterRealm;
        // setup a perfect client for SAML secure
        ClientRepresentation clientRep = ClientConfigBuilder.create()
                .name("test-client-policy-app")
                .clientId("test-client-policy-app")
                .protocol("saml")
                .baseUrl("https://www.keycloak.org")
                .redirectUris("https://www.keycloak.org")
                .adminUrl("https://www.keycloak.org")
                .attribute(SamlConfigAttributes.SAML_FORCE_POST_BINDING, "true")
                .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "true")
                .attribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true")
                .build();

        // change base url to unsecure http
        clientRep.setBaseUrl("http://www.keycloak.org");
        Response resp = realm.clients().create(clientRep);
        Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        OAuth2ErrorRepresentation error = resp.readEntity(OAuth2ErrorRepresentation.class);
        Assertions.assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, error.getError());
        MatcherAssert.assertThat(error.getErrorDescription(), Matchers.startsWith("Non secure scheme for"));
        clientRep.setBaseUrl("https://www.keycloak.org");

        // change force post to false
        clientRep.getAttributes().put(SamlConfigAttributes.SAML_FORCE_POST_BINDING, "false");
        resp = realm.clients().create(clientRep);
        Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        error = resp.readEntity(OAuth2ErrorRepresentation.class);
        Assertions.assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, error.getError());
        Assertions.assertEquals("Force POST binding is not enabled", error.getErrorDescription());
        clientRep.getAttributes().put(SamlConfigAttributes.SAML_FORCE_POST_BINDING, "true");

        // remove client signature
        clientRep.getAttributes().put(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false");
        resp = realm.clients().create(clientRep);
        Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        error = resp.readEntity(OAuth2ErrorRepresentation.class);
        Assertions.assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, error.getError());
        Assert.assertEquals("Signatures not ensured for the client. Ensure Client signature required and Sign documents or Sign assertions are ON",
                error.getErrorDescription());
        clientRep.getAttributes().put(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "true");

        // create OK
        Response finalResp = realm.clients().create(clientRep);
        Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), finalResp.getStatus());
        String createdId = ApiUtil.getCreatedId(finalResp);
        masterRealm.clients().get(createdId).remove();
    }

    private ClientPoliciesRepresentation getClientPolicies() {
        return masterRealm.clientPoliciesPoliciesResource().getPolicies(true);
    }

    protected void updatePolicies(ClientPoliciesRepresentation rep) throws ClientPolicyException {
        try {
            masterRealm.clientPoliciesPoliciesResource().updatePolicies(rep);
        } catch (BadRequestException e) {
            throw new ClientPolicyException("update policies failed", e.getResponse().getStatusInfo().toString());
        }
    }

    // Applying global policies
    public static class LaxSecurityProfileServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config
                .spiOption("security-profile", "default", "name", "lax-security-profile");
        }
    }

}
