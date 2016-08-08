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

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.events.Details;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;

/**
 * Test for response_type=code
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCBasicResponseTypeCodeTest extends AbstractOIDCResponseTypeTest {

    @Before
    public void clientConfiguration() {
        clientManagerBuilder().standardFlow(true).implicitFlow(false);

        oauth.clientId("test-app");
        oauth.responseType(OIDCResponseType.CODE);
    }


    protected List<IDToken> retrieveIDTokens(EventRepresentation loginEvent) {
        Assert.assertEquals(OIDCResponseType.CODE, loginEvent.getDetails().get(Details.RESPONSE_TYPE));

        OAuthClient.AuthorizationEndpointResponse authzResponse = new OAuthClient.AuthorizationEndpointResponse(oauth, false);
        Assert.assertNull(authzResponse.getAccessToken());
        Assert.assertNull(authzResponse.getIdToken());

        IDToken idToken = sendTokenRequestAndGetIDToken(loginEvent);

        return Collections.singletonList(idToken);
    }


    @Test
    public void nonceNotUsed() {
        EventRepresentation loginEvent = loginUser(null);

        List<IDToken> idTokens = retrieveIDTokens(loginEvent);
        for (IDToken idToken : idTokens) {
            Assert.assertNull(idToken.getNonce());
        }
    }

    @Test
    public void errorStandardFlowNotAllowed() throws Exception {
        super.validateErrorStandardFlowNotAllowed();
    }
}
