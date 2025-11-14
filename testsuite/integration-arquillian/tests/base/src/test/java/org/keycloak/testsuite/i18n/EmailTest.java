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

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.WaitUtils;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class EmailTest extends AbstractI18NTest {

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginPasswordResetPage resetPasswordPage;

    @Page
    private InfoPage infoPage;

    @Page
    private LoginPasswordUpdatePage loginPasswordUpdatePage;

    private void changeUserLocale(String locale) {
        UserRepresentation user = findUser("login-test");
        user.singleAttribute(UserModel.LOCALE, locale);
        ApiUtil.findUserByUsernameId(testRealm(), "login-test").update(user);
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
        testRealm().localization().saveRealmLocalizationText(Locale.ENGLISH.toLanguageTag(), subjectMessageKey, subjectEn);
        testRealm().localization().saveRealmLocalizationText(Locale.ENGLISH.toLanguageTag(), bodyMessageKey, bodyMessageEn);
        getCleanup().addLocalization(Locale.ENGLISH.toLanguageTag());

        String subjectDe = "Subject DE";
        String expectedBodyContentDe = "Body DE";
        String bodyMessageDe = expectedBodyContentDe + placeholders;
        testRealm().localization().saveRealmLocalizationText(Locale.GERMAN.toLanguageTag(), subjectMessageKey, subjectDe);
        testRealm().localization().saveRealmLocalizationText(Locale.GERMAN.toLanguageTag(), bodyMessageKey, bodyMessageDe);
        getCleanup().addLocalization(Locale.GERMAN.toLanguageTag());

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
    public void updatePasswordFromAdmin() throws MessagingException, IOException {
        changeUserLocale(null);
        try {
            UserResource testUser = ApiUtil.findUserByUsernameId(testRealm(), "login-test");
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            SimpleHttpResponse responseGet = SimpleHttpDefault.doPut(getAuthServerRoot() + "admin/realms/test/users/" + testUser.toRepresentation().getId() + "/execute-actions-email", httpClient)
                    .auth(adminClient.tokenManager().getAccessTokenString())
                    .header("Accept-Language", "de")
                    .json(Arrays.asList(UserModel.RequiredAction.UPDATE_PASSWORD.toString()))
                    .asResponse();

            assertEquals(responseGet.getStatus(), 204);

            MimeMessage message = greenMail.getReceivedMessages()[0];
            String textBody = MailUtils.getBody(message).getText();

            Assert.assertThat(textBody, containsString("Your administrator has just requested"));

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            // Revert
            changeUserLocale("en");
        }
    }

    @Test
    public void restPasswordEmailWithAcceptLanguageHeader() throws MessagingException, IOException {
        changeUserLocale(null);
        try {

            loginPage.open();
            loginPage.resetPassword();

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

                Set<Cookie> cookies =  oauth.getDriver().manage().getCookies();
                String cookieHeader = cookies.stream()
                        .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                        .collect(Collectors.joining("; "));
                String resetFormUrl = resetPasswordPage.getFormUrl();

                HttpPost post = new HttpPost(resetFormUrl);
                post.setHeader(HttpHeaders.COOKIE, cookieHeader);
                post.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "de");

                List<NameValuePair> parameters = new LinkedList<>();
                parameters.add(new BasicNameValuePair("username", "login-test"));
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
        loginPage.open();
        loginPage.resetPassword();
        resetPasswordPage.changePassword("login-test");
    }

    private void verifyResetPassword(String expectedSubject, String expectedTextBodyContent, String expectedHtmlBodyContent, int expectedMsgCount)
            throws MessagingException, IOException {
        assertEquals(expectedMsgCount, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[expectedMsgCount - 1];

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
        UserResource testUser = ApiUtil.findUserByUsernameId(testRealm(), "login-test");
        testUser.executeActionsEmail(Arrays.asList(UserModel.RequiredAction.UPDATE_PASSWORD.toString()));

        if (!greenMail.waitForIncomingEmail(1)) {
            Assert.fail("Error when receiving email");
        }

        String link = MailUtils.getPasswordResetEmailLink(greenMail.getLastReceivedMessage());

        // Make sure kc_locale added to link doesn't set locale
        link += "&kc_locale=de";

        DroneUtils.getCurrentDriver().navigate().to(link);
        WaitUtils.waitForPageToLoad();

        Assert.assertTrue("Expected to be on InfoPage, but it was on " + DroneUtils.getCurrentDriver().getTitle(), infoPage.isCurrent());
        assertThat(infoPage.getLanguageDropdownText(), is(equalTo("English")));

        infoPage.openLanguage("Deutsch");

        assertThat(DroneUtils.getCurrentDriver().getPageSource(), containsString("Passwort aktualisieren"));

        infoPage.clickToContinueDe();

        loginPasswordUpdatePage.openLanguage("English");
        loginPasswordUpdatePage.changePassword("pass", "pass");
        WaitUtils.waitForPageToLoad();

        Assert.assertTrue("Expected to be on InfoPage, but it was on " + DroneUtils.getCurrentDriver().getTitle(), infoPage.isCurrent());
        assertThat(infoPage.getInfo(), containsString("Your account has been updated."));

        // Change language again when on final info page with the message about updated account (authSession removed already at this point)
        infoPage.openLanguage("Deutsch");
        assertEquals("Deutsch", infoPage.getLanguageDropdownText());
        assertThat(infoPage.getInfo(), containsString("Ihr Benutzerkonto wurde aktualisiert."));

        infoPage.openLanguage("English");
        assertEquals("English", infoPage.getLanguageDropdownText());
        assertThat(infoPage.getInfo(), containsString("Your account has been updated."));

    }

    // Issue 10981
    @Test
    public void resetPasswordOriginalUiLocalePreservedAfterForgetPassword() throws MessagingException, IOException {
        // Assert login page is in german
        oauth.loginForm().uiLocales("de").open();
        assertEquals("Deutsch", loginPage.getLanguageDropdownText());

        // Click "Forget password"
        driver.findElement(By.linkText("Passwort vergessen?")).click();
        assertEquals("Deutsch", resetPasswordPage.getLanguageDropdownText());
        resetPasswordPage.changePassword("login-test");

        // Ensure that page is still in german (after authenticationSession was forked on server). The emailSentMessage should be also displayed in german
        loginPage.assertCurrent();
        assertEquals("Deutsch", loginPage.getLanguageDropdownText());
        assertEquals("Sie sollten in Kürze eine E-Mail mit weiteren Instruktionen erhalten.", loginPage.getSuccessMessage());
    }

}
