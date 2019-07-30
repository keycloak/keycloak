/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.account;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.keycloak.common.Profile.Feature.ACCOUNT_API;
import static org.keycloak.testsuite.ProfileAssume.assumeFeatureEnabled;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.client.methods.HttpPost;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.DeviceInfo;
import org.keycloak.representations.account.ClientRepresentation;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.TokenUtil;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SessionRestServiceTest extends AbstractRestServiceTest {

    private static void customUserAgent(HttpPost httpPost) {
        httpPost.addHeader("User-Agent",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 5_1_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B206 Safari/7534.48.3");
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);

        testRealm.getClients().add(ClientBuilder.create()
                .clientId("public-client-0")
                .name("Public Client 0")
                .baseUrl("http://client0.example.com")
                .redirectUris(OAuthClient.APP_ROOT + "/auth")
                .publicClient().build());

        testRealm.getClients().add(ClientBuilder.create()
                .clientId("mobile-client-0")
                .name("Mobile Client 0")
                .secret("secret")
                .directAccessGrants().build());
    }
    
    @Test
    public void testProfilePreviewPermissions() throws IOException {
        assumeFeatureEnabled(ACCOUNT_API);
        
        TokenUtil noaccessToken = new TokenUtil("no-account-access", "password");
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        
        // Read sessions with no access
        assertEquals(403, SimpleHttp.doGet(getAccountUrl("sessions"), httpClient).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus());
        
        // Delete all sessions with no access
        assertEquals(403, SimpleHttp.doDelete(getAccountUrl("sessions"), httpClient).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus());
        
        // Delete all sessions with read only
        assertEquals(403, SimpleHttp.doDelete(getAccountUrl("sessions"), httpClient).header("Accept", "application/json").auth(viewToken.getToken()).asStatus());
        
        // Delete single session with no access
        assertEquals(403, SimpleHttp.doDelete(getAccountUrl("sessions/bogusId"), httpClient).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus());
        
        // Delete single session with read only
        assertEquals(403, SimpleHttp.doDelete(getAccountUrl("sessions/bogusId"), httpClient).header("Accept", "application/json").auth(viewToken.getToken()).asStatus());
    }

    @Test
    public void testGetSessions() throws Exception {
        assumeFeatureEnabled(ACCOUNT_API);

        oauth.clientId("public-client-0");
        oauth.redirectUri(OAuthClient.APP_ROOT + "/auth");
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        assertEquals(200, response.getStatusCode());
        
        oauth.clientId("mobile-client-0");
        response = oauth
                .doBeforeGrantAccessTokenRequest("secret", "test-user@localhost", "password", SessionRestServiceTest::customUserAgent);

        assertEquals(200, response.getStatusCode());
        
        List<SessionRepresentation> sessions = SimpleHttp.doGet(getAccountUrl("sessions"), httpClient).auth(tokenUtil.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});

        assertEquals(3, sessions.size());

        for (SessionRepresentation session : sessions) {
            assertNotNull(session.getId());
            assertThat(session.getStarted(), Matchers.greaterThan(0));
            assertThat(session.getLastAccess(), Matchers.greaterThan(0));
            assertThat(session.getExpires(), Matchers.greaterThan(0));
            assertEquals(1, session.getClients().size());
            assertThat(session.getClients(), hasItems(hasProperty("clientId", isOneOf("direct-grant", "public-client-0", "mobile-client-0"))));

            ClientRepresentation client = session.getClients().get(0);

            DeviceInfo deviceInfo = session.getDeviceInfo();
            
            if ("direct-grant".equalsIgnoreCase(client.getClientId())) {
                assertNull(deviceInfo);
            } else if ("public-client-0".equalsIgnoreCase(client.getClientId())) {
                assertNotNull(deviceInfo);
                assertNotNull(deviceInfo.getIp());
                assertEquals("Other", deviceInfo.getDevice());
                assertEquals("Chrome", deviceInfo.getBrowser());
                assertEquals("58.0.3029", deviceInfo.getBrowserVersion());
                assertEquals("Windows", deviceInfo.getOs());
                assertEquals("7", deviceInfo.getOsVersion());
            } else if ("mobile-client-0".equalsIgnoreCase(client.getClientId())) {
                assertNotNull(deviceInfo);
                assertNotNull(deviceInfo.getIp());
                assertEquals("iPhone", deviceInfo.getDevice());
                assertEquals("Mobile Safari", deviceInfo.getBrowser());
                assertEquals("5.1", deviceInfo.getBrowserVersion());
                assertEquals("iOS", deviceInfo.getOs());
                assertEquals("5.1.1", deviceInfo.getOsVersion());
            }
        }
    }

    @Test
    public void testDeleteSession() throws IOException {
        assumeFeatureEnabled(ACCOUNT_API);
        
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        String sessionId = oauth.doLogin("view-account-access", "password").getSessionState();
        List<SessionRepresentation> sessions = SimpleHttp.doGet(getAccountUrl("sessions"), httpClient).auth(viewToken.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});
        assertEquals(2, sessions.size());

        // With `ViewToken` you can only read
        int status = SimpleHttp.doDelete(getAccountUrl("sessions/" + sessionId), httpClient).acceptJson().auth(viewToken.getToken()).asStatus();
        assertEquals(403, status);
        sessions = SimpleHttp.doGet(getAccountUrl("sessions"), httpClient).auth(viewToken.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});
        assertEquals(2, sessions.size());

        // Here you can delete the session
        status = SimpleHttp.doDelete(getAccountUrl("sessions/" + sessionId), httpClient).acceptJson().auth(tokenUtil.getToken()).asStatus();
        assertEquals(200, status);
        sessions = SimpleHttp.doGet(getAccountUrl("sessions"), httpClient).auth(tokenUtil.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});
        assertEquals(1, sessions.size());
    }
}
