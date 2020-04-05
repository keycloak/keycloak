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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Arrays;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.WaitUtils;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
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
    public void restPasswordEmail() throws IOException, MessagingException {
        loginPage.open();
        loginPage.resetPassword();
        resetPasswordPage.changePassword("login-test");

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        Assert.assertEquals("Reset password", message.getSubject());

        changeUserLocale("en");

        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.changePassword("login-test");

        Assert.assertEquals(2, greenMail.getReceivedMessages().length);

        message = greenMail.getReceivedMessages()[1];

        Assert.assertEquals("Reset password", message.getSubject());
    }

    @Test
    public void restPasswordEmailGerman() throws IOException, MessagingException {
        ProfileAssume.assumeCommunity();
        
        changeUserLocale("de");

        loginPage.open();
        loginPage.resetPassword();
        resetPasswordPage.changePassword("login-test");

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        Assert.assertEquals("Passwort zurücksetzen", message.getSubject());

        // Revert
        changeUserLocale("en");
    }

    //KEYCLOAK-7478
    @Test
    public void changeLocaleOnInfoPage() throws InterruptedException, IOException, MessagingException {
        ProfileAssume.assumeCommunity();
              
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
        Assert.assertThat(infoPage.getLanguageDropdownText(), is(equalTo("English")));
        
        infoPage.openLanguage("Deutsch");

        Assert.assertThat(DroneUtils.getCurrentDriver().getPageSource(), containsString("Passwort aktualisieren"));
        
        infoPage.clickToContinueDe();
        
        loginPasswordUpdatePage.openLanguage("English");
        loginPasswordUpdatePage.changePassword("pass", "pass");
        WaitUtils.waitForPageToLoad();
        
        Assert.assertTrue("Expected to be on InfoPage, but it was on " + DroneUtils.getCurrentDriver().getTitle(), infoPage.isCurrent());
        Assert.assertThat(infoPage.getInfo(), containsString("Your account has been updated."));
    }
}
