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
package org.keycloak.testsuite.forms;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.ClientModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.util.Time;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class CustomFlowTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {
        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            UserModel user = manager.getSession().users().addUser(appRealm, "login-test");
            user.setEmail("login@test.com");
            user.setEnabled(true);

            userId = user.getId();

            AuthenticationFlowModel flow = new AuthenticationFlowModel();
            flow.setAlias("dummy");
            flow.setDescription("dummy pass through flow");
            flow.setProviderId("basic-flow");
            flow.setTopLevel(true);
            flow.setBuiltIn(false);
            flow = appRealm.addAuthenticationFlow(flow);
            appRealm.setBrowserFlow(flow);
            appRealm.setDirectGrantFlow(flow);

            AuthenticationExecutionModel execution;

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(flow.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(PassThroughAuthenticator.PROVIDER_ID);
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);
            appRealm.addAuthenticatorExecution(execution);



            flow = new AuthenticationFlowModel();
            flow.setAlias("dummy registration");
            flow.setDescription("dummy pass through registration");
            flow.setProviderId("basic-flow");
            flow.setTopLevel(true);
            flow.setBuiltIn(false);
            flow = appRealm.addAuthenticationFlow(flow);
            appRealm.setRegistrationFlow(flow);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(flow.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(PassThroughRegistration.PROVIDER_ID);
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);
            appRealm.addAuthenticatorExecution(execution);

            AuthenticationFlowModel clientFlow = new AuthenticationFlowModel();
            clientFlow.setAlias("client-dummy");
            clientFlow.setDescription("dummy pass through flow");
            clientFlow.setProviderId(AuthenticationFlow.CLIENT_FLOW);
            clientFlow.setTopLevel(true);
            clientFlow.setBuiltIn(false);
            clientFlow = appRealm.addAuthenticationFlow(clientFlow);
            appRealm.setClientAuthenticationFlow(clientFlow);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(clientFlow.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(PassThroughClientAuthenticator.PROVIDER_ID);
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);
            appRealm.addAuthenticatorExecution(execution);

            // Set passthrough clientAuthenticator for our clients
            ClientModel dummyClient = new ClientManager().createClient(appRealm, "dummy-client");
            dummyClient.setClientAuthenticatorType(PassThroughClientAuthenticator.PROVIDER_ID);
            appRealm.getClientByClientId("test-app").setClientAuthenticatorType(PassThroughClientAuthenticator.PROVIDER_ID);
        }
    });

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected ErrorPage errorPage;
    
    @WebResource
    protected LoginPasswordUpdatePage updatePasswordPage;

    @WebResource
    protected RegisterPage registerPage;

    private static String userId;

    @Test
    public void loginSuccess() {

        PassThroughAuthenticator.username = "login-test";

        oauth.openLoginForm();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void grantTest() throws Exception {
        PassThroughAuthenticator.username = "login-test";
        grantAccessToken("test-app", "login-test");
    }

    @Test
    public void clientAuthTest() throws Exception {
        PassThroughClientAuthenticator.clientId = "dummy-client";
        PassThroughAuthenticator.username = "login-test";
        grantAccessToken("dummy-client", "login-test");

        PassThroughClientAuthenticator.clientId = "test-app";
        grantAccessToken("test-app", "login-test");

        PassThroughClientAuthenticator.clientId = "unknown";
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user", "password");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_client", response.getError());

        events.expectLogin()
                .client((String) null)
                .user((String) null)
                .session((String) null)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .error(Errors.CLIENT_NOT_FOUND)
                .assertEvent();
    }


    private void grantAccessToken(String clientId, String login) throws Exception {

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", login, "password");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.verifyRefreshToken(response.getRefreshToken());

        events.expectLogin()
                .client(clientId)
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.RESPONSE_TYPE, "token")
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, login)
                .detail(Details.CLIENT_AUTH_METHOD, PassThroughClientAuthenticator.PROVIDER_ID)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();

        assertEquals(accessToken.getSessionState(), refreshToken.getSessionState());

        OAuthClient.AccessTokenResponse refreshedResponse = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");

        AccessToken refreshedAccessToken = oauth.verifyToken(refreshedResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.verifyRefreshToken(refreshedResponse.getRefreshToken());

        assertEquals(accessToken.getSessionState(), refreshedAccessToken.getSessionState());
        assertEquals(accessToken.getSessionState(), refreshedRefreshToken.getSessionState());

        events.expectRefresh(refreshToken.getId(), refreshToken.getSessionState())
                .user(userId)
                .client(clientId)
                .detail(Details.CLIENT_AUTH_METHOD, PassThroughClientAuthenticator.PROVIDER_ID)
                .assertEvent();
    }


}
