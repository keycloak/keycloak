/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.client;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.util.JsonSerialization;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.AUTHORIZATION_CODE;
import static org.keycloak.OAuth2Constants.CLIENT_CREDENTIALS;
import static org.keycloak.OAuth2Constants.DEVICE_CODE_GRANT_TYPE;
import static org.keycloak.OAuth2Constants.IMPLICIT;
import static org.keycloak.OAuth2Constants.PASSWORD;
import static org.keycloak.OAuth2Constants.REFRESH_TOKEN;
import static org.keycloak.OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE;
import static org.keycloak.models.OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED;
import static org.keycloak.protocol.oidc.utils.OIDCResponseType.CODE;
import static org.keycloak.protocol.oidc.utils.OIDCResponseType.ID_TOKEN;
import static org.keycloak.protocol.oidc.utils.OIDCResponseType.NONE;

import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Test of OIDC client registration with various combinations of parameters "response_types" and "grant_types"
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCClientRegistrationResponseTypesAndGrantsTest extends AbstractClientRegistrationTest {

    @Before
    public void before() throws Exception {
        super.before();

        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    private OIDCClientRepresentation createRep(List<String> responseTypes, List<String> grantTypes) {
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName("RegistrationAccessTokenTest");
        client.setClientUri("http://root");
        client.setRedirectUris(Collections.singletonList("http://redirect"));
        client.setResponseTypes(responseTypes);
        client.setGrantTypes(grantTypes);
        return client;
    }

    // OIDC mentions "code" as default response_type if ommitted. Type "none" used as well for backwards compatibility and lack of dedicated config.
    // Refresh token enabled as well for backwards compatibility.
    @Test
    public void testClientWithoutResponseTypesAndGrantTypes() throws Exception {
        OIDCClientRepresentation clientRep = createRep(null, null);

        OIDCClientRepresentation response = reg.oidc().create(clientRep);

        assertOIDCResponse(response, List.of(CODE, NONE), List.of(AUTHORIZATION_CODE, REFRESH_TOKEN));

        assertKeycloakClient(response, true, false, false, false, true, false, false);
    }

    @Test
    public void testResponseTypeCodeWithoutGrantTypes() throws Exception {
        OIDCClientRepresentation clientRep = createRep(List.of(CODE), null);

        OIDCClientRepresentation response = reg.oidc().create(clientRep);

        assertOIDCResponse(response, List.of(CODE, NONE), List.of(AUTHORIZATION_CODE, REFRESH_TOKEN));

        assertKeycloakClient(response, true, false, false, false, true, false, false);
    }

    // Limitation of Keycloak switches (Standard flow, Implicit flow) means that enabling any hybrid grant type enables also implicit flow.
    // This is also backwards compatibile behaviour
    @Test
    public void testResponseTypeCodeIDTokenWithoutGrantTypes() throws Exception {
        OIDCClientRepresentation clientRep = createRep(List.of(CODE, ID_TOKEN), null);

        OIDCClientRepresentation response = reg.oidc().create(clientRep);

        assertOIDCResponse(response, List.of(CODE, NONE, ID_TOKEN, "id_token token", "code id_token", "code token", "code id_token token"),
                List.of(AUTHORIZATION_CODE, IMPLICIT, REFRESH_TOKEN));

        assertKeycloakClient(response, true, true, false, false, true, false, false);
    }

    @Test
    public void testWithoutResponseTypeClientCredentialsGrant() throws Exception {
        OIDCClientRepresentation clientRep = createRep(null, List.of(CLIENT_CREDENTIALS));

        OIDCClientRepresentation response = reg.oidc().create(clientRep);

        assertOIDCResponse(response, Collections.emptyList(),
                List.of(CLIENT_CREDENTIALS));

        assertKeycloakClient(response, false, false, false, true, false, false, false);
    }

    @Test
    public void testWithoutResponseTypePasswordGrant() throws Exception {
        OIDCClientRepresentation clientRep = createRep(null, List.of(PASSWORD));

        OIDCClientRepresentation response = reg.oidc().create(clientRep);

        assertOIDCResponse(response, Collections.emptyList(),
                List.of(PASSWORD));

        assertKeycloakClient(response, false, false, true, false, false, false, false);
    }

    @Test
    public void testWithoutResponseTypePasswordClientCredentialsRefreshTokensGrants() throws Exception {
        OIDCClientRepresentation clientRep = createRep(null, List.of(PASSWORD, CLIENT_CREDENTIALS, REFRESH_TOKEN));

        OIDCClientRepresentation response = reg.oidc().create(clientRep);

        assertOIDCResponse(response, Collections.emptyList(),
                List.of(PASSWORD, CLIENT_CREDENTIALS, REFRESH_TOKEN));

        assertKeycloakClient(response, false, false, true, true, true, false, false);
    }

    @Test
    public void testClientWithoutRefreshToken() throws Exception {
        OIDCClientRepresentation clientRep = createRep(null, List.of(AUTHORIZATION_CODE));

        OIDCClientRepresentation response = reg.oidc().create(clientRep);

        assertOIDCResponse(response, List.of(CODE, NONE), List.of(AUTHORIZATION_CODE));

        assertKeycloakClient(response, true, false, false, false, false, false, false);
    }

    @Test
    public void testClientWithRefreshToken() throws Exception {
        OIDCClientRepresentation clientRep = createRep(null, List.of(AUTHORIZATION_CODE, REFRESH_TOKEN));

        OIDCClientRepresentation response = reg.oidc().create(clientRep);

        assertOIDCResponse(response, List.of(CODE, NONE), List.of(AUTHORIZATION_CODE, REFRESH_TOKEN));

        assertKeycloakClient(response, true, false, false, false, true, false, false);
    }

    @Test
    public void testNoResponseTypesWithDeviceGrant() throws Exception {
        OIDCClientRepresentation clientRep = createRep(null, List.of(DEVICE_CODE_GRANT_TYPE));

        OIDCClientRepresentation response = reg.oidc().create(clientRep);

        assertOIDCResponse(response, Collections.emptyList(), List.of(DEVICE_CODE_GRANT_TYPE));

        assertKeycloakClient(response, false, false, false, false, false, true, false);
    }

    @Test
    public void testGrantTypeTokenExchange() throws Exception {
        OIDCClientRepresentation clientRep = createRep(null, List.of(AUTHORIZATION_CODE, REFRESH_TOKEN, TOKEN_EXCHANGE_GRANT_TYPE));
        clientRep.setTokenEndpointAuthMethod("none");

        ClientRegistrationException clientRegistrationException = Assert.assertThrows(ClientRegistrationException.class, () -> reg.oidc().create(clientRep));
        MatcherAssert.assertThat(clientRegistrationException.getCause(), Matchers.instanceOf(HttpErrorException.class));
        HttpErrorException httpErrorException = (HttpErrorException) clientRegistrationException.getCause();
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), httpErrorException.getStatusLine().getStatusCode());
        OAuth2ErrorRepresentation error = JsonSerialization.readValue(httpErrorException.getErrorResponse(), OAuth2ErrorRepresentation.class);
        Assert.assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, error.getError());

        clientRep.setTokenEndpointAuthMethod(null);
        OIDCClientRepresentation response = reg.oidc().create(clientRep);

        assertOIDCResponse(response, List.of(CODE, NONE), List.of(AUTHORIZATION_CODE, REFRESH_TOKEN, TOKEN_EXCHANGE_GRANT_TYPE));

        assertKeycloakClient(response, true, false, false, false, true, false, true);
    }

    @Test
    public void testCodeResponseTypeWithMoreGrants() throws Exception {
        OIDCClientRepresentation clientRep = createRep(List.of(CODE), List.of(AUTHORIZATION_CODE, REFRESH_TOKEN, PASSWORD, CLIENT_CREDENTIALS));

        OIDCClientRepresentation response = reg.oidc().create(clientRep);

        assertOIDCResponse(response, List.of(CODE, NONE), List.of(AUTHORIZATION_CODE, REFRESH_TOKEN, PASSWORD, CLIENT_CREDENTIALS));

        assertKeycloakClient(response, true, false, true, true, true, false, false);
    }

    // Grant type "authorization_code" added automatically because of response_type "code" .
    // If provided response_types are not 100% aligned with provided grant_types, we can in theory reject the request based on https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata
    // and the note "The following table lists the correspondence between response_type values that the Client will use and grant_type values that MUST be included in the registered grant_types list"
    // Not doing it for backwards compatibility for now
    @Test
    public void testCodeResponseTypeWithIncompatibleGrants() throws Exception {
        OIDCClientRepresentation clientRep = createRep(List.of(CODE), List.of(REFRESH_TOKEN, PASSWORD, CLIENT_CREDENTIALS));

        OIDCClientRepresentation response = reg.oidc().create(clientRep);

        assertOIDCResponse(response, List.of(CODE, NONE), List.of(AUTHORIZATION_CODE, REFRESH_TOKEN, PASSWORD, CLIENT_CREDENTIALS));

        assertKeycloakClient(response, true, false, true, true, true, false, false);
    }

    private void assertOIDCResponse(OIDCClientRepresentation response, List<String> expectedResponseTypes, List<String> expectedGrantTypes) {
        assertOIDCResponseImpl(response, expectedResponseTypes, expectedGrantTypes);

        // Test subsequent "get" request returns the same as response from initial "create" request
        try {
            reg.auth(Auth.token(response));
            OIDCClientRepresentation rep = reg.oidc().get(response.getClientId());
            assertOIDCResponseImpl(rep, expectedResponseTypes, expectedGrantTypes);
        } catch (ClientRegistrationException cre) {
            throw new AssertionError(cre);
        }
    }

    private void assertOIDCResponseImpl(OIDCClientRepresentation response, List<String> expectedResponseTypes, List<String> expectedGrantTypes) {
        MatcherAssert.assertThat("Incompatible response_types", response.getResponseTypes(), containsInAnyOrder(expectedResponseTypes.toArray()));
        MatcherAssert.assertThat("Incompatible grant_Types", response.getGrantTypes(), containsInAnyOrder(expectedGrantTypes.toArray()));
    }

    private void assertKeycloakClient(OIDCClientRepresentation response,
                                             boolean expectedStandardFlow,
                                             boolean expectedImplicitFlow,
                                             boolean expectedDirectGrantFlow,
                                             boolean expectedServiceAccountsFlow,
                                             boolean expectedRefreshToken,
                                             boolean expectedDeviceGrant,
                                             boolean expectedTokenExchange) {
        ClientRepresentation kcClient = getClient(response.getClientId());
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
        Assert.assertEquals("Expected standard flow: " + expectedStandardFlow + " did not match.", expectedStandardFlow, kcClient.isStandardFlowEnabled());
        Assert.assertEquals("Expected implicit flow: " + expectedImplicitFlow + " did not match.", expectedImplicitFlow, kcClient.isImplicitFlowEnabled());
        Assert.assertEquals("Expected direct grant flow: " + expectedDirectGrantFlow + " did not match.", expectedDirectGrantFlow, kcClient.isDirectAccessGrantsEnabled());
        Assert.assertEquals("Expected service accounts flow: " + expectedServiceAccountsFlow + " did not match.", expectedServiceAccountsFlow, kcClient.isServiceAccountsEnabled());
        Assert.assertEquals("Expected refresh: " + expectedRefreshToken + " did not match.", expectedRefreshToken, config.isUseRefreshToken());
        Assert.assertFalse("Don't expect refresh token for client credentials grant enabled", config.isUseRefreshTokenForClientCredentialsGrant());
        boolean deviceEnabled = kcClient.getAttributes() != null && Boolean.parseBoolean(kcClient.getAttributes().get(OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED));
        Assert.assertEquals("Expected device: " + expectedDeviceGrant + " did not match.", expectedDeviceGrant, deviceEnabled);
        Assert.assertEquals("Expected Token Exchange: " + expectedTokenExchange + " did not match.", expectedTokenExchange, config.isStandardTokenExchangeEnabled());
    }
}
