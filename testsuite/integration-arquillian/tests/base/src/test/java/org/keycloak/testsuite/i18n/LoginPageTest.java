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
package org.keycloak.testsuite.i18n;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.freemarker.DetachedInfoStateChecker;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.forms.ClickThroughAuthenticator;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LanguageComboboxAwarePage;
import org.keycloak.testsuite.pages.LoginExpiredPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.pages.PageUtils;
import org.keycloak.testsuite.pages.TermsAndConditionsPage;
import org.keycloak.testsuite.updaters.UserAttributeUpdater;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.UIUtils;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Cookie;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class LoginPageTest extends AbstractI18NTest {

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected LoginPasswordUpdatePage changePasswordPage;

    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected LoginExpiredPage loginExpiredPage;

    @Page
    protected TermsAndConditionsPage termsPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Before
    public void before() {
        setRealmInternationalization(true);
    }
    
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.addIdentityProvider(IdentityProviderBuilder.create()
                .providerId("github")
                .alias("github")
                .build());
        testRealm.addIdentityProvider(IdentityProviderBuilder.create()
                .providerId("saml")
                .alias("mysaml")
                .build());
        testRealm.addIdentityProvider(IdentityProviderBuilder.create()
                .providerId("oidc")
                .alias("myoidc")
                .displayName("MyOIDC")
                .build());

    }

    @Test
    public void languageDropdown() {
        loginPage.open();
        assertEquals("English", loginPage.getLanguageDropdownText());

        switchLanguageToGermanAndBack("Username or email", "Benutzername oder E-Mail", loginPage);
    }

    @Test
    public void uiLocalesParameter() {
        oauth.loginForm().open();
        assertEquals("English", loginPage.getLanguageDropdownText());

        //test if cookie works
        oauth.loginForm().uiLocales("de").open();
        assertEquals("Deutsch", loginPage.getLanguageDropdownText());

        driver.manage().deleteAllCookies();
        oauth.loginForm().uiLocales("de").open();
        assertEquals("Deutsch", loginPage.getLanguageDropdownText());

        driver.manage().deleteAllCookies();
        oauth.loginForm().uiLocales("en de").open();
        assertEquals("English", loginPage.getLanguageDropdownText());

        driver.manage().deleteAllCookies();
        oauth.loginForm().uiLocales("fr de").open();
        assertEquals("Deutsch", loginPage.getLanguageDropdownText());
    }

    @Test
    public void htmlLangAttributeWithInternationalizationEnabled() {
        loginPage.open();
        assertEquals("en", loginPage.getHtmlLanguage());

        oauth.loginForm().uiLocales("de").open();
        assertEquals("de", loginPage.getHtmlLanguage());
    }

    @Test
    public void htmlLangAttributeWithInternationalizationDisabled() {
        setRealmInternationalization(false);

        loginPage.open();
        assertEquals("en", loginPage.getHtmlLanguage());
    }

    @Test
    public void acceptLanguageHeader() throws IOException {
        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);
            ResteasyClient client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();

            loginPage.open();

            try(Response responseDe = client.target(driver.getCurrentUrl()).request().acceptLanguage("de").get()) {
                Assert.assertTrue(responseDe.readEntity(String.class).contains("Anmeldung bei test"));

                try(Response responseEn = client.target(driver.getCurrentUrl()).request().acceptLanguage("en").get()) {
                    Assert.assertTrue(responseEn.readEntity(String.class).contains("Sign in to test"));
                }
            }

            client.close();
        }
    }

    @Test
    public void testIdentityProviderCapitalization(){
        loginPage.open();
        // contains even name of sub-item - svg element in this case
        assertThat(loginPage.findSocialButton("github").getText(), is("GitHub"));
        assertThat(loginPage.findSocialButton("mysaml").getText(), is("mysaml"));
        assertThat(loginPage.findSocialButton("myoidc").getText(), is("MyOIDC"));
    }


    // KEYCLOAK-3887
    @Test
    public void languageChangeRequiredActions() {
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        UserRepresentation userRep = user.toRepresentation();
        userRep.setRequiredActions(Arrays.asList(UserModel.RequiredAction.UPDATE_PASSWORD.toString()));
        user.update(userRep);

        loginPage.open();

        loginPage.login("test-user@localhost", "password");
        changePasswordPage.assertCurrent();
        assertEquals("English", changePasswordPage.getLanguageDropdownText());

        // Switch language
        switchLanguageToGermanAndBack("Update password", "Passwort aktualisieren", changePasswordPage);

        // Update password
        changePasswordPage.changePassword("password", "password");

        assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());
    }


    // KEYCLOAK-3887
    @Test
    public void languageChangeConsentScreen() {
        // Set client, which requires consent
        oauth.client("third-party", "password");

        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        grantPage.assertCurrent();
        assertEquals("English", grantPage.getLanguageDropdownText());

        // Switch language
        switchLanguageToGermanAndBack("Do you grant these access privileges?", "Wollen Sie diese Zugriffsrechte", changePasswordPage);

        // Confirm grant
        grantPage.accept();

        assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        // Revert client
        oauth.client("test-app", "password");
    }

    @Test
    public void languageUserUpdates() {
        loginPage.open();
        loginPage.openLanguage("Deutsch");

        assertEquals("Deutsch", loginPage.getLanguageDropdownText());

        Cookie localeCookie = driver.manage().getCookieNamed(CookieType.LOCALE.getName());
        assertEquals("de", localeCookie.getValue());

        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        String userId = user.toRepresentation().getId();
        loginPage.login("test-user@localhost", "password");

        events.expect(EventType.UPDATE_PROFILE)
                .user(userId)
                .client("test-app")
                .detail(Details.PREF_UPDATED + UserModel.LOCALE, "de")
                .assertEvent();
        events.expectLogin()
                .user(userId)
                .client("test-app")
                .assertEvent();

        UserRepresentation userRep = user.toRepresentation();
        assertEquals("de", userRep.getAttributes().get("locale").get(0));

        String code = oauth.parseLoginResponse().getCode();
        String idTokenHint = oauth.doAccessTokenRequest(code).getIdToken();
        appPage.logout(idTokenHint);

        loginPage.open();

        assertEquals("Deutsch", loginPage.getLanguageDropdownText());

        userRep.getAttributes().remove("locale");
        user.update(userRep);

        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        // User locale should not be updated due to previous cookie
        userRep = user.toRepresentation();
        Assert.assertNull(userRep.getAttributes());

        code = oauth.parseLoginResponse().getCode();
        idTokenHint = oauth.doAccessTokenRequest(code).getIdToken();
        appPage.logout(idTokenHint);

        loginPage.open();

        // Cookie should be removed as last user to login didn't have a locale
        localeCookie = driver.manage().getCookieNamed(CookieType.LOCALE.getName());
        Assert.assertNull(localeCookie);
    }


    // Test for user updating locale on the error page (when authenticationSession is not available)
    @Test
    public void languageUserUpdatesOnErrorPage() {
        // Login page with invalid redirect_uri
        oauth.redirectUri("http://invalid");
        loginPage.open();

        errorPage.assertCurrent();
        Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

        // Change language should be OK
        errorPage.openLanguage("Deutsch");
        assertEquals("Deutsch", errorPage.getLanguageDropdownText());
        Assert.assertEquals("Ungültiger Parameter: redirect_uri", errorPage.getError());

        // Refresh browser button should keep german language
        driver.navigate().refresh();
        assertEquals("Deutsch", errorPage.getLanguageDropdownText());
        Assert.assertEquals("Ungültiger Parameter: redirect_uri", errorPage.getError());

        // Changing to english should work
        errorPage.openLanguage("English");
        assertEquals("English", errorPage.getLanguageDropdownText());
        Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
    }

    @Test
    public void languageUserUpdatesOnErrorPageStateCheckerTest() throws URISyntaxException {
        // Login page with invalid redirect_uri
        oauth.redirectUri("http://invalid");
        loginPage.open();

        errorPage.assertCurrent();
        Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

        errorPage.openLanguage("Deutsch");
        Assert.assertEquals("Ungültiger Parameter: redirect_uri", errorPage.getError());

        // Add incorrect state checker parameter. Error page should be shown about expired action. Language won't be changed
        String currentUrl = driver.getCurrentUrl();
        String newUrl = KeycloakUriBuilder.fromUri(new URI(currentUrl))
                .replaceQueryParam(LocaleSelectorProvider.KC_LOCALE_PARAM, "en")
                .replaceQueryParam(DetachedInfoStateChecker.STATE_CHECKER_PARAM, "invalid").buildAsString();
        driver.navigate().to(newUrl);

        Assert.assertEquals("Die Aktion ist nicht mehr gültig.", errorPage.getError()); // Action expired.
    }

    @Test
    public void languageUserUpdatesOnExpiredPage() throws Exception {
        try (UserAttributeUpdater userUpdater = UserAttributeUpdater.forUserByUsername(testRealm(), "test-user@localhost")
                .setRequiredActions(UserModel.RequiredAction.UPDATE_PASSWORD).update()) {
            // login with a failure attempt
            loginPage.open();
            loginPage.login("test-user@localhost", "invalid-password");
            loginPage.assertCurrent();
            assertThat(loginPage.getUsernameInputError(), is("Invalid username or password."));
            loginPage.login("test-user@localhost", "password");
            changePasswordPage.assertCurrent();

            // navigate back to the login expired page and change language to german
            UIUtils.navigateBackWithRefresh(driver, loginExpiredPage);
            errorPage.openLanguage("Deutsch");
            assertEquals("Deutsch", errorPage.getLanguageDropdownText());
            assertThat(PageUtils.getPageTitle(driver), is("Diese Seite ist nicht mehr gültig."));

            // continue should show password update in german
            loginExpiredPage.clickLoginContinueLink();
            assertEquals("Deutsch", changePasswordPage.getLanguageDropdownText());
            assertThat(PageUtils.getPageTitle(driver), is("Passwort aktualisieren"));
        }
    }

    // GH issue 41292
    @Test
    public void languageUserUpdatesOnCustomAuthenticatorPage() {
        configureBrowserFlowWithClickThroughAuthenticator();

        loginPage.open();
        Assert.assertTrue(termsPage.isCurrent());

        // Change language on the custom page
        switchLanguageToGermanAndBack("Terms and Conditions", "Bedingungen und Konditionen", termsPage);

        // Revert dummy flow
        RealmRepresentation rep = testRealm().toRepresentation();
        rep.setBrowserFlow("browser");
        testRealm().update(rep);
    }

    @Test
    public void realmLocalizationMessagesAreApplied() {
        String realmLocalizationMessageKey = "loginAccountTitle";

        String realmLocalizationMessageValueEn = "Localization Test EN";
        saveLocalizationText(Locale.ENGLISH.toLanguageTag(), realmLocalizationMessageKey,
                realmLocalizationMessageValueEn);
        String realmLocalizationMessageValueDe = "Localization Test DE";
        saveLocalizationText(Locale.GERMAN.toLanguageTag(), realmLocalizationMessageKey,
                realmLocalizationMessageValueDe);

        loginPage.open();
        switchLanguageToGermanAndBack(realmLocalizationMessageValueEn, realmLocalizationMessageValueDe, loginPage);
    }

    // KEYCLOAK-18590
    @Test
    public void realmLocalizationMessagesAreNotCachedWithinTheTheme() {
        final String locale = Locale.ENGLISH.toLanguageTag();

        final String realmLocalizationMessageKey = "loginAccountTitle";
        final String realmLocalizationMessageValue = "Localization Test";

        saveLocalizationText(locale, realmLocalizationMessageKey, realmLocalizationMessageValue);
        loginPage.open();
        assertThat(driver.getPageSource(), containsString(realmLocalizationMessageValue));

        testRealm().localization().deleteRealmLocalizationText(locale, realmLocalizationMessageKey);
        loginPage.open();
        assertThat(driver.getPageSource(), not(containsString(realmLocalizationMessageValue)));
    }

    @Test
    public void realmLocalizationMessagesUsedDuringErrorHandling() {
        final String locale = Locale.ENGLISH.toLanguageTag();

        final String realmLocalizationMessageKey = "errorTitle";
        final String realmLocalizationMessageValue = "We are really sorry...";

        saveLocalizationText(locale, realmLocalizationMessageKey, realmLocalizationMessageValue);
        String nonExistingUrl = oauth.loginForm().build().split("protocol")[0] + "incorrect-path";
        driver.navigate().to(nonExistingUrl);

        assertThat(driver.getPageSource(), containsString(realmLocalizationMessageValue));
    }

    private void saveLocalizationText(String locale, String key, String value) {
        testRealm().localization().saveRealmLocalizationText(locale, key, value);
        getCleanup().addLocalization(locale);
    }

    private void switchLanguageToGermanAndBack(String expectedEnglishMessage, String expectedGermanMessage, LanguageComboboxAwarePage page) {
        // Switch language to Deutsch
        page.openLanguage("Deutsch");
        assertEquals("Deutsch", page.getLanguageDropdownText());
        String pageSource = driver.getPageSource();
        assertThat(pageSource, not(containsString(expectedEnglishMessage)));
        assertThat(pageSource, containsString(expectedGermanMessage));

        // Revert language
        page.openLanguage("English");
        assertEquals("English", page.getLanguageDropdownText());
        pageSource = driver.getPageSource();
        assertThat(pageSource, containsString(expectedEnglishMessage));
        assertThat(pageSource, not(containsString(expectedGermanMessage)));
    }

    private void setRealmInternationalization(final boolean enabled) {
        final var realmResource = testRealm();
        RealmRepresentation realm = realmResource.toRepresentation();
        realm.setInternationalizationEnabled(enabled);
        realmResource.update(realm);
    }

    private void configureBrowserFlowWithClickThroughAuthenticator() {
        final String newFlowAlias = "browser - rule";
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        // Update the browser forms with a UsernamePasswordForm
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ClickThroughAuthenticator.PROVIDER_ID)
                )
                .defineAsBrowserFlow()
        );
    }
}
