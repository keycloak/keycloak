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

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator;
import org.keycloak.authentication.authenticators.x509.ValidateX509CertificateUsernameFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.pages.TermsAndConditionsPage;
import org.keycloak.testsuite.rest.representation.AuthenticatorState;
import org.keycloak.testsuite.updaters.Creator;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ExecutionBuilder;
import org.keycloak.testsuite.util.FlowBuilder;
import org.keycloak.testsuite.util.RealmRepUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.util.Matchers.statusCodeIs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class CustomFlowTest extends AbstractFlowTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        UserRepresentation user = UserBuilder.create()
                                            .username("login-test")
                                            .email("login@test.com")
                                            .enabled(true)
                                            .build();
        testRealm.getUsers().add(user);

        // Set passthrough clientAuthenticator for our clients
        ClientRepresentation dummyClient = ClientBuilder.create()
                                              .clientId("dummy-client")
                                              .name("dummy-client")
                                              .authenticatorType(PassThroughClientAuthenticator.PROVIDER_ID)
                                              .directAccessGrants()
                                              .build();
        testRealm.getClients().add(dummyClient);

        ClientRepresentation testApp = RealmRepUtil.findClientByClientId(testRealm, "test-app");
        testApp.setClientAuthenticatorType(PassThroughClientAuthenticator.PROVIDER_ID);
        testApp.setDirectAccessGrantsEnabled(true);
    }

    @Before
    public void configureFlows() {
        userId = findUser("login-test").getId();

        // Do this just once per class
        if (testContext.isInitialized()) {
            return;
        }

        AuthenticationFlowRepresentation flow = FlowBuilder.create()
                                                           .alias("dummy")
                                                           .description("dummy pass through flow")
                                                           .providerId("basic-flow")
                                                           .topLevel(true)
                                                           .builtIn(false)
                                                           .build();
        testRealm().flows().createFlow(flow);

        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setBrowserFlow(flow.getAlias());
        realm.setDirectGrantFlow(flow.getAlias());
        testRealm().update(realm);

        // refresh flow to find its id
        flow = findFlowByAlias(flow.getAlias());

        AuthenticationExecutionRepresentation execution = ExecutionBuilder.create()
                                                            .parentFlow(flow.getId())
                                                            .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString())
                                                            .authenticator(PassThroughAuthenticator.PROVIDER_ID)
                                                            .priority(10)
                                                            .authenticatorFlow(false)
                                                            .build();
        testRealm().flows().addExecution(execution);

        flow = FlowBuilder.create()
                        .alias("dummy registration")
                        .description("dummy pass through registration")
                        .providerId("basic-flow")
                        .topLevel(true)
                        .builtIn(false)
                        .build();
        testRealm().flows().createFlow(flow);

        setRegistrationFlow(flow);

        // refresh flow to find its id
        flow = findFlowByAlias(flow.getAlias());

        execution = ExecutionBuilder.create()
                        .parentFlow(flow.getId())
                        .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString())
                        .authenticator(PassThroughRegistration.PROVIDER_ID)
                        .priority(10)
                        .authenticatorFlow(false)
                        .build();
        testRealm().flows().addExecution(execution);

        AuthenticationFlowRepresentation clientFlow = FlowBuilder.create()
                                                           .alias("client-dummy")
                                                           .description("dummy pass through flow")
                                                           .providerId(AuthenticationFlow.CLIENT_FLOW)
                                                           .topLevel(true)
                                                           .builtIn(false)
                                                           .build();
        testRealm().flows().createFlow(clientFlow);

        realm = testRealm().toRepresentation();
        realm.setClientAuthenticationFlow(clientFlow.getAlias());
        testRealm().update(realm);

        // refresh flow to find its id
        clientFlow = findFlowByAlias(clientFlow.getAlias());

        execution = ExecutionBuilder.create()
                        .parentFlow(clientFlow.getId())
                        .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString())
                        .authenticator(PassThroughClientAuthenticator.PROVIDER_ID)
                        .priority(10)
                        .authenticatorFlow(false)
                        .build();
        testRealm().flows().addExecution(execution);

        testContext.setInitialized(true);
    }


    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected TermsAndConditionsPage termsPage;


    @Page
    protected LoginPasswordUpdatePage updatePasswordPage;

    @Page
    protected RegisterPage registerPage;

    private static String userId;

    /**
     * KEYCLOAK-3506
     */
    @Test
    public void testRequiredAfterAlternative() {
        AuthenticationManagementResource authMgmtResource = testRealm().flows();
        Map<String, Object> params = new HashMap<>();
        String flowAlias = "Browser Flow With Extra";
        params.put("newName", flowAlias);
        Response response = authMgmtResource.copy("browser", params);
        String flowId = null;
        try {
            assertThat("Copy flow", response, statusCodeIs(Response.Status.CREATED));
            AuthenticationFlowRepresentation newFlow = findFlowByAlias(flowAlias);
            flowId = newFlow.getId();
        } finally {
            response.close();
        }

        AuthenticationExecutionRepresentation execution = ExecutionBuilder.create()
                .parentFlow(flowId)
                .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString())
                .authenticator(ClickThroughAuthenticator.PROVIDER_ID)
                .priority(10)
                .authenticatorFlow(false)
                .build();

        RealmRepresentation rep = testRealm().toRepresentation();
        try (Response r = testRealm().flows().addExecution(execution)) {
            rep.setBrowserFlow(flowAlias);
            testRealm().update(rep);
            rep = testRealm().toRepresentation();
            Assert.assertEquals(flowAlias, rep.getBrowserFlow());
        }


        loginPage.open();
         /* In the new flows, any required execution will render any optional flows unused.
        // test to make sure we aren't skipping anything
        loginPage.login("test-user@localhost", "bad-password");
        Assert.assertTrue(loginPage.isCurrent());
        loginPage.login("test-user@localhost", "password");*/
        Assert.assertTrue(termsPage.isCurrent());

        // Revert dummy flow
        rep.setBrowserFlow("dummy");
        testRealm().update(rep);
    }

    @Test
    public void validateX509FlowUpdate() throws Exception {
        String flowAlias = "Browser Flow With Extra 2";

        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(flowAlias);
        flow.setDescription("");
        flow.setProviderId("basic-flow");
        flow.setTopLevel(true);
        flow.setBuiltIn(false);

        try (Creator.Flow amr = Creator.create(testRealm(), flow)) {
            AuthenticationManagementResource authMgmtResource = amr.resource();

            //add execution - X509 username
            final AuthenticationExecutionInfoRepresentation execution = amr.addExecution(ValidateX509CertificateUsernameFactory.PROVIDER_ID);
            String executionId = execution.getId();

            Map<String, String> config = new HashMap<>();
            config.put(AbstractX509ClientCertificateAuthenticator.ENABLE_CRL, Boolean.TRUE.toString());
            AuthenticatorConfigRepresentation authConfig = new AuthenticatorConfigRepresentation();
            authConfig.setAlias("Config alias");
            authConfig.setConfig(config);

            String acId;
            try (Response resp = authMgmtResource.newExecutionConfig(executionId, authConfig)) {
                assertThat(resp, statusCodeIs(Status.CREATED));
                acId = ApiUtil.getCreatedId(resp);
            }

            authConfig = authMgmtResource.getAuthenticatorConfig(acId);
            authConfig.getConfig().put(AbstractX509ClientCertificateAuthenticator.ENABLE_CRL, Boolean.FALSE.toString());
            authConfig.getConfig().put(AbstractX509ClientCertificateAuthenticator.CRL_RELATIVE_PATH, "");

            authMgmtResource.updateAuthenticatorConfig(acId, authConfig);

            // Saving the same options for the second time would fail for CRL_RELATIVE_PATH on Oracle due to "" == NULL weirdness
            authMgmtResource.updateAuthenticatorConfig(acId, authConfig);
        }
    }

    @Test
    public void loginSuccess() {
        AuthenticatorState state = new AuthenticatorState();
        state.setUsername("login-test");
        state.setClientId("test-app");
        testingClient.testing().updateAuthenticator(state);

        oauth.openLoginForm();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void grantTest() throws Exception {
        AuthenticatorState state = new AuthenticatorState();
        state.setUsername("login-test");
        state.setClientId("test-app");
        testingClient.testing().updateAuthenticator(state);

        grantAccessToken("test-app", "login-test");
    }

    @Test
    public void clientAuthTest() throws Exception {
        AuthenticatorState state = new AuthenticatorState();
        state.setClientId("dummy-client");
        state.setUsername("login-test");
        testingClient.testing().updateAuthenticator(state);
        grantAccessToken("dummy-client", "login-test");

        state.setClientId("test-app");
        testingClient.testing().updateAuthenticator(state);
        grantAccessToken("test-app", "login-test");

        state.setClientId("unknown");
        testingClient.testing().updateAuthenticator(state);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user", "password");
        assertEquals(401, response.getStatusCode());
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

        state.setClientId("test-app");
        testingClient.testing().updateAuthenticator(state);

        // Test throwing exception from the client authenticator. No error details should be displayed
        response = oauth.passwordGrantRequest("test-user", "password").param(PassThroughClientAuthenticator.TEST_ERROR_PARAM, "Some Random Error").send();
        assertEquals(400, response.getStatusCode());
        assertEquals("unauthorized_client", response.getError());
        assertEquals("Unexpected error when authenticating client", response.getErrorDescription());
    }


    private void grantAccessToken(String clientId, String login) throws Exception {

        AccessTokenResponse response = oauth.doPasswordGrantRequest(login, "password");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

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

        AccessTokenResponse refreshedResponse = oauth.doRefreshTokenRequest(response.getRefreshToken());

        AccessToken refreshedAccessToken = oauth.verifyToken(refreshedResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshedResponse.getRefreshToken());

        assertEquals(accessToken.getSessionState(), refreshedAccessToken.getSessionState());
        assertEquals(accessToken.getSessionState(), refreshedRefreshToken.getSessionState());

        events.expectRefresh(refreshToken.getId(), refreshToken.getSessionState())
                .user(userId)
                .client(clientId)
                .detail(Details.CLIENT_AUTH_METHOD, PassThroughClientAuthenticator.PROVIDER_ID)
                .assertEvent();
    }


}
