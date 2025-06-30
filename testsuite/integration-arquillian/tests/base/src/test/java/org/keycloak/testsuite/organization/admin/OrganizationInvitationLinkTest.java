/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.organization.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.UriUtils;
import org.keycloak.cookie.CookieType;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.MailUtils.EmailBody;
import org.keycloak.testsuite.util.UserBuilder;

@EnableFeature(Feature.ORGANIZATION)
public class OrganizationInvitationLinkTest extends AbstractOrganizationTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected InfoPage infoPage;

    @Page
    protected RegisterPage registerPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        Map<String, String> smtpConfig = testRealm.getSmtpServer();
        super.configureTestRealm(testRealm);
        testRealm.setSmtpServer(smtpConfig);
    }

    @Test
    public void testInviteExistingUser() throws IOException, MessagingException {
        UserRepresentation user = UserBuilder.create()
                .username("invited")
                .email("invited@myemail.com")
                .password("password")
                .enabled(true)
                .build();
        try (Response response = testRealm().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());

        organization.members().inviteExistingUser(user.getId()).close();

        acceptInvitation(organization, user);
    }

    @Test
    public void testInviteExistingUserWithEmail() throws IOException, MessagingException {
        UserRepresentation user = UserBuilder.create()
                .username("invitedWithMatchingEmail")
                .email("invitedWithMatchingEmail@myemail.com")
                .password("password")
                .enabled(true)
                .build();
        try (Response response = testRealm().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());

        organization.members().inviteUser(user.getEmail(), "Homer", "Simpson").close();

        acceptInvitation(organization, user);
    }

    @Test
    public void testInviteNewUserRegistration() throws IOException, MessagingException {
        UserRepresentation user = UserBuilder.create()
                .username("invitedUser")
                .email("inviteduser@email")
                .enabled(true)
                .build();
        // User isn't created when we send the invite
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        organization.members().inviteUser(user.getEmail(), null, null).close();

        MimeMessage message = greenMail.getLastReceivedMessage();
        Assert.assertNotNull(message);
        Assert.assertEquals("Invitation to join the " + organizationName + " organization", message.getSubject());
        EmailBody body = MailUtils.getBody(message);
        String link = MailUtils.getLink(body.getHtml());
        String text = body.getHtml();
        assertTrue(text.contains("<p>You were invited to join the " + organizationName + " organization. Click the link below to join. </p>"));
        assertTrue(text.contains("<a href=\"" + link + "\" rel=\"nofollow\">Link to join the organization</a></p>"));
        assertTrue(text.contains("Link to join the organization"));
        assertTrue(text.contains("<p>If you dont want to join the organization, just ignore this message.</p>"));
        String orgToken = UriUtils.parseQueryParameters(link, false).values().stream().map(strings -> strings.get(0)).findFirst().orElse(null);
        Assert.assertNotNull(orgToken);
        driver.navigate().to(link.trim());
        Assert.assertFalse(organization.members().getAll().stream().anyMatch(actual -> user.getId().equals(actual.getId())));
        registerPage.assertCurrent(organizationName);
        registerPage.register("firstName", "lastName", user.getEmail(),
                user.getUsername(), "password", "password", null, false, null);
        List<UserRepresentation> users = testRealm().users().searchByEmail(user.getEmail(), true);
        Assert.assertFalse(users.isEmpty());
        // user is a member
        Assert.assertNotNull(organization.members().member(users.get(0).getId()).toRepresentation());
        getCleanup().addCleanup(() -> testRealm().users().get(users.get(0).getId()).remove());

        // authenticated to the account console
        Assert.assertTrue(driver.getPageSource().contains("Account Management"));
        Assert.assertNotNull(driver.manage().getCookieNamed(CookieType.IDENTITY.getName()));
    }

    @Test
    public void testFailRegistrationNotEnabledWhenInvitingNewUser() throws IOException, MessagingException {
        UserRepresentation user = UserBuilder.create()
                .username("invitedUser")
                .email("inviteduser@email")
                .enabled(true)
                .build();
        // User isn't created when we send the invite
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setRegistrationAllowed(false);
        testRealm().update(realm);
        try (Response response = organization.members().inviteUser(user.getEmail(), null, null)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertEquals("Realm does not allow self-registration", response.readEntity(ErrorRepresentation.class).getErrorMessage());
        } finally {
            realm.setRegistrationAllowed(true);
            testRealm().update(realm);
        }
    }

    @Test
    public void testEmailDoesNotChangeOnRegistration() throws IOException {
        UserRepresentation user = UserBuilder.create()
                .username("invitedUser")
                .email("inviteduser@email")
                .enabled(true)
                .build();
        // User isn't created when we send the invite
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        organization.members().inviteUser(user.getEmail(), null, null).close();

        MimeMessage message = greenMail.getLastReceivedMessage();
        Assert.assertNotNull(message);
        EmailBody body = MailUtils.getBody(message);
        String link = MailUtils.getLink(body.getHtml());
        String orgToken = UriUtils.parseQueryParameters(link, false).values().stream().map(strings -> strings.get(0)).findFirst().orElse(null);
        Assert.assertNotNull(orgToken);
        driver.navigate().to(link.trim());
        Assert.assertFalse(organization.members().getAll().stream().anyMatch(actual -> user.getId().equals(actual.getId())));
        registerPage.assertCurrent(organizationName);
        registerPage.register("firstName", "lastName", "invalid@email.com",
                user.getUsername(), "password", "password", null, false, null);
        Assert.assertTrue(driver.getPageSource().contains("Email does not match the invitation"));
        List<UserRepresentation> users = testRealm().users().searchByEmail(user.getEmail(), true);
        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void testLinkExpired() throws IOException {
        UserRepresentation user = UserBuilder.create()
                .username("invitedUser")
                .email("inviteduser@email")
                .enabled(true)
                .build();
        // User isn't created when we send the invite
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        organization.members().inviteUser(user.getEmail(), "Homer", "Simpson").close();

        try {
            setTimeOffset((int) TimeUnit.DAYS.toSeconds(1));
            MimeMessage message = greenMail.getLastReceivedMessage();
            Assert.assertNotNull(message);
            EmailBody body = MailUtils.getBody(message);
            String link = MailUtils.getLink(body.getHtml());
            String orgToken = UriUtils.parseQueryParameters(link, false).values().stream().map(strings -> strings.get(0)).findFirst().orElse(null);
            Assert.assertNotNull(orgToken);
            driver.navigate().to(link.trim());
            Assert.assertFalse(organization.members().getAll().stream().anyMatch(actual -> user.getId().equals(actual.getId())));
            registerPage.assertCurrent(organizationName);
            driver.manage().timeouts().pageLoadTimeout(1, TimeUnit.DAYS);
            registerPage.register("firstName", "lastName", "invalid@email.com",
                    user.getUsername(), "password", "password", null, false, null);
            Assert.assertTrue(driver.getPageSource().contains("The provided token is not valid or has expired."));
            List<UserRepresentation> users = testRealm().users().searchByEmail(user.getEmail(), true);
            Assert.assertTrue(users.isEmpty());
        } finally {
            resetTimeOffset();
        }
    }

    private void acceptInvitation(OrganizationResource organization, UserRepresentation user) throws MessagingException, IOException {
        MimeMessage message = greenMail.getLastReceivedMessage();
        Assert.assertNotNull(message);
        Assert.assertEquals("Invitation to join the " + organizationName + " organization", message.getSubject());
        EmailBody body = MailUtils.getBody(message);
        if (user.getFirstName() != null && user.getLastName() != null) {
            assertThat(body.getText(), Matchers.containsString("Hi, " + user.getFirstName() + " " + user.getLastName() + "."));
        }
        String link = MailUtils.getLink(body.getHtml());
        driver.navigate().to(link.trim());
        // not yet a member
        Assert.assertFalse(organization.members().getAll().stream().anyMatch(actual -> user.getId().equals(actual.getId())));
        // confirm the intent of membership
        assertThat(driver.getPageSource(), containsString("You are about to join organization " + organizationName));
        assertThat(infoPage.getInfo(), containsString("By clicking on the link below, you will become a member of the " + organizationName + " organization:"));
        infoPage.clickToContinue();
        // redirect to the account console and eventually force the user to authenticate if not already
        assertThat(driver.getTitle(), containsString("Account Management"));
        // now a member
        Assert.assertNotNull(organization.members().member(user.getId()).toRepresentation());
    }
}
