/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.user.profile;

import java.util.Map;

import jakarta.mail.Address;
import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the update email required action uses the email as converted by the user profile.
 */
@KeycloakIntegrationTest
public class UserProfileConverterUpdateEmailTest {

    private static final String USERNAME = "converter-update-email";
    private static final String PASSWORD = "password";

    @InjectRealm
    ManagedRealm realm;

    @InjectMailServer
    MailServer mailServer;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void testConfirmationIsSentToConvertedEmail() throws Exception {
        UPConfig originalConfig = realm.admin().users().userProfile().getConfiguration();
        UPConfig config = realm.admin().users().userProfile().getConfiguration();
        config.getAttribute(UserModel.EMAIL).addConverter("trim-whitespace", Map.of());
        realm.admin().users().userProfile().update(config);
        realm.cleanup().add(r -> r.users().userProfile().update(originalConfig));

        RequiredActionProviderRepresentation updateEmail = realm.admin().flows().getRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name());
        boolean updateEmailEnabled = updateEmail.isEnabled();
        updateEmail.setEnabled(true);
        realm.admin().flows().updateRequiredAction(updateEmail.getAlias(), updateEmail);
        realm.cleanup().add(r -> {
            RequiredActionProviderRepresentation rep = r.flows().getRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name());
            rep.setEnabled(updateEmailEnabled);
            r.flows().updateRequiredAction(rep.getAlias(), rep);
        });
        realm.updateWithCleanup(r -> r.verifyEmail(true));

        String userId;
        try (Response response = realm.admin().users().create(UserBuilder.create(USERNAME)
                .email("old-" + USERNAME + "@example.org")
                .emailVerified(true)
                .firstName("John")
                .lastName("Doe")
                .password(PASSWORD)
                .requiredActions(UserModel.RequiredAction.UPDATE_EMAIL.name())
                .build())) {
            userId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.users().get(userId).remove());

        oauth.openLoginForm();
        loginPage.fillLogin(USERNAME, PASSWORD);
        loginPage.submit();

        WebElement email = driver.findElement(By.id("email"));
        email.clear();
        email.sendKeys("  new-" + USERNAME + "@example.org  ");
        driver.findElement(By.id("kc-submit")).click();

        // without using the converted email, sending the confirmation fails on the untrimmed address
        assertTrue(mailServer.waitForIncomingEmail(1), "Expected the email update confirmation to be sent");
        MimeMessage message = mailServer.getLastReceivedMessage();
        Address[] recipients = message.getAllRecipients();
        assertEquals(1, recipients.length);
        assertEquals("new-" + USERNAME + "@example.org", recipients[0].toString());
    }
}
