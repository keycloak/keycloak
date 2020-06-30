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
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.ui.account2.page.PersonalInfoPage;
import org.keycloak.testsuite.ui.account2.page.SigningInPage;
import org.keycloak.testsuite.ui.account2.page.WelcomeScreen;
import org.keycloak.testsuite.util.WaitUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class InternationalizationTest extends AbstractAccountTest {
    @Page
    private WelcomeScreen welcomeScreen;

    @Page
    private PersonalInfoPage personalInfoPage;

    @Page
    private SigningInPage signingInPage;
    private SigningInPage.CredentialType passwordCredentialType;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        configureInternationalizationForRealm(testRealms.get(0));
    }

    @Before
    public void beforeI18nTest() {
        assertTestUserLocale(null);
        passwordCredentialType = signingInPage.getCredentialType(PasswordCredentialModel.TYPE);
    }

    @Test
    public void loggedInPageTest() {
        personalInfoPage.navigateTo();
        loginToAccount();
        assertTestUserLocale(null);
        personalInfoPage.selectLocale(CUSTOM_LOCALE);
        personalInfoPage.clickSave(false);
        WaitUtils.waitForPageToLoad();

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
    public void userAttributeTest() {
        testUser.setAttributes(singletonMap(UserModel.LOCALE, singletonList(CUSTOM_LOCALE)));
        testUserResource().update(testUser);

        welcomeScreen.navigateTo();
        welcomeScreen.clickPersonalInfoLink();
        assertEquals(DEFAULT_LOCALE_NAME, loginPage.localeDropdown().getSelected());
        loginToAccount();
        assertCustomLocalePersonalInfo();
    }

    @Test
    public void shouldDisplayTimeUsingSelectedLocale() {
        signingInPage.navigateTo();
        loginToAccount();
        SigningInPage.UserCredential passwordCred =
                passwordCredentialType.getUserCredential(testUserResource().credentials().get(0).getId());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy, h:mm a", Locale.ENGLISH);
        try {
            LocalDateTime.parse(passwordCred.getCreatedAtStr(), formatter);
        } catch (DateTimeParseException e) {
            fail("Time was not formatted with the locale");
        }

        signingInPage.header().clickLogoutBtn();
        signingInPage.navigateTo();
        loginPage.localeDropdown().selectAndAssert("Deutsch");
        loginPage.form().login(testUser);

        DateTimeFormatter formatterDe = DateTimeFormatter.ofPattern("d. MMMM yyyy, H:mm", Locale.GERMAN);

        try {
            LocalDateTime.parse(passwordCred.getCreatedAtStr(), formatterDe);
        } catch (DateTimeParseException e) {
            fail("Time was not formatted with the locale");
        }
    }

    private void assertCustomLocaleWelcomeScreen() {
        assertEquals("Vítejte v Keycloaku", welcomeScreen.getWelcomeMessage());
    }

    private void assertCustomLocalePersonalInfo() {
        assertEquals("Osobní údaje", personalInfoPage.getPageTitle());
    }

    private void assertCustomLocaleLoginPage() {
        assertEquals(CUSTOM_LOCALE_NAME, loginPage.localeDropdown().getSelected());
    }

    private void assertTestUserLocale(String expectedLocale) {
        String actualLocale = null;
        List <String> userLocales;
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
