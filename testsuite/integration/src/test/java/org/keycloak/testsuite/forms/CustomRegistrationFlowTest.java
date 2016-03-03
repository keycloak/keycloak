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
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.RealmModel;
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
public class CustomRegistrationFlowTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {
        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            AuthenticationFlowModel flow = new AuthenticationFlowModel();
            flow.setAlias("dummy registration");
            flow.setDescription("dummy pass through registration");
            flow.setProviderId("basic-flow");
            flow.setTopLevel(true);
            flow.setBuiltIn(false);
            flow = appRealm.addAuthenticationFlow(flow);
            appRealm.setRegistrationFlow(flow);

            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(flow.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(PassThroughRegistration.PROVIDER_ID);
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);
            appRealm.addAuthenticatorExecution(execution);


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
    public void registerUserSuccess() {
        loginPage.open();
        loginPage.clickRegister();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String userId = events.expectRegister(PassThroughRegistration.username, PassThroughRegistration.email).assertEvent().getUserId();
        events.expectLogin().detail("username", PassThroughRegistration.username).user(userId).assertEvent();
    }




}
