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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.InfoPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.tests.utils.MailUtils;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@KeycloakIntegrationTest
public class EmailTest {

    @InjectRealm(config = RealmWithInternationalization.class)
    ManagedRealm realm;

    @InjectUser(config = BasicUserConfig.class)
    ManagedUser user;

    @InjectMailServer
    MailServer mailServer;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginPasswordResetPage resetPasswordPage;

    @InjectPage
    InfoPage infoPage;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPasswordUpdatePage loginPasswordUpdatePage;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    private void changeUserLocale(String locale) {
        UserRepresentation userRep = user.admin().toRepresentation();
        userRep.singleAttribute(UserModel.LOCALE, locale);
        user.admin().update(userRep);
    }

    @Test
    public void restPasswordEmail() throws MessagingException, IOException {
        String expectedBodyContent = "Someone just requested to change";
        sendResetPasswordEmail();
        verifyResetPassword("Reset password", expectedBodyContent, null, 1);

        changeUserLocale("en");
        sendResetPasswordEmail();
        verifyResetPassword("Reset password", expectedBodyContent, null, 2);
    }

    @Test
    public void realmLocalizationMessagesAreApplied() throws MessagingException, IOException {
        String subjectMessageKey = "passwordResetSubject";
        String bodyMessageKey = "passwordResetBody";
        String placeholders = "{0} {1} {2}";

        String subjectEn = "Subject EN";
        String expectedBodyContentEn = "Body EN";
        String bodyMessageEn = expectedBodyContentEn + placeholders;
        realm.cleanup().add(r -> r.localization().deleteRealmLocalizationTexts(Locale.ENGLISH.toLanguageTag()));
        realm.admin().localization().saveRealmLocalizationText(Locale.ENGLISH.toLanguageTag(), subjectMessageKey, subjectEn);
        realm.admin().localization().saveRealmLocalizationText(Locale.ENGLISH.toLanguageTag(), bodyMessageKey, bodyMessageEn);

        String subjectDe = "Subject DE";
        String expectedBodyContentDe = "Body DE";
        String bodyMessageDe = expectedBodyContentDe + placeholders;
        realm.cleanup().add(r -> r.localization().deleteRealmLocalizationTexts(Locale.GERMAN.toLanguageTag()));
        realm.admin().localization().saveRealmLocalizationText(Locale.GERMAN.toLanguageTag(), subjectMessageKey, subjectDe);
        realm.admin().localization().saveRealmLocalizationText(Locale.GERMAN.toLanguageTag(), bodyMessageKey, bodyMessageDe);

        try {
            sendResetPasswordEmail();
            verifyResetPassword(subjectEn, expectedBodyContentEn, "<html lang=\"en\" dir=\"ltr\">", 1);

            changeUserLocale("de");

            sendResetPasswordEmail();
            verifyResetPassword(subjectDe, expectedBodyContentDe, "<html lang=\"de\" dir=\"ltr\">", 2);
        } finally {
            // Revert
            changeUserLocale("en");
        }
    }

    @Test
    public void restPasswordEmailGerman() throws MessagingException, IOException {
        changeUserLocale("de");
        try {
            sendResetPasswordEmail();
            verifyResetPassword("Passwort zurücksetzen", "Es wurde eine Änderung", null, 1);
        } finally {
            // Revert
            changeUserLocale("en");
        }
    }

    @Test
    public void updatePasswordFromAdmin() {
        changeUserLocale(null);
        try {
            UserResource testUser = user.admin();
            SimpleHttpResponse responseGet = simpleHttp.doPut(keycloakUrls.getAdmin() + "/realms/" + realm.getName() + "/users/" + testUser.toRepresentation().getId() + "/execute-actions-email")
                    .auth(adminClient.tokenManager().getAccessTokenString())
                    .header("Accept-Language", "de")
                    .json(Arrays.asList(UserModel.RequiredAction.UPDATE_PASSWORD.toString()))
                    .asResponse();

            assertEquals(responseGet.getStatus(), 204);

            MimeMessage message = mailServer.getReceivedMessages()[0];
            String textBody = MailUtils.getBody(message).getText();

            MatcherAssert.assertThat(textBody, containsString("Your administrator has just requested"));

        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        } finally {
            // Revert
            changeUserLocale("en");
        }
    }

    @Test
    public void restPasswordEmailWithAcceptLanguageHeader() throws MessagingException, IOException {
        changeUserLocale(null);
        try {

            oauth.openLoginForm();
            loginPage.resetPassword();

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

                Set<Cookie> cookies =  driver.cookies().getAll();
                String cookieHeader = cookies.stream()
                        .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                        .collect(Collectors.joining("; "));
                String resetFormUrl = resetPasswordPage.getFormUrl();

                HttpPost post = new HttpPost(resetFormUrl);
                post.setHeader(HttpHeaders.COOKIE, cookieHeader);
                post.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "de");

                List<NameValuePair> parameters = new LinkedList<>();
                parameters.add(new BasicNameValuePair("username", "basic-user"));
                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
                post.setEntity(formEntity);

                CloseableHttpResponse response = client.execute(post);
                assertEquals(200, response.getStatusLine().getStatusCode());
            }
            verifyResetPassword("Passwort zurücksetzen", "Es wurde eine Änderung", null, 1);
        } finally {
            // Revert
            changeUserLocale("en");
        }
    }


    private void sendResetPasswordEmail() {
        oauth.openLoginForm();
        loginPage.resetPassword();
        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword("basic-user");
    }

    private void verifyResetPassword(String expectedSubject, String expectedTextBodyContent, String expectedHtmlBodyContent, int expectedMsgCount)
            throws MessagingException, IOException {
        mailServer.waitForIncomingEmail(expectedMsgCount);
        assertEquals(expectedMsgCount, mailServer.getReceivedMessages().length);

        MimeMessage message = mailServer.getReceivedMessages()[expectedMsgCount - 1];

        assertEquals(expectedSubject, message.getSubject());

        String textBody = MailUtils.getBody(message).getText();
        assertThat(textBody, containsString(expectedTextBodyContent));
        // make sure all placeholders have been replaced
        assertThat(textBody, not(containsString("{")));
        assertThat(textBody, not(containsString("}")));

        if (expectedHtmlBodyContent != null) {
            String htmlBody = MailUtils.getBody(message).getHtml();
            assertThat(htmlBody, containsString(expectedHtmlBodyContent));
        }
    }

    //KEYCLOAK-7478
    // Issue 13922
    @Test
    public void changeLocaleOnInfoPage() throws InterruptedException, IOException {
        UserResource testUser = user.admin();
        testUser.executeActionsEmail(Arrays.asList(UserModel.RequiredAction.UPDATE_PASSWORD.toString()));

        if (!mailServer.waitForIncomingEmail(1)) {
            Assertions.fail("Error when receiving email");
        }

        String link = MailUtils.getPasswordResetEmailLink(mailServer.getLastReceivedMessage());

        // Make sure kc_locale added to link doesn't set locale
        link += "&kc_locale=de";

        driver.open(link);

        infoPage.assertCurrent();
        assertThat(infoPage.getSelectedLanguage(), is(equalTo("English")));

        infoPage.selectLanguage("Deutsch");

        assertThat(driver.page().getPageSource(), containsString("Passwort aktualisieren"));

        driver.findElement(By.linkText("» Klicken Sie hier um fortzufahren")).click();

        loginPasswordUpdatePage.selectLanguage("English");
        loginPasswordUpdatePage.changePassword("pass", "pass");

        assertThat(infoPage.getInfo(), containsString("Your account has been updated."));

        // Change language again when on final info page with the message about updated account (authSession removed already at this point)
        infoPage.selectLanguage("Deutsch");
        assertEquals("Deutsch", infoPage.getSelectedLanguage());
        assertThat(infoPage.getInfo(), containsString("Ihr Benutzerkonto wurde aktualisiert."));

        infoPage.selectLanguage("English");
        assertEquals("English", infoPage.getSelectedLanguage());
        assertThat(infoPage.getInfo(), containsString("Your account has been updated."));

    }

    // Issue 10981
    @Test
    public void resetPasswordOriginalUiLocalePreservedAfterForgetPassword() throws MessagingException, IOException {
        // Assert login page is in german
        oauth.loginForm().uiLocales("de").open();
        assertEquals("Deutsch", loginPage.getSelectedLanguage());

        // Click "Forget password"
        driver.findElement(By.linkText("Passwort vergessen?")).click();
        assertEquals("Deutsch", resetPasswordPage.getSelectedLanguage());
        resetPasswordPage.changePassword("basic-user");

        // Ensure that page is still in german (after authenticationSession was forked on server). The emailSentMessage should be also displayed in german
        loginPage.assertCurrent();
        assertEquals("Deutsch", loginPage.getSelectedLanguage());
        assertEquals("Sie sollten in Kürze eine E-Mail mit weiteren Instruktionen erhalten.", loginPage.getSuccessMessage());
    }

}
