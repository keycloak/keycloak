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

import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.TermsAndConditionsPage;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.UIUtils;
import org.keycloak.testsuite.util.UserBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TermsAndConditionsTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Drone
    @JavascriptBrowser
    private WebDriver jsDriver;

    @Page
    @JavascriptBrowser
    protected AppPage appPage;

    @Page
    @JavascriptBrowser
    protected LoginPage loginPage;

    @Page
    @JavascriptBrowser
    protected TermsAndConditionsPage termsPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void driver() {
        appPage.setDriver(jsDriver);
        termsPage.setDriver(jsDriver);
        loginPage.setDriver(jsDriver);
        DroneUtils.addWebDriver(jsDriver);
    }

    @Before
    public void addTermsAndConditionRequiredAction() {
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        UserBuilder.edit(user).requiredAction(TermsAndConditions.PROVIDER_ID);
        adminClient.realm("test").users().get(user.getId()).update(user);

        RequiredActionProviderRepresentation rep = adminClient.realm("test").flows().getRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name());
        rep.setEnabled(true);
        adminClient.realm("test").flows().updateRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name(), rep);
    }

    @Test
    public void termsAccepted() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(termsPage.isCurrent());

        termsPage.acceptTerms();

        events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION).removeDetail(Details.REDIRECT_URI).detail(Details.CUSTOM_REQUIRED_ACTION, TermsAndConditions.PROVIDER_ID).assertEvent();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();

        // assert user attribute is properly set
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        Map<String,List<String>> attributes = user.getAttributes();
        assertNotNull("timestamp for terms acceptance was not stored in user attributes", attributes);
        List<String> termsAndConditions = attributes.get(TermsAndConditions.USER_ATTRIBUTE);
        assertTrue("timestamp for terms acceptance was not stored in user attributes as "
                + TermsAndConditions.USER_ATTRIBUTE, termsAndConditions.size() == 1);
        String timestamp = termsAndConditions.get(0);
        assertNotNull("expected non-null timestamp for terms acceptance in user attribute "
                + TermsAndConditions.USER_ATTRIBUTE, timestamp);
        try {
            Integer.valueOf(timestamp);
        } catch (NumberFormatException e) {
            fail("timestamp for terms acceptance is not a valid integer: '" + timestamp + "'");
        }
    }

    @Test
    public void termsDeclined() {
        appPage.open();
        appPage.openAccount();

        loginPage.assertCurrent();

        loginPage.login("test-user@localhost", "password");

        Assert.assertTrue(termsPage.isCurrent());

        termsPage.declineTerms();

        events.expectLogin().event(EventType.CUSTOM_REQUIRED_ACTION_ERROR).detail(Details.CUSTOM_REQUIRED_ACTION, TermsAndConditions.PROVIDER_ID)
                .error(Errors.REJECTED_BY_USER)
                .removeDetail(Details.CONSENT)
                .session(Matchers.nullValue(String.class))
                .client(Constants.ACCOUNT_CONSOLE_CLIENT_ID)
                .detail(Details.REDIRECT_URI, getAuthServerContextRoot() + "/auth/realms/" + TEST_REALM_NAME + "/account")
                .assertEvent();


        // assert user attribute is properly removed
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        Map<String,List<String>> attributes = user.getAttributes();
        if (attributes != null) {
            assertNull("expected null for terms acceptance user attribute " + TermsAndConditions.USER_ATTRIBUTE,
                    attributes.get(TermsAndConditions.USER_ATTRIBUTE));
        }
        assertThat(DroneUtils.getCurrentDriver().getTitle(), equalTo("Account Management"));
        Assert.assertTrue(DroneUtils.getCurrentDriver().getPageSource().contains("You need to agree to Terms and Conditions"));
        Assert.assertFalse(DroneUtils.getCurrentDriver().getPageSource().contains("An unexpected error occurred"));

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

        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        assertTrue(appPage.isCurrent());

        events.expectLogin().assertEvent();

    }

}
