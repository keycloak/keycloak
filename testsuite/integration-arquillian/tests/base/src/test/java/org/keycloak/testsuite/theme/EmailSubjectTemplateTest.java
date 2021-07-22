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
package org.keycloak.testsuite.theme;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.*;
import org.keycloak.theme.Theme;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class EmailSubjectTemplateTest extends AbstractEmailSubjectTemplateTest {

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

    private String testTheme = "test";

    private void changeUserLocale(String locale) {
        UserRepresentation user = findUser(testUserUsername);
        user.singleAttribute(UserModel.LOCALE, locale);
        ApiUtil.findUserByUsernameId(testRealm(), testUserUsername).update(user);
    }

    private Map<String, String> getSMTPConfig() {
        Map<String, String> smtpConfig = new HashMap<>();
        smtpConfig.put("host", "127.0.0.1");
        smtpConfig.put("port", "3025");
        smtpConfig.put("from", "auto@keycloak.org");
        smtpConfig.put("auth", null);
        smtpConfig.put("ssl", null);
        smtpConfig.put("starttls", null);
        smtpConfig.put("user", null);
        smtpConfig.put("password", null);
        smtpConfig.put("replyTo", null);
        smtpConfig.put("envelopeFrom", null);
        return smtpConfig;
    }

    private void setTheme(String realmName, String themeName, Theme.Type themeType)
    {
        RealmResource realm = adminClient.realm(realmName);
        RealmRepresentation realmRep = realm.toRepresentation();
        switch (themeType) {
            case EMAIL:
                realmRep.setEmailTheme(themeName);
                realm.update(realmRep);
                RealmResource verifyRealm = adminClient.realm(realmName);
                Assert.assertEquals(themeName, verifyRealm.toRepresentation().getEmailTheme());
                break;
            default:
                Assert.fail("Only EMAIL type is supported");
        }
    }

    private void afterEmailSubjectTemplateTest()
    {
        setTheme(TEST_REALM_NAME, "base", Theme.Type.EMAIL);
    }

    private void assertThemeTemplate(String themeName, Theme.Type themeType, String templatePath, Boolean presence) {

        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme(themeName, themeType);
                if (presence)
                    Assert.assertNotNull(theme.getTemplate(templatePath));
                else
                    Assert.assertNull(theme.getTemplate(templatePath));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }

    // Send a mail for which a subject template doesn't exist (should send properly)
    @Test
    public void checkTestEmailSubject() throws MessagingException, InterruptedException {
        ProfileAssume.assumeCommunity();

        setTheme("master", "base", Theme.Type.EMAIL);

        // Subject construction should be handled by base theme message.properties file, not a template
        assertThemeTemplate("base", Theme.Type.EMAIL, "text/email-test-subject.ftl", false);
        assertThemeTemplate("base", Theme.Type.EMAIL, "text/email-test.ftl", true);
        assertThemeTemplate(testTheme, Theme.Type.EMAIL, "text/email-test-subject.ftl", false);
        assertThemeTemplate(testTheme, Theme.Type.EMAIL, "text/email-test.ftl", false);

        RealmResource realm = adminClient.realm("master");
        Response response = realm.testSMTPConnection(getSMTPConfig());
        Assert.assertEquals(
                String.format("Send mail request status expected [%s] but [%s] was received", 204, response.getStatus()),
                204,
                response.getStatus());

        if (!greenMail.waitForIncomingEmail(1)) {
            Assert.fail("Test email not received");
        }

        MimeMessage message = greenMail.getLastReceivedMessage();
        Assert.assertEquals(adminEmail, message.getAllRecipients()[0].toString());

        Assert.assertEquals("[KEYCLOAK] - SMTP test message", message.getSubject());

        afterEmailSubjectTemplateTest();
    }


    // Send a mail for which a subect template exists but contains a syntax error
    // Should send with fallback (backwards compatibility) and log the error
    @Test
    public void checkUpdatePasswordEmailSubject() throws MessagingException, InterruptedException {

        setTheme(TEST_REALM_NAME, testTheme, Theme.Type.EMAIL);

        UserResource searchUser = ApiUtil.findUserByUsernameId(testRealm(), testUserUsername);
        searchUser.executeActionsEmail(Arrays.asList(UserModel.RequiredAction.UPDATE_PASSWORD.toString()));

        if (!greenMail.waitForIncomingEmail(1)) {
            Assert.fail("Error when receiving email");
        }
        MimeMessage message = greenMail.getLastReceivedMessage();
        Assert.assertEquals(testUserEmail,message.getAllRecipients()[0].toString());

        String updatePwdSubject = message.getSubject();
        Assert.assertNotEquals("Update Your Account (TEMPLATE)", updatePwdSubject);
        Assert.assertEquals("Update Your Account (NO_TEMPLATE)", updatePwdSubject);

        afterEmailSubjectTemplateTest();
    }

    // Send a mail for which a valid subject template exists
    @Test
    public void checkResetPasswordEmailSubject() throws MessagingException, InterruptedException {

        setTheme(TEST_REALM_NAME, testTheme, Theme.Type.EMAIL);

        changeUserLocale("en");

        loginPage.open();
        loginPage.resetPassword();
        resetPasswordPage.changePassword(testUserUsername);

        if (!greenMail.waitForIncomingEmail(1)) {
            Assert.fail("Error when receiving email");
        }
        MimeMessage message = greenMail.getLastReceivedMessage();
        Assert.assertEquals(testUserEmail,message.getAllRecipients()[0].toString());

        String expectedSubject = String.format("%s, please reset your password [TEMPLATE]", testUserUsername);
        Assert.assertEquals(expectedSubject, message.getSubject());

        afterEmailSubjectTemplateTest();
    }

}
