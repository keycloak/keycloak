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

package org.keycloak.testsuite.forms;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.authentication.ExpectedParamAuthenticator;
import org.keycloak.testsuite.authentication.ExpectedParamAuthenticatorFactory;
import org.keycloak.testsuite.authentication.PushButtonAuthenticatorFactory;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;

/**
 * @author <a href="mailto:n1330@me.com">Tomohiro Nagai</a>
 */
public class AuthenticatorSubflowsTest2 extends AbstractChangeImportedUserPasswordsTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Before
    public void setupFlows() {
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


            // Subflow1
            AuthenticationFlowModel subflow1 = new AuthenticationFlowModel();
            subflow1.setTopLevel(false);
            subflow1.setBuiltIn(true);
            subflow1.setAlias("subflow-1");
            subflow1.setDescription("Parameter 'foo=bar1' AND username+password");
            subflow1.setProviderId("basic-flow");
            subflow1 = realm.addAuthenticationFlow(subflow1);

            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(browser.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setFlowId(subflow1.getId());
            execution.setPriority(10);
            execution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(execution);

            // Subflow1 - username password
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(subflow1.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(UsernamePasswordFormFactory.PROVIDER_ID);
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);

            realm.addAuthenticatorExecution(execution);

            // Subflow1 - foo=bar1
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(subflow1.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(ExpectedParamAuthenticatorFactory.PROVIDER_ID);
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);

            AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
            configModel.setAlias("bar1");
            Map<String, String> config = new HashMap<>();
            config.put(ExpectedParamAuthenticator.EXPECTED_VALUE, "bar1");
            configModel.setConfig(config);
            configModel = realm.addAuthenticatorConfig(configModel);
            execution.setAuthenticatorConfig(configModel.getId());

            realm.addAuthenticatorExecution(execution);


            // Subflow2
            AuthenticationFlowModel subflow2 = new AuthenticationFlowModel();
            subflow2.setTopLevel(false);
            subflow2.setBuiltIn(true);
            subflow2.setAlias("subflow-2");
            subflow2.setDescription("username+password AND pushButton");
            subflow2.setProviderId("basic-flow");
            subflow2 = realm.addAuthenticationFlow(subflow2);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(browser.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setFlowId(subflow2.getId());
            execution.setPriority(20);
            execution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(execution);

            // Subflow2 - push the button
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(subflow2.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(PushButtonAuthenticatorFactory.PROVIDER_ID);
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);

            realm.addAuthenticatorExecution(execution);
        });
    }


    @Test
    public void testSubflow1() throws Exception {
        // Add foo=bar1. I am redirected to subflow1 - username+password form.
        oauth.loginForm().param("foo", "bar1").open();

        loginPage.assertCurrent();

        // Fill username+password. I am successfully authenticated.
        oauth.fillLoginForm("test-user@localhost", getPassword("test-user@localhost"));
        appPage.assertCurrent();

        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }


    @Test
    public void testSubflow2() throws Exception {
        // Don't add 'foo' parameter. I am redirected to subflow1 - username+password form, then move to subflow2.
        oauth.openLoginForm();

        loginPage.assertCurrent();

        // Fill username+password. I am redirected push the button.
        oauth.fillLoginForm("test-user@localhost", getPassword("test-user@localhost"));
        Assert.assertEquals("PushTheButton", driver.getTitle());

        // Push the button. I am successfully authenticated.
        driver.findElement(By.name("submit1")).click();
        appPage.assertCurrent();

        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

}
