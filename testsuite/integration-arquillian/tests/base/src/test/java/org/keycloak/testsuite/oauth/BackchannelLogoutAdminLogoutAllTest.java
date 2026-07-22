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
 */

package org.keycloak.testsuite.oauth;

import java.util.List;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.LogoutToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for backchannel logout sid claim behaviour when admin invokes logout for all user sessions (issue #22914).
 *
 * @author <a href="mailto:jakub.choc@pm.me">Jakub Choc</a>
 */
public class BackchannelLogoutAdminLogoutAllTest extends AbstractTestRealmKeycloakTest {

    private static final String BACKCHANNEL_LOGOUT_URL = OAuthClient.APP_ROOT + "/admin/backchannelLogout";

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void setUp() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
        testingClient.testApp().clearAdminActions();
    }

    @Test
    public void adminLogoutAllSessions_singleSession_logoutTokenHasSid() throws Exception {
        try (ClientAttributeUpdater ignored = ClientAttributeUpdater.forClient(adminClient, "test", "test-app")
                .setAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, BACKCHANNEL_LOGOUT_URL)
                .setAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_SESSION_REQUIRED, "true")
                .update()) {

            loginUser();
            String userId = findUser("test-user@localhost").getId();

            adminClient.realm("test").users().get(userId).logout();

            String rawLogoutToken = testingClient.testApp().getBackChannelRawLogoutToken();
            assertNotNull(rawLogoutToken);

            LogoutToken logoutToken = new JWSInput(rawLogoutToken).readJsonContent(LogoutToken.class);

            // single session: sid is kept, no ambiguity
            assertNotNull(logoutToken.getSubject());
            assertNotNull(logoutToken.getSid());
        }
    }

    @Test
    public void adminLogoutAllSessions_multipleSessions_logoutTokenHasNoSid() throws Exception {
        try (ClientAttributeUpdater ignored = ClientAttributeUpdater.forClient(adminClient, "test", "test-app")
                .setAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, BACKCHANNEL_LOGOUT_URL)
                .setAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_SESSION_REQUIRED, "true")
                .update()) {

            loginUser();
            String userId = findUser("test-user@localhost").getId();

            // create a second session via password grant
            AccessTokenResponse session2 = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            assertNull(session2.getError());

            List<UserSessionRepresentation> sessions =
                    adminClient.realm("test").users().get(userId).getUserSessions();
            assertEquals(2, sessions.size());

            adminClient.realm("test").users().get(userId).logout();

            String rawLogoutToken = testingClient.testApp().getBackChannelRawLogoutToken();
            assertNotNull(rawLogoutToken);

            LogoutToken logoutToken = new JWSInput(rawLogoutToken).readJsonContent(LogoutToken.class);

            // multiple sessions: sid must be absent so RP terminates all sessions for this sub
            assertNotNull(logoutToken.getSubject());
            assertNull(logoutToken.getSid());

            // the client is notified exactly once (one sid-less token covers all of the user's sessions),
            // not once per session - so no further token is delivered
            assertNull(testingClient.testApp().getBackChannelRawLogoutToken());
        }
    }

    @Test
    public void userInitiatedLogout_singleSession_logoutTokenHasSid() throws Exception {
        try (ClientAttributeUpdater ignored = ClientAttributeUpdater.forClient(adminClient, "test", "test-app")
                .setAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, BACKCHANNEL_LOGOUT_URL)
                .setAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_SESSION_REQUIRED, "true")
                .update()) {

            AccessTokenResponse tokenResponse = loginUser();

            oauth.logoutForm()
                    .idTokenHint(tokenResponse.getIdToken())
                    .postLogoutRedirectUri(OAuthClient.APP_AUTH_ROOT)
                    .open();

            String rawLogoutToken = testingClient.testApp().getBackChannelRawLogoutToken();
            assertNotNull(rawLogoutToken);

            LogoutToken logoutToken = new JWSInput(rawLogoutToken).readJsonContent(LogoutToken.class);

            // regression guard: user-initiated logout must still include sid
            assertNotNull(logoutToken.getSubject());
            assertNotNull(logoutToken.getSid());
        }
    }

    private AccessTokenResponse loginUser() {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();
        return oauth.doAccessTokenRequest(code);
    }
}
