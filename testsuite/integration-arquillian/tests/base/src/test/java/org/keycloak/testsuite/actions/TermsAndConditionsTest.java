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

import java.util.List;
import java.util.Map;

import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.TermsAndConditionsPage;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.UIUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TermsAndConditionsTest extends AbstractChangeImportedUserPasswordsTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected TermsAndConditionsPage termsPage;

    @Before
    public void addTermsAndConditionRequiredAction() {
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        UserBuilder.update(user).requiredActions(TermsAndConditions.PROVIDER_ID);
        adminClient.realm("test").users().get(user.getId()).update(user);

        RequiredActionProviderRepresentation rep = adminClient.realm("test").flows().getRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name());
        rep.setEnabled(true);
        adminClient.realm("test").flows().updateRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name(), rep);
    }

    @Test
    public void termsAccepted() {
        oauth.openLoginForm();

        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

        Assertions.assertTrue(termsPage.isCurrent());

        termsPage.acceptTerms();

        EventAssertion.expectRequiredAction(events.poll()).type(EventType.CUSTOM_REQUIRED_ACTION).withoutDetails(Details.REDIRECT_URI).details(Details.CUSTOM_REQUIRED_ACTION, TermsAndConditions.PROVIDER_ID);

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        Assertions.assertNotNull(response.getCode());

        EventAssertion.expectLoginSuccess(events.poll());

        // assert user attribute is properly set
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        Map<String,List<String>> attributes = user.getAttributes();
        assertNotNull(attributes, "timestamp for terms acceptance was not stored in user attributes");
        List<String> termsAndConditions = attributes.get(TermsAndConditions.USER_ATTRIBUTE);
        assertTrue(termsAndConditions.size() == 1, "timestamp for terms acceptance was not stored in user attributes as "
                + TermsAndConditions.USER_ATTRIBUTE);
        String timestamp = termsAndConditions.get(0);
        assertNotNull(timestamp, "expected non-null timestamp for terms acceptance in user attribute "
                + TermsAndConditions.USER_ATTRIBUTE);
        try {
            Integer.valueOf(timestamp);
        } catch (NumberFormatException e) {
            fail("timestamp for terms acceptance is not a valid integer: '" + timestamp + "'");
        }
    }

    @Test
    public void termsDeclined() throws Exception {
        oauth.openLoginForm();
        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
        Assertions.assertTrue(termsPage.isCurrent());

        termsPage.declineTerms();
        WaitUtils.waitForPageToLoad();

        // assert on app page with reject login
        Assertions.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        Assertions.assertNull(response.getCode());
        Assertions.assertEquals(Errors.ACCESS_DENIED, response.getError());
        Assertions.assertEquals(Messages.TERMS_AND_CONDITIONS_DECLINED, response.getErrorDescription());

        // assert event
        EventAssertion.assertError(events.poll()).type(EventType.CUSTOM_REQUIRED_ACTION_ERROR).details(Details.CUSTOM_REQUIRED_ACTION, TermsAndConditions.PROVIDER_ID)
                .error(Errors.REJECTED_BY_USER)
                .withoutDetails(Details.CONSENT)
                .sessionId(null);

        // assert user attribute is properly removed
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        Map<String,List<String>> attributes = user.getAttributes();
        if (attributes != null) {
            assertNull(attributes.get(TermsAndConditions.USER_ATTRIBUTE),
                    "expected null for terms acceptance user attribute " + TermsAndConditions.USER_ATTRIBUTE);
        }
    }

    @Test // only for firefox and chrome as it needs to go to the account console
    @IgnoreBrowserDriver(value={ChromeDriver.class, FirefoxDriver.class}, negate=true)
    public void termsDeclinedAccount() {
        appPage.open();
        appPage.openAccount();

        loginPage.assertCurrent();

        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

        Assertions.assertTrue(termsPage.isCurrent());

        termsPage.declineTerms();

        WaitUtils.waitForPageToLoad();

        EventAssertion.assertError(events.poll()).type(EventType.CUSTOM_REQUIRED_ACTION_ERROR).details(Details.CUSTOM_REQUIRED_ACTION, TermsAndConditions.PROVIDER_ID)
                .error(Errors.REJECTED_BY_USER)
                .withoutDetails(Details.CONSENT)
                .sessionId(null)
                .clientId(Constants.ACCOUNT_CONSOLE_CLIENT_ID)
                .details(Details.REDIRECT_URI, getAuthServerContextRoot() + "/auth/realms/" + TEST_REALM_NAME + "/account");


        // assert user attribute is properly removed
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        Map<String,List<String>> attributes = user.getAttributes();
        if (attributes != null) {
            assertNull(attributes.get(TermsAndConditions.USER_ATTRIBUTE),
                    "expected null for terms acceptance user attribute " + TermsAndConditions.USER_ATTRIBUTE);
        }
        assertThat(DroneUtils.getCurrentDriver().getTitle(), equalTo("Account Management"));
        Assertions.assertTrue(DroneUtils.getCurrentDriver().getPageSource().contains("Access denied"));
        Assertions.assertFalse(DroneUtils.getCurrentDriver().getPageSource().contains("An unexpected error occurred"));

        WebElement tryAgainButton = DroneUtils.getCurrentDriver().findElement(By.tagName("button"));
        assertThat(tryAgainButton.getText(), equalTo("Try again"));
        UIUtils.click(tryAgainButton);

        loginPage.assertCurrent();
    }


    @Test
    // KEYCLOAK-3192
    public void termsDisabled() {
        RequiredActionProviderRepresentation rep = adminClient.realm("test").flows().getRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name());
        rep.setEnabled(false);
        adminClient.realm("test").flows().updateRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name(), rep);

        oauth.openLoginForm();

        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

        assertTrue(appPage.isCurrent());

        EventAssertion.expectLoginSuccess(events.poll());

    }

}
