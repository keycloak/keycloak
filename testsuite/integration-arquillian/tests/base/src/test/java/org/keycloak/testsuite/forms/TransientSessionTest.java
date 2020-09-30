/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.forms;

import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.OAuthClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;

/**
 * Test for transient user session
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class TransientSessionTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void loginSuccess() throws Exception {
        setUpDirectGrantFlowWithSetClientNoteAuthenticator();

        oauth.clientId("direct-grant");

        // Signal that we want userSession to be transient
        oauth.addCustomParameter(SetClientNoteAuthenticator.PREFIX + AuthenticationManager.USER_SESSION_PERSISTENT_STATE, UserSessionModel.SessionPersistenceState.TRANSIENT.toString());

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        // sessionState is available, but the session was transient and hence not really persisted on the server
        assertNotNull(accessToken.getSessionState());
        assertEquals(accessToken.getSessionState(), refreshToken.getSessionState());

        // Refresh will fail. There is no userSession on the server
        OAuthClient.AccessTokenResponse refreshedResponse = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");
        Assert.assertNull(refreshedResponse.getAccessToken());
        assertNotNull(refreshedResponse.getError());
        Assert.assertEquals("Session not active", refreshedResponse.getErrorDescription());
    }

    private void setUpDirectGrantFlowWithSetClientNoteAuthenticator() {
        final String newFlowAlias = "directGrantCustom";
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyFlow(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, newFlowAlias));
        testingClient.server("test").run(session -> {
            FlowUtil.inCurrentRealm(session)
                    .selectFlow(newFlowAlias)
                    .addAuthenticatorExecution(REQUIRED, SetClientNoteAuthenticator.PROVIDER_ID)
                    .defineAsDirectGrantFlow();
        });
    }

}
