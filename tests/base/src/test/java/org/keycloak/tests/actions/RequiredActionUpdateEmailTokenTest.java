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
package org.keycloak.tests.actions;

import java.io.IOException;
import java.net.URI;

import org.keycloak.TokenVerifier;
import org.keycloak.authentication.actiontoken.updateemail.UpdateEmailActionToken;
import org.keycloak.common.util.UriUtils;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.InfoPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.MailUtils;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class RequiredActionUpdateEmailTokenTest {

    private static final String OLD_EMAIL = "update-email@localhost";
    private static final String NEW_EMAIL = "updated-email@localhost";
    private static final String PASSWORD = "password";

    @InjectRealm(config = UpdateEmailRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(config = UpdateEmailUserConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedUser user;

    @InjectMailServer
    MailServer mailServer;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    InfoPage infoPage;

    @InjectPage
    ErrorPage errorPage;

    @Test
    public void testUpdateEmailTokenIsConsumed() throws IOException {
        String link = requestEmailUpdate();

        driver.open(link);
        infoPage.assertCurrent();
        assertEquals(NEW_EMAIL, user.admin().toRepresentation().getEmail());
        assertTokenConsumed(link);

        // Put the user back into the state the token was issued for, the used link must still be rejected
        String userId = user.getId();
        runOnServer.run(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            UserModel userModel = session.users().getUserById(realmModel, userId);
            userModel.setEmail(OLD_EMAIL);
            userModel.setSingleAttribute(UserModel.EMAIL_PENDING, NEW_EMAIL);
        });

        driver.open(link);
        errorPage.assertCurrent();
        assertThat(errorPage.getError(), containsString("Action expired"));
        assertEquals(OLD_EMAIL, user.admin().toRepresentation().getEmail());
    }

    private String requestEmailUpdate() throws IOException {
        RequiredActionProviderRepresentation updateEmail = realm.admin().flows().getRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name());
        updateEmail.setEnabled(true);
        realm.admin().flows().updateRequiredAction(updateEmail.getAlias(), updateEmail);
        realm.cleanup().add(r -> {
            updateEmail.setEnabled(false);
            r.flows().updateRequiredAction(updateEmail.getAlias(), updateEmail);
        });

        oauth.openLoginForm();
        loginPage.fillLogin(user.getUsername(), PASSWORD);
        loginPage.submit();

        WebElement email = driver.findElement(By.id("email"));
        email.clear();
        email.sendKeys(NEW_EMAIL);
        driver.findElement(By.id("kc-submit")).click();

        assertTrue(mailServer.waitForIncomingEmail(10_000, 1), "update email confirmation not received");
        return MailUtils.getPasswordResetEmailLink(mailServer.getLastReceivedMessage()).trim();
    }

    private void assertTokenConsumed(String link) {
        String tokenString = getActionToken(link);
        runOnServer.run(session -> {
            UpdateEmailActionToken token = TokenVerifier.create(tokenString, UpdateEmailActionToken.class).getToken();
            assertTrue(session.revokedTokens().contains(token.serializeKey()), "Used update email token must be consumed");
        });
    }

    private static String getActionToken(String link) {
        String token = UriUtils.parseQueryParameters(URI.create(link).getRawQuery(), false).getFirst("key");
        assertNotNull(token, "Update email link must contain its token");
        return token;
    }

    public static class UpdateEmailRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.verifyEmail(true);
        }
    }

    public static class UpdateEmailUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("update-email-user")
                    .email(OLD_EMAIL)
                    .name("Update", "Email")
                    .password(PASSWORD)
                    .emailVerified(true)
                    .requiredActions(UserModel.RequiredAction.UPDATE_EMAIL.name());
        }
    }
}
