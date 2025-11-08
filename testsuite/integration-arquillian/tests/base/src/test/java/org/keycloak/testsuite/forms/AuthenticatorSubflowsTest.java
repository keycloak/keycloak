/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.testsuite.util.UIUtils;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;


/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticatorSubflowsTest extends AbstractChangeImportedUserPasswordsTest {

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

            // Subflow1 - foo=bar1
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(subflow1.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(ExpectedParamAuthenticatorFactory.PROVIDER_ID);
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);

            AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
            configModel.setAlias("bar1");
            Map<String, String> config = new HashMap<>();
            config.put(ExpectedParamAuthenticator.EXPECTED_VALUE, "bar1");
            configModel.setConfig(config);
            configModel = realm.addAuthenticatorConfig(configModel);
            execution.setAuthenticatorConfig(configModel.getId());

            realm.addAuthenticatorExecution(execution);

            // Subflow1 - username password
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(subflow1.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(UsernamePasswordFormFactory.PROVIDER_ID);
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);

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

            // Subflow2 - username-password
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(subflow2.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(UsernamePasswordFormFactory.PROVIDER_ID);
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);

            realm.addAuthenticatorExecution(execution);

            // Subflow3
            AuthenticationFlowModel subflow3 = new AuthenticationFlowModel();
            subflow3.setTopLevel(false);
            subflow3.setBuiltIn(true);
            subflow3.setAlias("subflow-3");
            subflow3.setDescription("alternative subflow with child subflows");
            subflow3.setProviderId("basic-flow");
            subflow3 = realm.addAuthenticationFlow(subflow3);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(browser.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setFlowId(subflow3.getId());
            execution.setPriority(30);
            execution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(execution);

            // Subflow3-1
            AuthenticationFlowModel subflow31 = new AuthenticationFlowModel();
            subflow31.setTopLevel(false);
            subflow31.setBuiltIn(true);
            subflow31.setAlias("subflow-31");
            subflow31.setDescription("subflow-31");
            subflow31.setProviderId("basic-flow");
            subflow31 = realm.addAuthenticationFlow(subflow31);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(subflow3.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setFlowId(subflow31.getId());
            execution.setPriority(10);
            execution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(execution);

            // Subflow3-1 - foo=bar2
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(subflow31.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(ExpectedParamAuthenticatorFactory.PROVIDER_ID);
            execution.setPriority(10);
            execution.setAuthenticatorFlow(false);

            configModel = new AuthenticatorConfigModel();
            configModel.setAlias("bar2");
            config = new HashMap<>();
            config.put(ExpectedParamAuthenticator.EXPECTED_VALUE, "bar2");
            config.put(ExpectedParamAuthenticator.LOGGED_USER, "john-doh@localhost");
            configModel.setConfig(config);
            configModel = realm.addAuthenticatorConfig(configModel);
            execution.setAuthenticatorConfig(configModel.getId());

            realm.addAuthenticatorExecution(execution);

            // Subflow3-1 - push the button
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(subflow31.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator(PushButtonAuthenticatorFactory.PROVIDER_ID);
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);

            realm.addAuthenticatorExecution(execution);

            // Subflow3  - foo=bar3
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(subflow3.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setAuthenticator(ExpectedParamAuthenticatorFactory.PROVIDER_ID);
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);

            configModel = new AuthenticatorConfigModel();
            configModel.setAlias("bar3");
            config = new HashMap<>();
            config.put(ExpectedParamAuthenticator.EXPECTED_VALUE, "bar3");
            config.put(ExpectedParamAuthenticator.LOGGED_USER, "keycloak-user@localhost");
            configModel.setConfig(config);
            configModel = realm.addAuthenticatorConfig(configModel);
            execution.setAuthenticatorConfig(configModel.getId());

            realm.addAuthenticatorExecution(execution);


        });
    }


//    @Test
//    public void testSleep() throws Exception {
//        log.info("Start sleeping");
//        Thread.sleep(1000000);
//    }


    @Test
    public void testSubflow1() throws Exception {
        // Add foo=bar1 . I am redirected to subflow1 - username+password form
        oauth.loginForm().param("foo", "bar1").open();

        loginPage.assertCurrent();

        // Fill username+password. I am successfully authenticated
        oauth.fillLoginForm("test-user@localhost", getPassword("test-user@localhost"));
        appPage.assertCurrent();

        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }


    @Test
    public void testSubflow2() throws Exception {
        // Don't add 'foo' parameter. I am redirected to subflow2 - push the button
        oauth.loginForm().open();

        Assert.assertEquals("PushTheButton", driver.getTitle());

        // Push the button. I am redirected to username+password form
        UIUtils.clickLink(driver.findElement(By.name("submit1")));


        loginPage.assertCurrent();

        // Fill username+password. I am successfully authenticated
        oauth.fillLoginForm("test-user@localhost", getPassword("test-user@localhost"));
        appPage.assertCurrent();

        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }


//    @Test
//    public void testSubflow31() {
//        // Fill foo=bar2. I am see the pushButton
//        String loginFormUrl = oauth.getLoginFormUrl();
//        loginFormUrl = loginFormUrl + "&foo=bar2";
//        log.info("loginFormUrl: " + loginFormUrl);
//
//        //Thread.sleep(10000000);
//
//        driver.navigate().to(loginFormUrl);
//        Assert.assertEquals("PushTheButton", driver.getTitle());
//
//        // Confirm push button. I am authenticated as john-doh@localhost
//        driver.findElement(By.name("submit1")).click();
//
//        appPage.assertCurrent();
//
//        events.expectLogin().detail(Details.USERNAME, "john-doh@localhost").assertEvent();
//    }
//
//
//    @Test
//    public void testSubflow32() {
//        // Fill foo=bar3. I am login automatically as "keycloak-user@localhost"
//
//
//    }


}
