/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.ui.account2;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.testsuite.ui.account2.page.ApplicationsPage;
import org.keycloak.testsuite.ui.account2.page.DeviceActivityPage;
import org.keycloak.testsuite.ui.account2.page.LinkedAccountsPage;
import org.keycloak.testsuite.ui.account2.page.PersonalInfoPage;

import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class WelcomeScreenTest extends AbstractAccountTest {
    @Page
    private PersonalInfoPage personalInfoPage;
    @Page
    private DeviceActivityPage deviceActivityPage;
    @Page
    private LinkedAccountsPage linkedAccountsPage;
    @Page
    private ApplicationsPage applicationsPage;

    @Test
    public void loginLogoutTest() {
        accountWelcomeScreen.assertCurrent();
        accountWelcomeScreen.header().assertLogoutBtnVisible(false);
        accountWelcomeScreen.header().assertLocaleVisible(false);

        // login
        accountWelcomeScreen.header().clickLoginBtn();
        loginToAccount();
        accountWelcomeScreen.assertCurrent();
        accountWelcomeScreen.header().assertLoginBtnVisible(false);

        // try if we're really logged in
        personalInfoPage.navigateTo();
        personalInfoPage.assertCurrent();
        accountWelcomeScreen.navigateTo();
        accountWelcomeScreen.assertCurrent();

        // logout
        accountWelcomeScreen.header().assertLoginBtnVisible(false);
        accountWelcomeScreen.header().clickLogoutBtn();
        accountWelcomeScreen.assertCurrent();
        accountWelcomeScreen.header().assertLogoutBtnVisible(false);
        accountWelcomeScreen.header().assertLoginBtnVisible(true);
        personalInfoPage.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(personalInfoPage);
    }

    @Test
    public void personalInfoTest() {
        accountWelcomeScreen.clickPersonalInfoLink();
        loginToAccount();
        personalInfoPage.assertCurrent();
    }

    @Test
    public void accountSecurityTest() {
        // TODO rewrite this! (KEYCLOAK-12105)
//        // change password link
//        accountWelcomeScreen.accountSecurityCard().clickChangePassword();
//        loginToAccount();
//        changePasswordPage.assertCurrent();
//
//        // authenticator link
//        accountWelcomeScreen.navigateTo();
//        accountWelcomeScreen.accountSecurityCard().clickAuthenticator();
//        authenticatorPage.assertCurrent();

        // device activity link
        accountWelcomeScreen.navigateTo();
        accountWelcomeScreen.clickDeviceActivityLink();
        loginToAccount();
        deviceActivityPage.assertCurrent();

        // linked accounts nav item (this doesn't test welcome page directly but the sidebar after login)
        personalInfoPage.navigateTo();
        personalInfoPage.sidebar().assertNavNotPresent(LinkedAccountsPage.LINKED_ACCOUNTS_ID);

        // linked accounts link
        accountWelcomeScreen.navigateTo();
        accountWelcomeScreen.assertLinkedAccountsLinkVisible(false);
        // add simple IdP
        testRealmResource().identityProviders().create(createIdentityProviderRepresentation("test-idp", "test-provider"));
        // test link appeared
        accountWelcomeScreen.navigateTo();
        accountWelcomeScreen.clickLinkedAccountsLink();
        linkedAccountsPage.assertCurrent();
        // no need to remove the IdP
    }

    @Test
    public void applicationsTest() {
        accountWelcomeScreen.clickApplicationsLink();
        loginToAccount();
        applicationsPage.assertCurrent();
    }

//    @Test
//    public void resourcesTest() {
//        assertFalse(accountWelcomeScreen.myResourcesCard().isVisible());
//
//        // set user managed access
//        RealmRepresentation testRealm = testRealmResource().toRepresentation();
//        testRealm.setUserManagedAccessAllowed(true);
//        testRealmResource().update(testRealm);
//
//        // test my resources appeared
//        accountWelcomeScreen.navigateTo();
//        assertTrue(accountWelcomeScreen.myResourcesCard().isVisible());
//        accountWelcomeScreen.myResourcesCard().clickMyResources();
//        loginToAccount();
//        resourcesPage.assertCurrent();
//        // no need to disable user managed access
//    }
}
