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
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.auth.page.account2.ApplicationsPage;
import org.keycloak.testsuite.auth.page.account2.AuthenticatorPage;
import org.keycloak.testsuite.auth.page.account2.ChangePasswordPage;
import org.keycloak.testsuite.auth.page.account2.DeviceActivityPage;
import org.keycloak.testsuite.auth.page.account2.LinkedAccountsPage;
import org.keycloak.testsuite.auth.page.account2.PersonalInfoPage;
import org.keycloak.testsuite.auth.page.account2.ResourcesPage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class WelcomeScreenTest extends AbstractAccountTest {
    @Page
    private PersonalInfoPage personalInfoPage;
    @Page
    private ChangePasswordPage changePasswordPage;
    @Page
    private AuthenticatorPage authenticatorPage;
    @Page
    private DeviceActivityPage deviceActivityPage;
    @Page
    private LinkedAccountsPage linkedAccountsPage;
    @Page
    private ApplicationsPage applicationsPage;
    @Page
    private ResourcesPage resourcesPage;

    @Test
    public void loginTest() {
        accountWelcomeScreen.assertCurrent();
        assertTrue(accountWelcomeScreen.isLoginBtnVisible());

        // login
        accountWelcomeScreen.clickLoginBtn();
        loginToAccount();
        accountWelcomeScreen.assertCurrent();
        assertFalse(accountWelcomeScreen.isLoginBtnVisible());

        // TODO logout test (blocked by KEYCLOAK-8084)
    }

    @Test
    public void personalInfoTest() {
        assertTrue(accountWelcomeScreen.personalInfo().isVisible());
        accountWelcomeScreen.personalInfo().clickPersonalInfo();
        loginToAccount();
        personalInfoPage.assertCurrent();
    }

    @Test
    public void accountSecurityTest() {
        assertTrue(accountWelcomeScreen.accountSecurityCard().isVisible());

        // change password link
        accountWelcomeScreen.accountSecurityCard().clickChangePassword();
        loginToAccount();
        changePasswordPage.assertCurrent();

        // authenticator link
        accountWelcomeScreen.navigateTo();
        accountWelcomeScreen.accountSecurityCard().clickAuthenticator();
        authenticatorPage.assertCurrent();

        // device activity link
        accountWelcomeScreen.navigateTo();
        accountWelcomeScreen.accountSecurityCard().clickDeviceActivity();
        deviceActivityPage.assertCurrent();

        // linked accounts link
        accountWelcomeScreen.navigateTo();
        assertFalse(accountWelcomeScreen.accountSecurityCard().isLinkedAccountsVisible());
        // add simple IdP
        testRealmResource().identityProviders().create(createIdentityProviderRepresentation("test-idp", "test-provider"));
        // test link appeared
        accountWelcomeScreen.navigateTo();
        accountWelcomeScreen.accountSecurityCard().clickLinkedAccounts();
        linkedAccountsPage.assertCurrent();
        // no need to remove the IdP
    }

    @Test
    public void applicationsTest() {
        assertTrue(accountWelcomeScreen.applicationsCard().isVisible());
        accountWelcomeScreen.applicationsCard().clickApplicationsLink();
        loginToAccount();
        applicationsPage.assertCurrent();
    }

    @Test
    public void resourcesTest() {
        assertFalse(accountWelcomeScreen.myResourcesCard().isVisible());

        // set user managed access
        RealmRepresentation testRealm = testRealmResource().toRepresentation();
        testRealm.setUserManagedAccessAllowed(true);
        testRealmResource().update(testRealm);

        // test my resources appeared
        accountWelcomeScreen.navigateTo();
        assertTrue(accountWelcomeScreen.myResourcesCard().isVisible());
        accountWelcomeScreen.myResourcesCard().clickMyResources();
        loginToAccount();
        resourcesPage.assertCurrent();
        // no need to disable user managed access
    }
}
