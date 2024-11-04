/*
 * Copyright 2024 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.testsuite.admin.client;

import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testsuite.util.OAuthClient;

/**
 *
 * @author rmartinc
 */
public class ServiceAccountClientTest extends AbstractClientTest {


    @Test
    public void testServiceAccountEnableDisable() throws Exception {
        // Create a client with service account enabled
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("service-account-client");
        clientRep.setProtocol("openid-connect");
        clientRep.setSecret("password");
        clientRep.setServiceAccountsEnabled(Boolean.TRUE);
        clientRep.setClientAuthenticatorType("client-secret");
        clientRep.setPublicClient(Boolean.FALSE);
        String clientUuid = createClient(clientRep);
        ClientResource client = testRealmResource().clients().get(clientUuid);
        getCleanup().addClientUuid(clientUuid);
        MatcherAssert.assertThat(client.getDefaultClientScopes().stream().map(ClientScopeRepresentation::getName).collect(Collectors.toList()),
                Matchers.hasItem("service_account"));

        // perform a login and check the claims are there
        oauth.clientId("service-account-client");
        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("password");
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        org.junit.Assert.assertEquals("service-account-client", accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID));
        org.junit.Assert.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_HOST));
        org.junit.Assert.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ADDRESS));

        // update the client to remove service account
        clientRep.setServiceAccountsEnabled(Boolean.FALSE);
        client.update(clientRep);
        MatcherAssert.assertThat(client.getDefaultClientScopes().stream().map(ClientScopeRepresentation::getName).collect(Collectors.toList()),
                Matchers.not(Matchers.hasItem("service_account")));
        response = oauth.doClientCredentialsGrantAccessTokenRequest("password");
        org.junit.Assert.assertEquals("unauthorized_client", response.getError());

        // re-enable sevice accounts
        clientRep.setServiceAccountsEnabled(Boolean.TRUE);
        client.update(clientRep);
        MatcherAssert.assertThat(client.getDefaultClientScopes().stream().map(ClientScopeRepresentation::getName).collect(Collectors.toList()),
                Matchers.hasItem("service_account"));
        response = oauth.doClientCredentialsGrantAccessTokenRequest("password");
        accessToken = oauth.verifyToken(response.getAccessToken());
        org.junit.Assert.assertEquals("service-account-client", accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID));
        org.junit.Assert.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_HOST));
        org.junit.Assert.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ADDRESS));
    }
}
