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
package org.keycloak.testsuite.sessionlimits;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

@AuthServerContainerExclude(REMOTE)
public class UserSessionLimitsTest extends AbstractTestRealmKeycloakTest {

    private static final String LOGINTEST1 = "login-test-1";
    private static final String PASSWORD1 = "password1";

    private static final String ERROR_TO_DISPLAY = "This account has too many sessions";

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        UserRepresentation user1 = UserBuilder.create()
                .id(LOGINTEST1)
                .username(LOGINTEST1)
                .email("login1@test.com")
                .enabled(true)
                .password(PASSWORD1)
                .build();
        RealmBuilder.edit(testRealm).user(user1);
    }

    @Before
    public void setupFlows() {
        // Do this just once per class
        if (testContext.isInitialized()) {
            return;
        }
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");

            if (realm.getBrowserFlow().getAlias().equals("parent-flow")) {
                return;
            }
            // Parent flow
            AuthenticationFlowModel browser = new AuthenticationFlowModel();
            browser.setAlias("parent-flow");
            browser.setDescription("browser based authentication");
            browser.setProviderId("basic-flow");
            browser.setTopLevel(true);
            browser.setBuiltIn(true);
            browser = realm.addAuthenticationFlow(browser);
            realm.setBrowserFlow(browser);

            //  username password
            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(browser.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(UsernamePasswordFormFactory.PROVIDER_ID);
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);

            // user session limits authenticator
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(browser.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(UserSessionLimitsAuthenticatorFactory.USER_SESSION_LIMITS);
            execution.setPriority(30);
            execution.setAuthenticatorFlow(false);

            AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
            Map<String, String> sessionAuthenticatorConfig = new HashMap<>();
            sessionAuthenticatorConfig.put(UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
            sessionAuthenticatorConfig.put(UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            sessionAuthenticatorConfig.put(UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
            sessionAuthenticatorConfig.put(UserSessionLimitsAuthenticatorFactory.ERROR_MESSAGE, ERROR_TO_DISPLAY);
            configModel.setConfig(sessionAuthenticatorConfig);
            configModel.setAlias("user-session-limits");
            configModel = realm.addAuthenticatorConfig(configModel);
            execution.setAuthenticatorConfig(configModel.getId());
            realm.addAuthenticatorExecution(execution);
        });
        testContext.setInitialized(true);
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginPage loginPage;
    @Page
    protected AppPage appPage;
    @Page
    protected ErrorPage errorPage;

    @Test
    public void testSessionCountExceededAndNewSessionDenied() throws InterruptedException {
        // Login and verify login was succesfull
        loginPage.open();
        loginPage.login(LOGINTEST1, PASSWORD1);
        appPage.assertCurrent();
        appPage.openAccount();
        
        // Delete the cookies, while maintaining the server side session active
        super.deleteCookies();
        
        // Login the same user again and verify the configured error message is shown
        loginPage.open();
        loginPage.login(LOGINTEST1, PASSWORD1);
        errorPage.assertCurrent();

        Assert.assertEquals(ERROR_TO_DISPLAY, errorPage.getError());
    }

}
