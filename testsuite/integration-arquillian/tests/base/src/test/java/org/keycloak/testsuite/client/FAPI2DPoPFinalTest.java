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
 *
 */
package org.keycloak.testsuite.client;

import org.junit.Test;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.crypto.Algorithm;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.ParResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FAPI2DPoPFinalTest extends FAPI2DPoPTest {

    protected String getSecurityProfileName() {
        return FAPI2_DPOP_SECURITY_PROFILE_FINAL_NAME;
    }

    @Test
    public void testSecureClientAuthenticationAssertion() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(getSecurityProfileName());

        // Register client with private-key-jwt
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
        });

        oauth.client(clientId);

        PkceGenerator pkceGenerator = PkceGenerator.s256();

        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setNonce("123456");
        requestObject.setCodeChallenge(pkceGenerator.getCodeChallenge());
        requestObject.setCodeChallengeMethod(pkceGenerator.getCodeChallengeMethod());
        registerRequestObject(requestObject, clientId, Algorithm.PS256, false);

        // Send a push authorization request with invalid 'aud' . Should fail
        String signedJwt = createSignedRequestToken(clientId, Algorithm.PS256, getRealmInfoUrl() + "/protocol/openid-connect/ext/par/request");
        ParResponse pResp = oauth.pushedAuthorizationRequest().request(request).signedJwt(signedJwt).send();
        assertEquals(400, pResp.getStatusCode());

        // Send a push authorization request with valid 'aud' . Should succeed
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256, getRealmInfoUrl());
        pResp = oauth.pushedAuthorizationRequest().request(request).signedJwt(signedJwt).send();
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();
        request = null;

        // Send an authorization request . Should succeed
        String code = loginUserAndGetCode(clientId, null, false);
        assertNotNull(code);

        // Send a token request with invalid 'aud' . Should fail
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256, getRealmInfoUrl() + "/protocol/openid-connect/token");
        this.pkceGenerator = pkceGenerator;
        AccessTokenResponse tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, MutualTLSUtils::newCloseableHttpClientWithDefaultKeyStoreAndTrustStore);
        assertEquals(400, tokenResponse.getStatusCode());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }

}
