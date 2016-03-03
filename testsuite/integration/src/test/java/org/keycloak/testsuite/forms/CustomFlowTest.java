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
package org.keycloak.testsuite.forms;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
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
import org.openqa.selenium.WebDriver;

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
            ClientModel dummyClient = KeycloakModelUtils.createClient(appRealm, "dummy-client");
            dummyClient.setClientAuthenticatorType(PassThroughClientAuthenticator.PROVIDER_ID);
            dummyClient.setDirectAccessGrantsEnabled(true);

            ClientModel testApp = appRealm.getClientByClientId("test-app");
            testApp.setClientAuthenticatorType(PassThroughClientAuthenticator.PROVIDER_ID);
            testApp.setDirectAccessGrantsEnabled(true);
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
        assertEquals("unauthorized_client", response.getError());

        events.expectLogin()
                .client((String) null)
                .user((String) null)
                .session((String) null)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .error(Errors.INVALID_CLIENT_CREDENTIALS)
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
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
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
