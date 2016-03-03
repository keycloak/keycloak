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
package org.keycloak.testsuite.actions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.rule.GreenMailRule;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.KeycloakRule.KeycloakSetup;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequiredActionResetPasswordTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel defaultRealm, RealmModel appRealm) {
            appRealm.setResetPasswordAllowed(true);

            UserModel user = manager.getSession().users().getUserByUsername("test-user@localhost", appRealm);
            user.addRequiredAction(RequiredAction.UPDATE_PASSWORD);
        }

    });

    @Rule
    public WebRule webRule = new WebRule(this);

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected LoginPasswordUpdatePage changePasswordPage;

    @Before
    public void before() {
        oauth.state("mystate"); // have to set this as keycloak validates that state is sent
    }


    @Test
    public void tempPassword() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("new-password", "new-password");

        String sessionId = events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent().getSessionId();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        Event loginEvent = events.expectLogin().session(sessionId).assertEvent();

        oauth.openLogout();

        events.expectLogout(loginEvent.getSessionId()).assertEvent();

        loginPage.open();
        loginPage.login("test-user@localhost", "new-password");

        events.expectLogin().assertEvent();
    }

}
