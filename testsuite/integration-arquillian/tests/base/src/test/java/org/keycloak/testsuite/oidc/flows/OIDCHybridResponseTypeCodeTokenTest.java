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

package org.keycloak.testsuite.oidc.flows;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.events.Details;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.OAuthClient;

import java.util.Collections;
import java.util.List;

/**
 * Tests with response_type=code token
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCHybridResponseTypeCodeTokenTest extends AbstractOIDCResponseTypeTest  {

    @Before
    public void clientConfiguration() {
        clientManagerBuilder().standardFlow(true).implicitFlow(true);

        oauth.clientId("test-app");
        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.TOKEN);
    }


    @Override
    protected boolean isFragment() {
        return true;
    }


    protected List<IDToken> testAuthzResponseAndRetrieveIDTokens(OAuthClient.AuthorizationEndpointResponse authzResponse, EventRepresentation loginEvent) {
        Assert.assertEquals(OIDCResponseType.CODE + " " + OIDCResponseType.TOKEN, loginEvent.getDetails().get(Details.RESPONSE_TYPE));

        Assert.assertNotNull(authzResponse.getAccessToken());
        Assert.assertNull(authzResponse.getIdToken());

        // IDToken exchanged for the code
        OAuthClient.AccessTokenResponse authzResponse2 = sendTokenRequestAndGetResponse(loginEvent);
        IDToken idToken2 = oauth.verifyIDToken(authzResponse2.getIdToken());

        // Validate "at_hash"
        assertValidAccessTokenHash(idToken2.getAccessTokenHash(), authzResponse2.getAccessToken());

        // Validate if token_type is present
        Assert.assertNotNull(authzResponse.getTokenType());

        // Validate if expires_in is present
        Assert.assertNotNull(authzResponse.getExpiresIn());

        return Collections.singletonList(idToken2);
    }


    @Test
    public void nonceNotUsedErrorExpected() {
        super.validateNonceNotUsedSuccessExpected();
    }

    @Test
    public void errorStandardFlowNotAllowed() throws Exception {
        super.validateErrorStandardFlowNotAllowed();
    }

    @Test
    public void errorImplicitFlowNotAllowed() throws Exception {
        super.validateErrorImplicitFlowNotAllowed();
    }

}
