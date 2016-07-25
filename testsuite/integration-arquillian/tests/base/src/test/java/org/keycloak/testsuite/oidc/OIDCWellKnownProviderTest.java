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

package org.keycloak.testsuite.oidc;

import java.net.URI;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCWellKnownProviderTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
        /*
         * Configure the default client ID. Seems like OAuthClient is keeping the state of clientID
         * For example: If some test case configure oauth.clientId("sample-public-client"), other tests
         * will faile and the clientID will always be "sample-public-client
         * @see AccessTokenTest#testAuthorizationNegotiateHeaderIgnored()
         */
        oauth.clientId("test-app");
    }


    @Test
    public void testDiscovery() {
        Client client = ClientBuilder.newClient();
        try {
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryConfiguration(client);

            // URIs are filled
            Assert.assertEquals(oidcConfig.getAuthorizationEndpoint(), OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString());
            Assert.assertEquals(oidcConfig.getTokenEndpoint(), oauth.getAccessTokenUrl());
            Assert.assertEquals(oidcConfig.getUserinfoEndpoint(), OIDCLoginProtocolService.userInfoUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString());
            Assert.assertEquals(oidcConfig.getJwksUri(), oauth.getCertsUrl("test"));

            // Support standard + implicit + hybrid flow
            assertContains(oidcConfig.getResponseTypesSupported(), OAuth2Constants.CODE, OIDCResponseType.ID_TOKEN, "id_token token", "code id_token", "code token", "code id_token token");
            assertContains(oidcConfig.getGrantTypesSupported(), OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.IMPLICIT);
            assertContains(oidcConfig.getResponseModesSupported(), "query", "fragment");

            Assert.assertNames(oidcConfig.getSubjectTypesSupported(), "public");
            Assert.assertNames(oidcConfig.getIdTokenSigningAlgValuesSupported(), Algorithm.RS256.toString());

            // Client authentication
            Assert.assertNames(oidcConfig.getTokenEndpointAuthMethodsSupported(), "client_secret_basic", "client_secret_post", "private_key_jwt");
            Assert.assertNames(oidcConfig.getTokenEndpointAuthSigningAlgValuesSupported(), Algorithm.RS256.toString());

            // Claims
            assertContains(oidcConfig.getClaimsSupported(), IDToken.NAME, IDToken.EMAIL, IDToken.PREFERRED_USERNAME, IDToken.FAMILY_NAME);
            Assert.assertNames(oidcConfig.getClaimTypesSupported(), "normal");
            Assert.assertFalse(oidcConfig.getClaimsParameterSupported());

            // Scopes supported
            Assert.assertNames(oidcConfig.getScopesSupported(), OAuth2Constants.SCOPE_OPENID, OAuth2Constants.OFFLINE_ACCESS);

            // Request and Request_Uri
            Assert.assertFalse(oidcConfig.getRequestParameterSupported());
            Assert.assertFalse(oidcConfig.getRequestUriParameterSupported());
        } finally {
            client.close();
        }
    }

    @Test
    public void testIssuerMatches() throws Exception {
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
        Assert.assertEquals(200, response.getStatusCode());
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        Client client = ClientBuilder.newClient();
        try {
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryConfiguration(client);

            // assert issuer matches
            Assert.assertEquals(idToken.getIssuer(), oidcConfig.getIssuer());
        } finally {
            client.close();
        }
    }

    private OIDCConfigurationRepresentation getOIDCDiscoveryConfiguration(Client client) {
        UriBuilder builder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
        URI oidcDiscoveryUri = RealmsResource.wellKnownProviderUrl(builder).build("test", OIDCWellKnownProviderFactory.PROVIDER_ID);
        WebTarget oidcDiscoveryTarget = client.target(oidcDiscoveryUri);

        Response response = oidcDiscoveryTarget.request().get();
        return response.readEntity(OIDCConfigurationRepresentation.class);
    }

    private void assertContains(List<String> actual, String... expected) {
        for (String exp : expected) {
            Assert.assertTrue(actual.contains(exp));
        }
    }
}
