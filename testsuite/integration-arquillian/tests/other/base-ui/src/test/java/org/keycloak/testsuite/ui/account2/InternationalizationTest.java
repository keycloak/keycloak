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

package org.keycloak.testsuite.ui.account2;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.ui.account2.page.PersonalInfoPage;
import org.keycloak.testsuite.ui.account2.page.WelcomeScreen;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class InternationalizationTest extends AbstractAccountTest {
    @Page
    private WelcomeScreen welcomeScreen;

    @Page
    private PersonalInfoPage personalInfoPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        configureInternationalizationForRealm(testRealms.get(0));
    }

    @Before
    public void beforeI18nTest() {
        assertTestUserLocale(null);
        assertEquals(DEFAULT_LOCALE_NAME, welcomeScreen.header().getCurrentLocaleName());
    }

    @Test
    public void welcomeScreenTest() {
        welcomeScreen.header().selectLocale(CUSTOM_LOCALE);
        assertCustomLocaleWelcomeScreen();

        // check if selected locale is preserved
        welcomeScreen.clickPersonalInfoLink();
        assertCustomLocaleLoginPage();
        loginToAccount();
        assertTestUserLocale(CUSTOM_LOCALE);
        assertCustomLocalePersonalInfo();
    }

    @Test
    public void loggedInPageTest() {
        personalInfoPage.navigateTo();
        loginToAccount();
        assertTestUserLocale(null);
        assertEquals(DEFAULT_LOCALE_NAME, personalInfoPage.header().getCurrentLocaleName());
        personalInfoPage.header().selectLocale(CUSTOM_LOCALE);
        assertTestUserLocale(CUSTOM_LOCALE);
        assertCustomLocalePersonalInfo();

        // check if selected locale is preserved
        personalInfoPage.header().clickLogoutBtn();
        welcomeScreen.assertCurrent();
        assertCustomLocaleWelcomeScreen();
    }

    @Test
    public void loginFormTest() {
        personalInfoPage.navigateTo();
        loginPage.localeDropdown().selectAndAssert(CUSTOM_LOCALE_NAME);
        loginPage.form().login(testUser); // cannot use loginToAccount() because it asserts URL which is now different
        assertTestUserLocale(CUSTOM_LOCALE);
        assertCustomLocalePersonalInfo();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void userAttributeTest() {
        testUser.setAttributes(singletonMap(UserModel.LOCALE, singletonList(CUSTOM_LOCALE)));
        testUserResource().update(testUser);

        welcomeScreen.navigateTo();
        assertEquals(DEFAULT_LOCALE_NAME, welcomeScreen.header().getCurrentLocaleName());
        welcomeScreen.clickPersonalInfoLink();
        assertEquals(DEFAULT_LOCALE_NAME, loginPage.localeDropdown().getSelected());
        loginToAccount();
        assertCustomLocalePersonalInfo();
    }

    private void assertCustomLocaleWelcomeScreen() {
        welcomeScreen.header().assertLocaleVisible(true);
        assertEquals(CUSTOM_LOCALE_NAME, welcomeScreen.header().getCurrentLocaleName());
        assertEquals("Vítejte v Keycloaku", welcomeScreen.getWelcomeMessage());
    }

    private void assertCustomLocalePersonalInfo() {
        personalInfoPage.header().assertLocaleVisible(true);
        assertEquals(CUSTOM_LOCALE_NAME, personalInfoPage.header().getCurrentLocaleName());
        assertEquals("Osobní údaje", personalInfoPage.getPageTitle());
    }

    private void assertCustomLocaleLoginPage() {
        assertEquals(CUSTOM_LOCALE_NAME, loginPage.localeDropdown().getSelected());
    }

    private void assertTestUserLocale(String expectedLocale) {
        String actualLocale = null;
        List <String> userLocales = null;
        Map<String, List<String>> userAttributes = testUserResource().toRepresentation().getAttributes();

        if (userAttributes != null) {
            userLocales = userAttributes.get(UserModel.LOCALE);
            if (userLocales != null) {
                actualLocale = userLocales.get(0);
            }
        }

        assertEquals(expectedLocale, actualLocale);
    }
}
