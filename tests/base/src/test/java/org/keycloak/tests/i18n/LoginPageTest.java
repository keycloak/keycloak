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
package org.keycloak.tests.i18n;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.freemarker.DetachedInfoStateChecker;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.AbstractLoginPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginExpiredPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.page.OAuthGrantPage;
import org.keycloak.testframework.ui.page.TermsAndConditionsPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.testsuite.forms.ClickThroughAuthenticator;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.IdentityProviderBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@KeycloakIntegrationTest(config = LoginPageTest.ServerConfig.class)
public class LoginPageTest {

    @InjectRealm(config = LoginPageRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(config = BasicUserConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedUser user;

    @InjectOAuthClient
    protected OAuthClient oauth;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected ErrorPage errorPage;

    @InjectPage
    protected LoginPasswordUpdatePage changePasswordPage;

    @InjectPage
    protected OAuthGrantPage grantPage;

    @InjectPage
    protected LoginExpiredPage loginExpiredPage;

    @InjectPage
    protected TermsAndConditionsPage termsPage;

    @InjectWebDriver
    protected ManagedWebDriver driver;

    @InjectSimpleHttp
    protected SimpleHttp simpleHttp;

    @InjectEvents
    protected Events events;

    @InjectRunOnServer
    protected RunOnServerClient runOnServer;

    @Test
    public void languageDropdown() {
        oauth.openLoginForm();
        assertEquals("English", loginPage.getSelectedLanguage());

        switchLanguageToGermanAndBack("Username or email", "Benutzername oder E-Mail", loginPage);
    }

    @Test
    public void uiLocalesParameter() {
        oauth.loginForm().open();
        assertEquals("English", loginPage.getSelectedLanguage());

        //test if cookie works
        oauth.loginForm().uiLocales("de").open();
        assertEquals("Deutsch", loginPage.getSelectedLanguage());

        driver.cookies().deleteAll();
        oauth.loginForm().uiLocales("de").open();
        assertEquals("Deutsch", loginPage.getSelectedLanguage());

        driver.cookies().deleteAll();
        oauth.loginForm().uiLocales("en de").open();
        assertEquals("English", loginPage.getSelectedLanguage());

        driver.cookies().deleteAll();
        oauth.loginForm().uiLocales("fr de").open();
        assertEquals("Deutsch", loginPage.getSelectedLanguage());
    }

    @Test
    public void htmlLangAttributeWithInternationalizationEnabled() {
        oauth.openLoginForm();
        assertEquals("en", getHtmlLanguage());

        oauth.loginForm().uiLocales("de").open();
        assertEquals("de", getHtmlLanguage());
    }

    @Test
    public void htmlLangAttributeWithInternationalizationDisabled() {
        realm.updateWithCleanup(r -> r.internationalizationEnabled(false));

        oauth.openLoginForm();
        assertEquals("en", getHtmlLanguage());
    }

    @Test
    public void acceptLanguageHeader() throws IOException {
        String responseDe = simpleHttp.doGet(oauth.loginForm().build()).header("Accept-Language", "de").header("Accept", "text/html").asString();
        Assertions.assertTrue(responseDe.contains("Bei Ihrem Konto anmelden"));

        String responseEn = simpleHttp.doGet(oauth.loginForm().build()).header("Accept-Language", "en").header("Accept", "text/html").asString();
        Assertions.assertTrue(responseEn.contains("Sign in to your account"));
    }

    @Test
    public void testIdentityProviderCapitalization(){
        oauth.openLoginForm();
        // contains even name of sub-item - svg element in this case
        assertThat(loginPage.findSocialButton("github").getText(), is("GitHub"));
        assertThat(loginPage.findSocialButton("mysaml").getText(), is("mysaml"));
        assertThat(loginPage.findSocialButton("myoidc").getText(), is("MyOIDC"));
    }


    // KEYCLOAK-3887
    @Test
    public void languageChangeRequiredActions() {
        UserResource user = this.user.admin();
        UserRepresentation userRep = user.toRepresentation();
        userRep.setRequiredActions(Arrays.asList(UserModel.RequiredAction.UPDATE_PASSWORD.toString()));
        user.update(userRep);

        oauth.openLoginForm();
        oauth.fillLoginForm("basic-user", "password");

        changePasswordPage.assertCurrent();
        assertEquals("English", changePasswordPage.getSelectedLanguage());

        // Switch language
        switchLanguageToGermanAndBack("Update password", "Passwort aktualisieren", changePasswordPage);

        // Update password
        changePasswordPage.changePassword("password", "password");

        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());
    }


    // KEYCLOAK-3887
    @Test
    public void languageChangeConsentScreen() {
        // Set client, which requires consent
        oauth.client("third-party", "password");

        oauth.openLoginForm();
        oauth.fillLoginForm("basic-user", "password");

        grantPage.assertCurrent();
        assertEquals("English", grantPage.getSelectedLanguage());

        // Switch language
        switchLanguageToGermanAndBack("Do you grant these access privileges?", "Wollen Sie diese Zugriffsrechte", changePasswordPage);

        // Confirm grant
        grantPage.accept();

        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        // Revert client
        oauth.client("test-app", "password");
    }

    @Test
    public void languageUserUpdates() throws InterruptedException {
        oauth.openLoginForm();
        loginPage.selectLanguage("Deutsch");

        assertEquals("Deutsch", loginPage.getSelectedLanguage());

        Cookie localeCookie = driver.cookies().get(CookieType.LOCALE);
        assertEquals("de", localeCookie.getValue());

        loginPage.fillLogin("basic-user", "password");
        loginPage.submit();

        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.assertSuccess(events.poll()).type(EventType.UPDATE_PROFILE).userId(user.getId()).details(Details.PREF_UPDATED + UserModel.LOCALE, "de");
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN).userId(user.getId());

        UserRepresentation userRep = user.admin().toRepresentation();
        assertEquals("de", userRep.getAttributes().get("locale").get(0));

        String code = oauth.parseLoginResponse().getCode();
        String idTokenHint = oauth.doAccessTokenRequest(code).getIdToken();
        oauth.logoutRequest().idTokenHint(idTokenHint).send();
        oauth.openLoginForm();

        assertEquals("Deutsch", loginPage.getSelectedLanguage());

        userRep.getAttributes().remove("locale");
        user.admin().update(userRep);

        oauth.doLogin("basic-user", "password");

        // User locale should not be updated due to previous cookie
        userRep = user.admin().toRepresentation();
        Assertions.assertNull(userRep.getAttributes());

        code = oauth.parseLoginResponse().getCode();
        idTokenHint = oauth.doAccessTokenRequest(code).getIdToken();
        oauth.logoutRequest().idTokenHint(idTokenHint).send();

        oauth.openLoginForm();

        // Cookie should be removed as last user to login didn't have a locale
        localeCookie = driver.cookies().get(CookieType.LOCALE);
        Assertions.assertNull(localeCookie);
    }


    // Test for user updating locale on the error page (when authenticationSession is not available)
    @Test
    public void languageUserUpdatesOnErrorPage() {
        // Login page with invalid redirect_uri
        String redirectUri = oauth.getRedirectUri();
        oauth.redirectUri("http://invalid");
        oauth.openLoginForm();

        errorPage.assertCurrent();
        Assertions.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

        // Change language should be OK
        errorPage.selectLanguage("Deutsch");
        assertEquals("Deutsch", errorPage.getSelectedLanguage());
        Assertions.assertEquals("Ungültiger Parameter: redirect_uri", errorPage.getError());

        // Refresh browser button should keep german language
        driver.navigate().refresh();
        assertEquals("Deutsch", errorPage.getSelectedLanguage());
        Assertions.assertEquals("Ungültiger Parameter: redirect_uri", errorPage.getError());

        // Changing to english should work
        errorPage.selectLanguage("English");
        assertEquals("English", errorPage.getSelectedLanguage());
        Assertions.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

        oauth.redirectUri(redirectUri);
    }

    @Test
    public void languageUserUpdatesOnErrorPageStateCheckerTest() throws URISyntaxException {
        String redirectUri = oauth.getRedirectUri();

        // Login page with invalid redirect_uri
        oauth.redirectUri("http://invalid");
        oauth.openLoginForm();

        errorPage.assertCurrent();
        Assertions.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

        errorPage.selectLanguage("Deutsch");
        Assertions.assertEquals("Ungültiger Parameter: redirect_uri", errorPage.getError());

        // Add incorrect state checker parameter. Error page should be shown about expired action. Language won't be changed
        String currentUrl = driver.getCurrentUrl();
        String newUrl = KeycloakUriBuilder.fromUri(new URI(currentUrl))
                .replaceQueryParam(LocaleSelectorProvider.KC_LOCALE_PARAM, "en")
                .replaceQueryParam(DetachedInfoStateChecker.STATE_CHECKER_PARAM, "invalid").buildAsString();
        driver.open(newUrl);

        Assertions.assertEquals("Die Aktion ist nicht mehr gültig.", errorPage.getError()); // Action expired.

        oauth.redirectUri(redirectUri);
    }

    @Test
    public void languageUserUpdatesOnExpiredPage() throws Exception {
        UserRepresentation userRep = user.admin().toRepresentation();
        userRep.setRequiredActions(Collections.singletonList(UserModel.RequiredAction.UPDATE_PASSWORD.toString()));
        user.admin().update(userRep);

        oauth.openLoginForm();
        oauth.fillLoginForm("basic-user", "invalid-password");
        loginPage.assertCurrent();

        assertThat(loginPage.getUsernameInputError(), is("Invalid username or password."));
        loginPage.fillLogin("basic-user", "password");
        loginPage.submit();

        changePasswordPage.assertCurrent();

        // navigate back to the login expired page and change language to german
        driver.navigate().backWithRefresh(loginExpiredPage);
        errorPage.selectLanguage("Deutsch");
        assertEquals("Deutsch", errorPage.getSelectedLanguage());
        driver.assertions().assertTitle("Diese Seite ist nicht mehr gültig.");

        // continue should show password update in german
        loginExpiredPage.clickLoginContinueLink();
        assertEquals("Deutsch", changePasswordPage.getSelectedLanguage());
        driver.assertions().assertTitle("Passwort aktualisieren");
    }

    // GH issue 41292
    @Test
    public void languageUserUpdatesOnCustomAuthenticatorPage() {
        configureBrowserFlowWithClickThroughAuthenticator();

        oauth.openLoginForm();
        termsPage.assertCurrent();

        // Change language on the custom page
        switchLanguageToGermanAndBack("Terms and Conditions", "Bedingungen und Konditionen", termsPage);

        // Revert dummy flow
        RealmRepresentation rep = realm.admin().toRepresentation();
        rep.setBrowserFlow("browser");
        realm.admin().update(rep);
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

        oauth.openLoginForm();
        switchLanguageToGermanAndBack(realmLocalizationMessageValueEn, realmLocalizationMessageValueDe, loginPage);
    }

    // KEYCLOAK-18590
    @Test
    public void realmLocalizationMessagesAreNotCachedWithinTheTheme() {
        final String locale = Locale.ENGLISH.toLanguageTag();

        final String realmLocalizationMessageKey = "loginAccountTitle";
        final String realmLocalizationMessageValue = "Localization Test";

        saveLocalizationText(locale, realmLocalizationMessageKey, realmLocalizationMessageValue);
        oauth.openLoginForm();
        assertThat(driver.page().getPageSource(), containsString(realmLocalizationMessageValue));

        realm.admin().localization().deleteRealmLocalizationText(locale, realmLocalizationMessageKey);
        oauth.openLoginForm();
        assertThat(driver.page().getPageSource(), not(containsString(realmLocalizationMessageValue)));
    }

    @Test
    public void realmLocalizationMessagesUsedDuringErrorHandling() {
        final String locale = Locale.ENGLISH.toLanguageTag();

        final String realmLocalizationMessageKey = "errorTitle";
        final String realmLocalizationMessageValue = "We are really sorry...";

        saveLocalizationText(locale, realmLocalizationMessageKey, realmLocalizationMessageValue);
        String nonExistingUrl = oauth.loginForm().build().split("protocol")[0] + "incorrect-path";
        driver.open(nonExistingUrl);

        errorPage.assertCurrent();

        assertThat(driver.page().getPageSource(), containsString(realmLocalizationMessageValue));
    }

    private String getHtmlLanguage() {
        return driver.findElement(By.xpath("//html")).getAttribute("lang");
    }

    private void saveLocalizationText(String locale, String key, String value) {
        realm.admin().localization().saveRealmLocalizationText(locale, key, value);
        realm.cleanup().add(r -> r.localization().deleteRealmLocalizationTexts(locale));
    }

    private void switchLanguageToGermanAndBack(String expectedEnglishMessage, String expectedGermanMessage, AbstractLoginPage page) {
        // Switch language to Deutsch
        page.selectLanguage("Deutsch");
        assertEquals("Deutsch", page.getSelectedLanguage());
        String pageSource = driver.page().getPageSource();
        assertThat(pageSource, not(containsString(expectedEnglishMessage)));
        assertThat(pageSource, containsString(expectedGermanMessage));

        // Revert language
        page.selectLanguage("English");
        assertEquals("English", page.getSelectedLanguage());
        pageSource = driver.page().getPageSource();
        assertThat(pageSource, containsString(expectedEnglishMessage));
        assertThat(pageSource, not(containsString(expectedGermanMessage)));
    }

    private void configureBrowserFlowWithClickThroughAuthenticator() {
        final String newFlowAlias = "browser - rule";
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        // Update the browser forms with a UsernamePasswordForm
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ClickThroughAuthenticator.PROVIDER_ID)
                )
                .defineAsBrowserFlow()
        );
    }

    public static class LoginPageRealmConfig extends RealmWithInternationalization {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm = super.configure(realm);
            realm.identityProvider(IdentityProviderBuilder.create()
                        .providerId("github")
                        .alias("github")
                        .build());
            realm.identityProvider(IdentityProviderBuilder.create()
                        .providerId("saml")
                        .alias("mysaml")
                        .build());
            realm.identityProvider(IdentityProviderBuilder.create()
                        .providerId("oidc")
                        .alias("myoidc")
                        .displayName("MyOIDC")
                        .build());
            realm.client(ClientConfigBuilder.create().clientId("third-party").secret("password").consentRequired(true).redirectUris("*").build());
            return realm;
        }
    }

    protected static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }

}
