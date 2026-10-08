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
package org.keycloak.tests.organization.admin;

import java.io.IOException;
import java.net.URI;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.authentication.actiontoken.inviteorg.InviteOrgActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.UriUtils;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.InfoPage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OrganizationInvitationTokenTest extends AbstractOrganizationTest {

    private static final String INVITEE_EMAIL = "invited@example.org";

    @InjectMailServer
    MailServer mailServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    InfoPage infoPage;

    @InjectPage
    ErrorPage errorPage;

    @InjectPage
    RegisterPage registerPage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void testInvitationIsConsumedOnlyAfterConfirmation() throws IOException {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        realm.addUser(UserConfigBuilder.create().username("invitee").email(INVITEE_EMAIL)
                .name("Invited", "User").password("password").emailVerified(true));
        String userId = getUserRepresentation(INVITEE_EMAIL).getId();
        organization.members().inviteExistingUser(userId).close();

        String link = getInvitationLink(mailServer);
        driver.open(link);
        infoPage.assertCurrent();
        assertEquals(0L, organization.members().count());
        assertEquals(1, organization.invitations().list().size());

        driver.findElement(By.cssSelector("a[href*='login-actions/action-token']")).click();
        assertEquals(1L, organization.members().count());
        assertTrue(organization.invitations().list().isEmpty());
        assertInvitationConsumed(link);

        driver.open(link);
        errorPage.assertCurrent();
        assertEquals(1L, organization.members().count());
    }

    @Test
    public void testRegistrationConsumesInvitation() throws IOException {
        OrganizationResource organization = inviteNewUser();
        String link = getInvitationLink(mailServer);
        driver.open(link);
        registerPage.assertCurrent();
        registerPage.register("Invited", "User", INVITEE_EMAIL, "invitee", "password");

        assertEquals(1L, organization.members().count());
        assertTrue(organization.invitations().list().isEmpty());
        assertInvitationConsumed(link);
    }

    @Test
    public void testUsedInvitationRollsBackRegistration() throws IOException {
        OrganizationResource organization = inviteNewUser();
        String link = getInvitationLink(mailServer);
        driver.open(link);
        registerPage.assertCurrent();
        String tokenString = getInvitationToken(link);

        // Simulate another acceptance while this browser is still on the registration form.
        runOnServer.run(session -> {
            InviteOrgActionToken token = parseInvitationToken(tokenString);
            assertTrue(session.singleUseObjects().putIfAbsent(token.serializeKey() + SingleUseObjectProvider.REVOKED_KEY,
                    token.getExp() - Time.currentTime() + 1));
        });

        registerPage.register("Invited", "User", INVITEE_EMAIL, "invitee", "password");
        errorPage.assertCurrent();
        assertThat(errorPage.getError(), containsString("The link you clicked is no longer valid"));
        assertTrue(realm.admin().users().searchByEmail(INVITEE_EMAIL, true).isEmpty());
        assertEquals(0L, organization.members().count());
    }

    private void assertInvitationConsumed(String link) {
        String tokenString = getInvitationToken(link);
        runOnServer.run(session -> {
            InviteOrgActionToken token = parseInvitationToken(tokenString);
            assertTrue(session.singleUseObjects().contains(token.serializeKey() + SingleUseObjectProvider.REVOKED_KEY), "Accepted invitation token must be consumed");
        });
    }

    private static InviteOrgActionToken parseInvitationToken(String tokenString) throws IOException {
        try {
            return new JWSInput(tokenString).readJsonContent(InviteOrgActionToken.class);
        } catch (JWSInputException e) {
            throw new IOException(e);
        }
    }

    private static String getInvitationToken(String link) {
        var parameters = UriUtils.parseQueryParameters(URI.create(link).getRawQuery(), false);
        String token = parameters.getFirst("token") != null ? parameters.getFirst("token") : parameters.getFirst("key");

        assertNotNull(token, "Invitation URL must contain its token");
        return token;
    }

    private OrganizationResource inviteNewUser() {
        realm.updateWithCleanup(r -> r.registrationAllowed(true));
        realm.cleanup().add(r -> r.users().searchByEmail(INVITEE_EMAIL, true)
                .forEach(user -> r.users().get(user.getId()).remove()));
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        organization.members().inviteUser(INVITEE_EMAIL, "Invited", "User").close();
        return organization;
    }

}
