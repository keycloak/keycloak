/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.forms;

import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.LogoutToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class RPInitiatedFrontChannelLogoutTest extends AbstractChangeImportedUserPasswordsTest {

    @Test
    public void testFrontChannelLogoutWithPostLogoutRedirectUri() throws Exception {
        ClientsResource clients = adminClient.realm(oauth.getRealm()).clients();
        ClientRepresentation rep = clients.findByClientId(oauth.getClientId()).get(0);
        rep.setFrontchannelLogout(true);
        rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, OAuthClient.APP_ROOT + "/admin/frontchannelLogout");
        clients.get(rep.getId()).update(rep);
        try {
            oauth.doLogin("test-user@localhost", getPassword("test-user@localhost"));
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            String idTokenString = tokenResponse.getIdToken();
            oauth.logoutForm().idTokenHint(idTokenString)
                    .postLogoutRedirectUri(OAuthClient.APP_AUTH_ROOT).open();
            LogoutToken logoutToken = testingClient.testApp().getFrontChannelLogoutToken();
            Assert.assertNotNull(logoutToken);

            IDToken idToken = new JWSInput(idTokenString).readJsonContent(IDToken.class);

            Assert.assertEquals(logoutToken.getIssuer(), idToken.getIssuer());
            Assert.assertEquals(logoutToken.getSid(), idToken.getSessionId());
        } finally {
            rep.setFrontchannelLogout(false);
            rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, "");
            clients.get(rep.getId()).update(rep);
        }
    }

    @Test
    public void testFrontChannelLogoutWithoutSessionRequired() throws Exception {
        ClientsResource clients = adminClient.realm(oauth.getRealm()).clients();
        ClientRepresentation rep = clients.findByClientId(oauth.getClientId()).get(0);
        rep.setFrontchannelLogout(true);
        rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, OAuthClient.APP_ROOT + "/admin/frontchannelLogout");
        rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED, "false");
        clients.get(rep.getId()).update(rep);
        try {
            oauth.doLogin("test-user@localhost", getPassword("test-user@localhost"));
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            String idTokenString = tokenResponse.getIdToken();
            oauth.logoutForm().idTokenHint(idTokenString)
                    .postLogoutRedirectUri(OAuthClient.APP_AUTH_ROOT).open();
            LogoutToken logoutToken = testingClient.testApp().getFrontChannelLogoutToken();
            Assert.assertNotNull(logoutToken);

            Assert.assertNull(logoutToken.getIssuer());
            Assert.assertNull(logoutToken.getSid());
        } finally {
            rep.setFrontchannelLogout(false);
            rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, "");
            rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED, "true");
            clients.get(rep.getId()).update(rep);
        }
    }

    @Test
    public void testFrontChannelLogout() throws Exception {
        ClientsResource clients = adminClient.realm(oauth.getRealm()).clients();
        ClientRepresentation rep = clients.findByClientId(oauth.getClientId()).get(0);
        rep.setName("My Testing App");
        rep.setFrontchannelLogout(true);
        rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, OAuthClient.APP_ROOT + "/admin/frontchannelLogout");
        clients.get(rep.getId()).update(rep);
        try {
            oauth.doLogin("test-user@localhost", getPassword("test-user@localhost"));
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            String idTokenString = tokenResponse.getIdToken();
            oauth.logoutForm().idTokenHint(idTokenString).open();
            LogoutToken logoutToken = testingClient.testApp().getFrontChannelLogoutToken();
            org.keycloak.testsuite.Assert.assertNotNull(logoutToken);
            IDToken idToken = new JWSInput(idTokenString).readJsonContent(IDToken.class);
            org.keycloak.testsuite.Assert.assertEquals(logoutToken.getIssuer(), idToken.getIssuer());
            org.keycloak.testsuite.Assert.assertEquals(logoutToken.getSid(), idToken.getSessionId());
            Assert.assertTrue(driver.getTitle().equals("Logging out"));
            Assert.assertTrue(driver.getPageSource().contains("You are logging out from following apps"));
            Assert.assertTrue(driver.getPageSource().contains("My Testing App"));
        } finally {
            rep.setFrontchannelLogout(false);
            rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, "");
            clients.get(rep.getId()).update(rep);
        }
    }

    @Test
    public void testFrontChannelLogoutCustomCSP() throws Exception {
        try (RealmAttributeUpdater realmUpdater = new RealmAttributeUpdater(adminClient.realm(oauth.getRealm()))
                .setBrowserSecurityHeader(BrowserSecurityHeaders.CONTENT_SECURITY_POLICY.getKey(),
                        "frame-src 'keycloak.org'; frame-ancestors 'self'; object-src 'none'; style-src 'self';")
                .update();
             ClientAttributeUpdater clientUpdater = ClientAttributeUpdater.forClient(adminClient, oauth.getRealm(), oauth.getClientId())
                .setName("My Testing App")
                .setFrontchannelLogout(true)
                .setAttribute(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, OAuthClient.APP_ROOT + "/admin/frontchannelLogout")
                .update()) {
            oauth.doLogin("test-user@localhost", getPassword("test-user@localhost"));
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            String idTokenString = tokenResponse.getIdToken();
            oauth.logoutForm().idTokenHint(idTokenString).open();
            LogoutToken logoutToken = testingClient.testApp().getFrontChannelLogoutToken();
            Assert.assertNotNull(logoutToken);
            IDToken idToken = new JWSInput(idTokenString).readJsonContent(IDToken.class);
            Assert.assertEquals(logoutToken.getIssuer(), idToken.getIssuer());
            Assert.assertEquals(logoutToken.getSid(), idToken.getSessionId());
            Assert.assertTrue(driver.getTitle().equals("Logging out"));
            Assert.assertTrue(driver.getPageSource().contains("You are logging out from following apps"));
            Assert.assertTrue(driver.getPageSource().contains("My Testing App"));
        }
     }
}
