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
package org.keycloak.test.broker.oidc;

import java.io.IOException;

import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link OIDCIdentityProvider#extractIdentity} covering how the broker session ID
 * is resolved from the sid claim in the ID token and the session_state in the token response.
 */
public class OIDCIdentityProviderTest {

    private static final String IDP_ALIAS = "test-idp";
    private static final String SUBJECT = "user-subject-123";
    private static final String SID_FROM_ID_TOKEN = "sid-from-id-token";
    private static final String SESSION_STATE_FROM_TOKEN_RESPONSE = "session-state-from-token-response";

    @Test
    public void extractIdentity_sidInIdToken_noSessionState_usesSid() throws IOException {
        TestOIDCIdentityProvider provider = createProvider();

        JsonWebToken idToken = buildIdToken(SUBJECT, SID_FROM_ID_TOKEN);
        BrokeredIdentityContext identity = provider.extractIdentity(null, null, idToken);

        Assert.assertEquals(IDP_ALIAS + "." + SID_FROM_ID_TOKEN, identity.getBrokerSessionId());
    }

    @Test
    public void extractIdentity_sidInIdToken_sessionStateInTokenResponse_prefersSid() throws IOException {
        TestOIDCIdentityProvider provider = createProvider();

        JsonWebToken idToken = buildIdToken(SUBJECT, SID_FROM_ID_TOKEN);
        AccessTokenResponse tokenResponse = buildTokenResponse(SESSION_STATE_FROM_TOKEN_RESPONSE);

        BrokeredIdentityContext identity = provider.extractIdentity(tokenResponse, null, idToken);

        Assert.assertEquals(IDP_ALIAS + "." + SID_FROM_ID_TOKEN, identity.getBrokerSessionId());
    }

    @Test
    public void extractIdentity_noSidInIdToken_sessionStateInTokenResponse_usesSessionState() throws IOException {
        TestOIDCIdentityProvider provider = createProvider();

        JsonWebToken idToken = buildIdToken(SUBJECT, null);
        AccessTokenResponse tokenResponse = buildTokenResponse(SESSION_STATE_FROM_TOKEN_RESPONSE);

        BrokeredIdentityContext identity = provider.extractIdentity(tokenResponse, null, idToken);

        Assert.assertEquals(IDP_ALIAS + "." + SESSION_STATE_FROM_TOKEN_RESPONSE, identity.getBrokerSessionId());
    }

    @Test
    public void extractIdentity_noSidNoSessionState_brokerSessionIdIsNull() throws IOException {
        TestOIDCIdentityProvider provider = createProvider();

        JsonWebToken idToken = buildIdToken(SUBJECT, null);

        BrokeredIdentityContext identity = provider.extractIdentity(null, null, idToken);

        Assert.assertNull(identity.getBrokerSessionId());
    }

    @Test
    public void extractIdentity_sidMatchesSessionState_noWarningSideEffect() throws IOException {
        TestOIDCIdentityProvider provider = createProvider();

        JsonWebToken idToken = buildIdToken(SUBJECT, SID_FROM_ID_TOKEN);
        AccessTokenResponse tokenResponse = buildTokenResponse(SID_FROM_ID_TOKEN);

        BrokeredIdentityContext identity = provider.extractIdentity(tokenResponse, null, idToken);

        Assert.assertEquals(IDP_ALIAS + "." + SID_FROM_ID_TOKEN, identity.getBrokerSessionId());
    }

    private TestOIDCIdentityProvider createProvider() {
        IdentityProviderModel model = new IdentityProviderModel();
        OIDCIdentityProviderConfig config = new OIDCIdentityProviderConfig(model);
        config.setAlias(IDP_ALIAS);
        config.setEnabled(true);
        config.setDisableUserInfoService(true); // avoid HTTP calls to userinfo endpoint
        return new TestOIDCIdentityProvider(config);
    }

    private static class TestOIDCIdentityProvider extends OIDCIdentityProvider {

        TestOIDCIdentityProvider(OIDCIdentityProviderConfig config) {
            super(null, config);
        }

        @Override
        public BrokeredIdentityContext extractIdentity(AccessTokenResponse tokenResponse,
                String accessToken, JsonWebToken idToken) throws IOException {
            return super.extractIdentity(tokenResponse, accessToken, idToken);
        }
    }

    private JsonWebToken buildIdToken(String subject, String sid) {
        JsonWebToken token = new JsonWebToken();
        token.subject(subject);
        if (sid != null) {
            token.getOtherClaims().put("sid", sid);
        }
        return token;
    }

    private AccessTokenResponse buildTokenResponse(String sessionState) {
        AccessTokenResponse response = new AccessTokenResponse();
        response.setSessionState(sessionState);
        return response;
    }
}
