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

package org.keycloak.services.clientpolicy.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.ClientData;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GrantTypeConditionTest {

    private static final List<String> HYBRID_RESPONSE_TYPES = Arrays.asList("code token", "code id_token", "code id_token token");

    private static final List<ClientPolicyVote> ALL_YES = Arrays.asList(ClientPolicyVote.YES, ClientPolicyVote.YES, ClientPolicyVote.YES);

    @Test
    public void codeResponseMatchesAuthorizationCodeOnly() throws ClientPolicyException {
        assertEquals(ClientPolicyVote.YES, vote("code", OAuth2Constants.AUTHORIZATION_CODE));
        assertEquals(ClientPolicyVote.NO, vote("code", OAuth2Constants.IMPLICIT));
    }

    @Test
    public void implicitResponseMatchesImplicitOnly() throws ClientPolicyException {
        for (String responseType : Arrays.asList("token", "id_token", "id_token token")) {
            assertEquals(responseType, ClientPolicyVote.YES, vote(responseType, OAuth2Constants.IMPLICIT));
            assertEquals(responseType, ClientPolicyVote.NO, vote(responseType, OAuth2Constants.AUTHORIZATION_CODE));
        }
    }

    @Test
    public void hybridResponseMatchesImplicit() throws ClientPolicyException {
        assertEquals(ALL_YES, votes(OAuth2Constants.IMPLICIT));
    }

    @Test
    public void hybridResponseMatchesAuthorizationCode() throws ClientPolicyException {
        assertEquals(ALL_YES, votes(OAuth2Constants.AUTHORIZATION_CODE));
    }

    @Test
    public void noneResponseMatchesNothing() throws ClientPolicyException {
        assertEquals(ClientPolicyVote.NO, vote("none", OAuth2Constants.AUTHORIZATION_CODE));
        assertEquals(ClientPolicyVote.NO, vote("none", OAuth2Constants.IMPLICIT));
    }

    private List<ClientPolicyVote> votes(String... grantTypes) throws ClientPolicyException {
        List<ClientPolicyVote> votes = new ArrayList<>();
        for (String responseType : HYBRID_RESPONSE_TYPES) {
            votes.add(vote(responseType, grantTypes));
        }
        return votes;
    }

    private ClientPolicyVote vote(String responseType, String... grantTypes) throws ClientPolicyException {
        GrantTypeCondition condition = new GrantTypeCondition(null);
        GrantTypeCondition.Configuration configuration = new GrantTypeCondition.Configuration();
        configuration.setGrantTypes(Arrays.asList(grantTypes));
        condition.setupConfiguration(configuration);

        AuthorizationEndpointRequest request = AuthorizationEndpointRequest.fromClientData(
                new ClientData(null, responseType, null, null));

        return condition.applyPolicy(new AuthorizationRequestContext(
                OIDCResponseType.parse(responseType), request, null, null, null));
    }
}
