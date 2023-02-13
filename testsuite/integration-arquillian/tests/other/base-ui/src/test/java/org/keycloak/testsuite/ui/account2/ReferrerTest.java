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
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.ui.account2.page.PersonalInfoPage;
import org.keycloak.testsuite.ui.account2.page.WelcomeScreen;
import org.keycloak.testsuite.ui.account2.page.fragment.AbstractHeader;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ReferrerTest extends AbstractAccountTest {
    public static final String FAKE_CLIENT_ID = "fake-client-name";
    public static final String REFERRER_LINK_TEXT = "Back to " + LOCALE_CLIENT_NAME_LOCALIZED;
    public static final String FAKE_CLIENT_URL_CONTEXT = "auth/non-existing-page/";
    public static final String FAKE_CLIENT_URL_FRAGMENT = "?foo=bar&bar=foo#anchor";

    @Page
    private WelcomeScreen welcomeScreen;

    @Page
    private PersonalInfoPage personalInfoPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        RealmRepresentation testRealm = testRealms.get(0);

        ClientRepresentation testClient = new ClientRepresentation();
        testClient.setClientId(FAKE_CLIENT_ID);
        testClient.setName(LOCALE_CLIENT_NAME);

        // Redirect URIs are no longer allowed to contain a fragment, so we
        // need the wildcard in order to use fragments in tests
        testClient.setRedirectUris(Collections.singletonList(getFakeClientUrl("*")));

        testClient.setEnabled(true);

        testRealm.setClients(Collections.singletonList(testClient));
        testRealm.setAccountTheme(LOCALIZED_THEME_PREVIEW); // using localized custom theme for the fake client localized name
    }

    @Test
    // https://issues.redhat.com/browse/KEYCLOAK-17033
    // If the referrer is unescaped, this test will throw an exception.
    // org.openqa.selenium.UnhandledAlertException: unexpected alert open: {Alert text : XSS}
    public void reflectedXSSTest() {
        String attackUrl = getFakeClientUrl("'+alert('XSS')+'");
        welcomeScreen.navigateTo(FAKE_CLIENT_ID, attackUrl);

        welcomeScreen.header().clickLoginBtn();
        loginToAccount();
        welcomeScreen.clickPersonalInfoLink();
    }

    @Test
    public void loggedInWelcomeScreenTest() {
        welcomeScreen.header().clickLoginBtn();
        loginToAccount();

        welcomeScreen.navigateTo(FAKE_CLIENT_ID, getFakeClientUrl(FAKE_CLIENT_URL_FRAGMENT));
        welcomeScreen.header().assertLoginBtnVisible(false);
        welcomeScreen.header().assertLogoutBtnVisible(true);

        testReferrer(welcomeScreen.header(), true);
    }

    @Test
    public void loggedOutWelcomeScreenTest() {
        welcomeScreen.navigateTo(FAKE_CLIENT_ID, getFakeClientUrl(FAKE_CLIENT_URL_FRAGMENT));
        welcomeScreen.header().assertLoginBtnVisible(true);
        welcomeScreen.header().assertLogoutBtnVisible(false);

        testReferrer(welcomeScreen.header(), true);
    }

    @Test
    public void loggedInPageTest() {
        welcomeScreen.header().clickLoginBtn();
        loginToAccount();

        welcomeScreen.navigateTo(FAKE_CLIENT_ID, getFakeClientUrl(FAKE_CLIENT_URL_FRAGMENT));
        welcomeScreen.clickPersonalInfoLink();

        testReferrer(personalInfoPage.header(), true);
    }

    @Test
    public void loggedOutPageTest() {
        welcomeScreen.navigateTo(FAKE_CLIENT_ID, getFakeClientUrl(FAKE_CLIENT_URL_FRAGMENT));
        welcomeScreen.clickPersonalInfoLink();
        loginToAccount();

        testReferrer(personalInfoPage.header(), true);
    }

    @Test
    public void badClientNameTest() {
        welcomeScreen.navigateTo(FAKE_CLIENT_ID + "-bad", getFakeClientUrl(FAKE_CLIENT_URL_FRAGMENT));
        testReferrer(welcomeScreen.header(), false);

        welcomeScreen.navigateTo(FAKE_CLIENT_ID + "-bad", getFakeClientUrl(FAKE_CLIENT_URL_FRAGMENT));
        welcomeScreen.clickPersonalInfoLink();
        loginToAccount();
        testReferrer(personalInfoPage.header(), false);
    }

    @Test
    public void badClientUriTest() {
        welcomeScreen.navigateTo(FAKE_CLIENT_ID, getFakeClientUrlWithBadContext());
        testReferrer(welcomeScreen.header(), false);

        welcomeScreen.navigateTo(FAKE_CLIENT_ID, getFakeClientUrlWithBadContext());
        welcomeScreen.clickPersonalInfoLink();
        loginToAccount();
        testReferrer(personalInfoPage.header(), false);
    }

    private void testReferrer(AbstractHeader header, boolean expectReferrerVisible) {
        if (expectReferrerVisible) {
            assertEquals(REFERRER_LINK_TEXT, header.getReferrerLinkText());
            header.clickReferrerLink();
            assertCurrentUrlEquals(getFakeClientUrl(FAKE_CLIENT_URL_FRAGMENT));
        }
        else {
            header.assertReferrerLinkVisible(false);
        }
    }

    private String getFakeClientUrl(String suffix) {
        // we need to use some page which host exists – Firefox is throwing exceptions like crazy if we try to load
        // a page on a non-existing host, like e.g. http://non-existing-server/
        // also we need to do this here as getAuthServerRoot is not ready when firing this class' constructor
         return getAuthServerRoot() + FAKE_CLIENT_URL_CONTEXT + suffix;
    }

    private String getFakeClientUrlWithBadContext() {
        // we need to use some page which host exists – Firefox is throwing exceptions like crazy if we try to load
        // a page on a non-existing host, like e.g. http://non-existing-server/
        // also we need to do this here as getAuthServerRoot is not ready when firing this class' constructor
         return getAuthServerRoot() + "bad/" + FAKE_CLIENT_URL_CONTEXT;
    }
}
