/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.oauth;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.enums.SslRequired;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.Event;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.Time;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RefreshTokenTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected LoginPage loginPage;

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    /**
     * KEYCLOAK-547
     *
     * @throws Exception
     */
    @Test
    public void nullRefreshToken() throws Exception {
        Client client = ClientBuilder.newClient();
        UriBuilder builder = UriBuilder.fromUri(org.keycloak.testsuite.Constants.AUTH_SERVER_ROOT);
        URI uri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget target = client.target(uri);

        org.keycloak.representations.AccessTokenResponse tokenResponse = null;
        {
            String header = BasicAuthHelper.createHeader("test-app", "password");
            Form form = new Form();
            Response response = target.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            Assert.assertEquals(400, response.getStatus());
            response.close();
        }
        events.clear();


    }

    @Test
    public void refreshTokenRequest() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        Event loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String refreshTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.verifyRefreshToken(refreshTokenString);

        Event tokenEvent = events.expectCodeToToken(codeId, sessionId).assertEvent();

        Assert.assertNotNull(refreshTokenString);

        Assert.assertEquals("bearer", tokenResponse.getTokenType());

        Assert.assertThat(token.getExpiration() - Time.currentTime(), allOf(greaterThanOrEqualTo(200), lessThanOrEqualTo(350)));
        int actual = refreshToken.getExpiration() - Time.currentTime();
        Assert.assertThat(actual, allOf(greaterThanOrEqualTo(1799), lessThanOrEqualTo(1800)));

        Assert.assertEquals(sessionId, refreshToken.getSessionState());

        Thread.sleep(2000);

        AccessTokenResponse response = oauth.doRefreshTokenRequest(refreshTokenString, "password");
        AccessToken refreshedToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.verifyRefreshToken(response.getRefreshToken());

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals(sessionId, refreshedToken.getSessionState());
        Assert.assertEquals(sessionId, refreshedRefreshToken.getSessionState());

        Assert.assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        Assert.assertThat(refreshedToken.getExpiration() - Time.currentTime(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));

        Assert.assertThat(refreshedToken.getExpiration() - token.getExpiration(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(5)));
        Assert.assertThat(refreshedRefreshToken.getExpiration() - refreshToken.getExpiration(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(5)));

        Assert.assertNotEquals(token.getId(), refreshedToken.getId());
        Assert.assertNotEquals(refreshToken.getId(), refreshedRefreshToken.getId());

        Assert.assertEquals("bearer", response.getTokenType());

        Assert.assertEquals(keycloakRule.getUser("test", "test-user@localhost").getId(), refreshedToken.getSubject());
        Assert.assertNotEquals("test-user@localhost", refreshedToken.getSubject());

        Assert.assertEquals(1, refreshedToken.getRealmAccess().getRoles().size());
        Assert.assertTrue(refreshedToken.getRealmAccess().isUserInRole("user"));

        Assert.assertEquals(1, refreshedToken.getResourceAccess(oauth.getClientId()).getRoles().size());
        Assert.assertTrue(refreshedToken.getResourceAccess(oauth.getClientId()).isUserInRole("customer-user"));

        Event refreshEvent = events.expectRefresh(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), sessionId).assertEvent();
        Assert.assertNotEquals(tokenEvent.getDetails().get(Details.TOKEN_ID), refreshEvent.getDetails().get(Details.TOKEN_ID));
        Assert.assertNotEquals(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), refreshEvent.getDetails().get(Details.UPDATED_REFRESH_TOKEN_ID));
    }

    PrivateKey privateKey;
    PublicKey publicKey;

    @Test
    public void refreshTokenRealmKeysChanged() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        Event loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        String refreshTokenString = response.getRefreshToken();
        RefreshToken refreshToken = oauth.verifyRefreshToken(refreshTokenString);

        events.expectCodeToToken(codeId, sessionId).assertEvent();

        try {
            keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    privateKey = appRealm.getPrivateKey();
                    publicKey = appRealm.getPublicKey();
                    KeycloakModelUtils.generateRealmKeys(appRealm);
                }
            });

            response = oauth.doRefreshTokenRequest(refreshTokenString, "password");

            assertEquals(400, response.getStatusCode());
            assertEquals("invalid_grant", response.getError());

            events.expectRefresh(refreshToken.getId(), sessionId).user((String) null).session((String) null).clearDetails().error(Errors.INVALID_TOKEN).assertEvent();
        } finally {
            keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    appRealm.setPrivateKey(privateKey);
                    appRealm.setPublicKey(publicKey);
                }
            });

        }
    }

    @Test
    public void refreshTokenUserSessionExpired() {
        oauth.doLogin("test-user@localhost", "password");

        Event loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        events.poll();

        String refreshId = oauth.verifyRefreshToken(tokenResponse.getRefreshToken()).getId();

        keycloakRule.removeUserSession(sessionId);

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);

        events.clear();
    }

    @Test
    public void testUserSessionRefreshAndIdle() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        Event loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        events.poll();

        String refreshId = oauth.verifyRefreshToken(tokenResponse.getRefreshToken()).getId();

        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = session.realms().getRealmByName("test");
        UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
        int last = userSession.getLastSessionRefresh();
        session.getTransaction().commit();
        session.close();

        Thread.sleep(2000);

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

        AccessToken refreshedToken = oauth.verifyToken(tokenResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.verifyRefreshToken(tokenResponse.getRefreshToken());

        Assert.assertEquals(200, tokenResponse.getStatusCode());

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("test");
        userSession = session.sessions().getUserSession(realm, sessionId);
        int next = userSession.getLastSessionRefresh();
        session.getTransaction().commit();
        session.close();

        Assert.assertNotEquals(last, next);

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("test");
        int lastAccessTokenLifespan = realm.getAccessTokenLifespan();
        realm.setAccessTokenLifespan(100000);
        session.getTransaction().commit();
        session.close();

        Thread.sleep(2000);
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("test");
        userSession = session.sessions().getUserSession(realm, sessionId);
        next = userSession.getLastSessionRefresh();
        session.getTransaction().commit();
        session.close();

        // lastSEssionRefresh should be updated because access code lifespan is higher than sso idle timeout
        Assert.assertThat(next, allOf(greaterThan(last), lessThan(last + 50)));

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("test");
        int originalIdle = realm.getSsoSessionIdleTimeout();
        realm.setSsoSessionIdleTimeout(1);
        session.getTransaction().commit();
        session.close();

        events.clear();
        Thread.sleep(2000);
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

        // test idle timeout
        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("test");
        realm.setSsoSessionIdleTimeout(originalIdle);
        realm.setAccessTokenLifespan(lastAccessTokenLifespan);
        session.getTransaction().commit();
        session.close();

        events.clear();
    }

    @Test
    public void refreshTokenUserSessionMaxLifespan() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        Event loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        events.poll();

        String refreshId = oauth.verifyRefreshToken(tokenResponse.getRefreshToken()).getId();

        KeycloakSession session = keycloakRule.startSession();
        RealmModel realm = session.realms().getRealmByName("test");
        int maxLifespan = realm.getSsoSessionMaxLifespan();
        realm.setSsoSessionMaxLifespan(1);
        session.getTransaction().commit();
        session.close();

        Thread.sleep(1000);

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "password");

        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        session = keycloakRule.startSession();
        realm = session.realms().getRealmByName("test");
        realm.setSsoSessionMaxLifespan(maxLifespan);
        session.getTransaction().commit();
        session.close();


        events.expectRefresh(refreshId, sessionId).error(Errors.INVALID_TOKEN);

        events.clear();
    }

    @Test
    public void testCheckSsl() throws Exception {
        Client client = ClientBuilder.newClient();
        UriBuilder builder = UriBuilder.fromUri(org.keycloak.testsuite.Constants.AUTH_SERVER_ROOT);
        URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget grantTarget = client.target(grantUri);
        builder = UriBuilder.fromUri(org.keycloak.testsuite.Constants.AUTH_SERVER_ROOT);
        URI uri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget refreshTarget = client.target(uri);

        String refreshToken = null;
        {
            Response response = executeGrantAccessTokenRequest(grantTarget);
            Assert.assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            refreshToken = tokenResponse.getRefreshToken();
            response.close();
        }

        {
            Response response = executeRefreshToken(refreshTarget, refreshToken);
            Assert.assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            refreshToken = tokenResponse.getRefreshToken();
            response.close();
        }

        {   // test checkSsl
            {
                KeycloakSession session = keycloakRule.startSession();
                RealmModel realm = session.realms().getRealmByName("test");
                realm.setSslRequired(SslRequired.ALL);
                session.getTransaction().commit();
                session.close();
            }

            Response response = executeRefreshToken(refreshTarget, refreshToken);
            Assert.assertEquals(403, response.getStatus());
            response.close();

            {
                KeycloakSession session = keycloakRule.startSession();
                RealmModel realm = session.realms().getRealmByName("test");
                realm.setSslRequired(SslRequired.EXTERNAL);
                session.getTransaction().commit();
                session.close();
            }

        }

        {
            Response response = executeRefreshToken(refreshTarget, refreshToken);
            Assert.assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            refreshToken = tokenResponse.getRefreshToken();
            response.close();
        }


        client.close();
        events.clear();

    }

    protected Response executeRefreshToken(WebTarget refreshTarget, String refreshToken) {
        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN);
        form.param("refresh_token", refreshToken);
        return refreshTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
    }

    protected Response executeGrantAccessTokenRequest(WebTarget grantTarget) {
        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .param("username", "test-user@localhost")
                .param("password", "password");
        return grantTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
    }


}
