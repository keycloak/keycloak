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
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OpenIDConnectService;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.util.BasicAuthHelper;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccessTokenTest {

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

    @Test
    public void accessTokenRequest() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        Event loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));

        Assert.assertEquals("bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals(keycloakRule.getUser("test", "test-user@localhost").getId(), token.getSubject());
        Assert.assertNotEquals("test-user@localhost", token.getSubject());

        Assert.assertEquals(sessionId, token.getSessionState());

        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getRealmAccess().isUserInRole("user"));

        Assert.assertEquals(1, token.getResourceAccess(oauth.getClientId()).getRoles().size());
        Assert.assertTrue(token.getResourceAccess(oauth.getClientId()).isUserInRole("customer-user"));

        Event event = events.expectCodeToToken(codeId, sessionId).assertEvent();
        Assert.assertEquals(token.getId(), event.getDetails().get(Details.TOKEN_ID));
        Assert.assertEquals(oauth.verifyRefreshToken(response.getRefreshToken()).getId(), event.getDetails().get(Details.REFRESH_TOKEN_ID));
        Assert.assertEquals(sessionId, token.getSessionState());

    }

    @Test
    public void accessTokenInvalidClientCredentials() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        Event loginEvent = events.expectLogin().assertEvent();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "invalid");
        Assert.assertEquals(400, response.getStatusCode());

        AssertEvents.ExpectedEvent expectedEvent = events.expectCodeToToken(codeId, loginEvent.getSessionId()).error("invalid_client_credentials").removeDetail(Details.TOKEN_ID).removeDetail(Details.REFRESH_TOKEN_ID);
        expectedEvent.assertEvent();
    }

    @Test
    public void accessTokenUserSessionExpired() {
        oauth.doLogin("test-user@localhost", "password");

        Event loginEvent = events.expectLogin().assertEvent();

        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String sessionId = loginEvent.getSessionId();

        keycloakRule.removeUserSession(sessionId);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        events.expectCodeToToken(codeId, sessionId).removeDetail(Details.TOKEN_ID).client((String) null).user((String) null).session((String) null).removeDetail(Details.REFRESH_TOKEN_ID).error(Errors.INVALID_CODE).assertEvent();

        events.clear();
    }

    @Test
    public void accessTokenCodeExpired() {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setAccessCodeLifespan(1);
            }
        });

        oauth.doLogin("test-user@localhost", "password");

        Event loginEvent = events.expectLogin().assertEvent();

        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        Assert.assertEquals(400, response.getStatusCode());

        AssertEvents.ExpectedEvent expectedEvent = events.expectCodeToToken(codeId, null);
        expectedEvent.error("invalid_code").removeDetail(Details.TOKEN_ID).removeDetail(Details.REFRESH_TOKEN_ID).client((String) null).user((String) null);
        expectedEvent.assertEvent();

        events.clear();

        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setAccessCodeLifespan(60);
            }
        });
    }

    @Test
    public void accessTokenCodeUsed() {
        oauth.doLogin("test-user@localhost", "password");

        Event loginEvent = events.expectLogin().assertEvent();

        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        Assert.assertEquals(200, response.getStatusCode());

        events.clear();

        response = oauth.doAccessTokenRequest(code, "password");
        Assert.assertEquals(400, response.getStatusCode());

        AssertEvents.ExpectedEvent expectedEvent = events.expectCodeToToken(codeId, null);
        expectedEvent.error("invalid_code").removeDetail(Details.TOKEN_ID).removeDetail(Details.REFRESH_TOKEN_ID).client((String) null).user((String) null);
        expectedEvent.assertEvent();

        events.clear();

        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setAccessCodeLifespan(60);
            }
        });
    }

    @Test
    public void accessTokenCodeRoleMissing() {
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                RoleModel role = appRealm.addRole("tmp-role");
                session.users().getUserByUsername("test-user@localhost", appRealm).grantRole(role);
            }
        });

        oauth.doLogin("test-user@localhost", "password");

        Event loginEvent = events.expectLogin().assertEvent();

        loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.removeRole(appRealm.getRole("tmp-role"));
            }
        });

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        AccessToken token = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getRealmAccess().isUserInRole("user"));

        events.clear();
    }

    @Test
    public void accessTokenCodeHasRequiredAction() {
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel defaultRealm, RealmModel appRealm) {
                UserModel user = manager.getSession().users().getUserByUsername("test-user@localhost", appRealm);
                user.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
            }
        });

        oauth.doLogin("test-user@localhost", "password");

        String code = driver.getPageSource().split("code=")[1].split("&")[0].split("\"")[0];

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        Assert.assertEquals(400, response.getStatusCode());

        Event event = events.poll();
        assertNotNull(event.getDetails().get(Details.CODE_ID));

        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                manager.getSession().users().getUserByUsername("test-user@localhost", appRealm).removeRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
            }
        });
    }

    @Test
    public void testValidateAccessToken() throws Exception {
        Client client = ClientBuilder.newClient();
        UriBuilder builder = UriBuilder.fromUri(org.keycloak.testsuite.Constants.AUTH_SERVER_ROOT);
        URI grantUri = OpenIDConnectService.grantAccessTokenUrl(builder).build("test");
        WebTarget grantTarget = client.target(grantUri);
        builder = UriBuilder.fromUri(org.keycloak.testsuite.Constants.AUTH_SERVER_ROOT);
        URI validateUri = OpenIDConnectService.validateAccessTokenUrl(builder).build("test");
        WebTarget validateTarget = client.target(validateUri);

        {
            Response response = validateTarget.queryParam("access_token", "bad token").request().get();
            Assert.assertEquals(400, response.getStatus());
            HashMap<String, String> error = response.readEntity(new GenericType <HashMap<String, String>>() {});
            Assert.assertNotNull(error.get("error"));
        }


        org.keycloak.representations.AccessTokenResponse tokenResponse = null;
        {
            Response response = executeGrantAccessTokenRequest(grantTarget);
            Assert.assertEquals(200, response.getStatus());
            tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            response.close();
        }

        {
            Response response = validateTarget.queryParam("access_token", tokenResponse.getToken()).request().get();
            Assert.assertEquals(200, response.getStatus());
            AccessToken token = response.readEntity(AccessToken.class);
            Assert.assertNotNull(token);
            response.close();
        }
        {
            builder = UriBuilder.fromUri(org.keycloak.testsuite.Constants.AUTH_SERVER_ROOT);
            URI logoutUri = OpenIDConnectService.logoutUrl(builder).build("test");
            String header = BasicAuthHelper.createHeader("test-app", "password");
            Form form = new Form();
            form.param("refresh_token", tokenResponse.getRefreshToken());
            Response response = client.target(logoutUri).request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            Assert.assertEquals(204, response.getStatus());
            response.close();
        }
        {
            Response response = validateTarget.queryParam("access_token", tokenResponse.getToken()).request().get();
            Assert.assertEquals(400, response.getStatus());
            HashMap<String, String> error = response.readEntity(new GenericType <HashMap<String, String>>() {});
            Assert.assertNotNull(error.get("error"));
        }

        client.close();
        events.clear();

    }

    @Test
    public void testGrantAccessToken() throws Exception {
        Client client = ClientBuilder.newClient();
        UriBuilder builder = UriBuilder.fromUri(org.keycloak.testsuite.Constants.AUTH_SERVER_ROOT);
        URI grantUri = OpenIDConnectService.grantAccessTokenUrl(builder).build("test");
        WebTarget grantTarget = client.target(grantUri);

        {   // test checkSsl
            {
                KeycloakSession session = keycloakRule.startSession();
                RealmModel realm = session.realms().getRealmByName("test");
                realm.setSslRequired(SslRequired.ALL);
                session.getTransaction().commit();
                session.close();
            }

            Response response = executeGrantAccessTokenRequest(grantTarget);
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

        {   // test null username
            String header = BasicAuthHelper.createHeader("test-app", "password");
            Form form = new Form();
            form.param("password", "password");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            Assert.assertEquals(401, response.getStatus());
            response.close();
        }

        {   // test no password
            String header = BasicAuthHelper.createHeader("test-app", "password");
            Form form = new Form();
            form.param("username", "test-user@localhost");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            Assert.assertEquals(400, response.getStatus());
            response.close();
        }

        {   // test bearer-only

            {
                KeycloakSession session = keycloakRule.startSession();
                RealmModel realm = session.realms().getRealmByName("test");
                ApplicationModel clientModel = realm.getApplicationByName("test-app");
                clientModel.setBearerOnly(true);
                session.getTransaction().commit();
                session.close();
            }


            Response response = executeGrantAccessTokenRequest(grantTarget);
            Assert.assertEquals(400, response.getStatus());
            response.close();

            {
                KeycloakSession session = keycloakRule.startSession();
                RealmModel realm = session.realms().getRealmByName("test");
                ApplicationModel clientModel = realm.getApplicationByName("test-app");
                clientModel.setBearerOnly(false);
                session.getTransaction().commit();
                session.close();
            }

        }

        {   // test realm disabled
            {
                KeycloakSession session = keycloakRule.startSession();
                RealmModel realm = session.realms().getRealmByName("test");
                realm.setEnabled(false);
                session.getTransaction().commit();
                session.close();
            }

            Response response = executeGrantAccessTokenRequest(grantTarget);
            Assert.assertEquals(401, response.getStatus());
            response.close();

            {
                KeycloakSession session = keycloakRule.startSession();
                RealmModel realm = session.realms().getRealmByName("test");
                realm.setEnabled(true);
                session.getTransaction().commit();
                session.close();
            }

        }

        {   // test application disabled

            {
                KeycloakSession session = keycloakRule.startSession();
                RealmModel realm = session.realms().getRealmByName("test");
                ClientModel clientModel = realm.findClient("test-app");
                clientModel.setEnabled(false);
                session.getTransaction().commit();
                session.close();
            }


            Response response = executeGrantAccessTokenRequest(grantTarget);
            Assert.assertEquals(400, response.getStatus());
            response.close();

            {
                KeycloakSession session = keycloakRule.startSession();
                RealmModel realm = session.realms().getRealmByName("test");
                ClientModel clientModel = realm.findClient("test-app");
                clientModel.setEnabled(true);
                session.getTransaction().commit();
                session.close();
            }

        }

        {   // test user action required

            {
                KeycloakSession session = keycloakRule.startSession();
                RealmModel realm = session.realms().getRealmByName("test");
                UserModel user = session.users().getUserByUsername("test-user@localhost", realm);
                user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                session.getTransaction().commit();
                session.close();
            }


            Response response = executeGrantAccessTokenRequest(grantTarget);
            Assert.assertEquals(400, response.getStatus());
            response.close();

            {
                KeycloakSession session = keycloakRule.startSession();
                RealmModel realm = session.realms().getRealmByName("test");
                UserModel user = session.users().getUserByUsername("test-user@localhost", realm);
                user.removeRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                session.getTransaction().commit();
                session.close();
            }

        }
        {   // test user disabled
            {
                KeycloakSession session = keycloakRule.startSession();
                RealmModel realm = session.realms().getRealmByName("test");
                UserModel user = session.users().getUserByUsername("test-user@localhost", realm);
                user.setEnabled(false);
                session.getTransaction().commit();
                session.close();
            }


            Response response = executeGrantAccessTokenRequest(grantTarget);
            Assert.assertEquals(400, response.getStatus());
            response.close();

            {
                KeycloakSession session = keycloakRule.startSession();
                RealmModel realm = session.realms().getRealmByName("test");
                UserModel user = session.users().getUserByUsername("test-user@localhost", realm);
                user.setEnabled(true);
                session.getTransaction().commit();
                session.close();
            }

        }


        {
            Response response = executeGrantAccessTokenRequest(grantTarget);
            Assert.assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            response.close();
        }

        client.close();
        events.clear();

    }

    protected Response executeGrantAccessTokenRequest(WebTarget grantTarget) {
        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        form.param("username", "test-user@localhost")
                .param("password", "password");
        return grantTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
    }


}
