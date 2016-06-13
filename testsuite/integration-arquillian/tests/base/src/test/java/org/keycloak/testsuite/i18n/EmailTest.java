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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.UserModel;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.GreenMailRule;

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

    private void changeUserLocale(String locale) {
        UserRepresentation user = findUser("login-test");
        if (user.getAttributes() == null) user.setAttributes(new HashMap<String, Object>());
        user.getAttributes().put(UserModel.LOCALE, Collections.singletonList(locale));
        ApiUtil.findUserByUsernameId(testRealm(), "login-test").update(user);
    }

    @Test
    public void restPasswordEmail() throws IOException, MessagingException {
        loginPage.open();
        loginPage.resetPassword();
        resetPasswordPage.changePassword("login-test");

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        Assert.assertEquals("Reset password", message.getSubject());

        changeUserLocale("en");

        loginPage.open();
        loginPage.resetPassword();

        resetPasswordPage.changePassword("login-test");

        assertEquals(2, greenMail.getReceivedMessages().length);

        message = greenMail.getReceivedMessages()[1];

        Assert.assertEquals("Reset password", message.getSubject());
    }

    @Test
    public void restPasswordEmailGerman() throws IOException, MessagingException {
        changeUserLocale("de");

        loginPage.open();
        loginPage.resetPassword();
        resetPasswordPage.changePassword("login-test");

        assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        Assert.assertEquals("Passwort zurückzusetzen", message.getSubject());
    }

}
