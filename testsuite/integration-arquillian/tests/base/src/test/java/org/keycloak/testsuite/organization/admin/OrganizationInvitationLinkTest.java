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
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

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
        UserRepresentation user = createUser("invited", "invited@myemail.com");

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());

        organization.members().inviteExistingUser(user.getId()).close();

        acceptInvitation(organization, user);
    }

    @Test
    public void testInviteExistingUserWithEmail() throws IOException, MessagingException {
        UserRepresentation user = createUser("invitedWithMatchingEmail", "invitedWithMatchingEmail@myemail.com");

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());

        organization.members().inviteUser(user.getEmail(), "Homer", "Simpson").close();

        acceptInvitation(organization, user);
    }

    @Test
    public void testInviteNewUserRegistration() throws IOException, MessagingException {
        String email = "inviteduser@email";
        String firstName = "Homer";
        String lastName = "Simpson";

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        organization.members().inviteUser(email, firstName, lastName).close();

        registerUser(organization, email);

        List<UserRepresentation> users = testRealm().users().searchByEmail(email, true);
        assertThat(users, Matchers.not(empty()));
        // user is a member
        Assert.assertNotNull(organization.members().member(users.get(0).getId()).toRepresentation());
        getCleanup().addCleanup(() -> testRealm().users().get(users.get(0).getId()).remove());

        // authenticated to the account console
        Assert.assertTrue(driver.getPageSource().contains("Account Management"));
        Assert.assertNotNull(driver.manage().getCookieNamed(CookieType.IDENTITY.getName()));
    }

    @Test
    public void testFailRegistrationNotEnabledWhenInvitingNewUser() {
        String email = "inviteduser@email";

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setRegistrationAllowed(false);
        testRealm().update(realm);
        try (Response response = organization.members().inviteUser(email, null, null)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertEquals("Realm does not allow self-registration", response.readEntity(ErrorRepresentation.class).getErrorMessage());
        } finally {
            realm.setRegistrationAllowed(true);
            testRealm().update(realm);
        }
    }

    @Test
    public void testEmailDoesNotChangeOnRegistration() throws IOException, MessagingException {
        String email = "inviteduser@email";

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        organization.members().inviteUser(email, null, null).close();

        registerUser(organization, "invalid@email.com");

        assertThat(driver.getPageSource(), Matchers.containsString("Email does not match the invitation"));
        assertThat(testRealm().users().searchByEmail(email, true), Matchers.empty());
    }

    private UserRepresentation createUser(String invitedWithMatchingEmail, String mail) {
        UserRepresentation user = UserBuilder.create()
                .username(invitedWithMatchingEmail)
                .email(mail)
                .password("password")
                .enabled(true)
                .build();
        try (Response response = testRealm().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }
        return user;
    }

    private String getInvitationLinkFromEmail(String ...parameters) throws MessagingException, IOException {
        MimeMessage message = greenMail.getLastReceivedMessage();
        Assert.assertNotNull(message);
        Assert.assertEquals("Invitation to join the " + organizationName + " organization", message.getSubject());

        EmailBody body = MailUtils.getBody(message);
        String text = body.getHtml();
        String link = MailUtils.getLink(body.getHtml()).trim();

        if (Arrays.stream(parameters).noneMatch(Predicate.isEqual(null)) && parameters.length == 2) {
            assertThat(text, Matchers.containsString("Hi, " + parameters[0] + " " + parameters[1] + "."));
        }

        assertThat(text, Matchers.containsString(("You were invited to join the " + organizationName + " organization. Click the link below to join. </p>")));
        assertThat(text, Matchers.containsString(("<a href=\"" + link + "\" rel=\"nofollow\">Link to join the organization</a></p>")));
        assertThat(text, Matchers.containsString(("Link to join the organization")));
        assertThat(text, Matchers.containsString(("<p>If you dont want to join the organization, just ignore this message.</p>")));

        String orgToken = UriUtils.parseQueryParameters(link, false).values().stream().map(strings -> strings.get(0)).findFirst().orElse(null);
        Assert.assertNotNull(orgToken);

        return link;
    }

    @Test
    public void testLinkExpired() throws IOException, MessagingException {
        String email = "inviteduser@email";

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        organization.members().inviteUser(email, "Homer", "Simpson").close();

        try {
            setTimeOffset((int) TimeUnit.DAYS.toSeconds(1));

            registerUser(organization, email);

            assertThat(driver.getPageSource(), Matchers.containsString("The provided token is not valid or has expired."));
            assertThat(testRealm().users().searchByEmail(email, true), Matchers.empty());
        } finally {
            resetTimeOffset();
        }
    }

    private void registerUser(OrganizationResource organization, String email) throws MessagingException, IOException {
        String link = getInvitationLinkFromEmail();
        driver.navigate().to(link);
        Assert.assertFalse(organization.members().getAll().stream().anyMatch(actual -> email.equals(actual.getEmail())));
        registerPage.assertCurrent(organizationName);
        driver.manage().timeouts().pageLoadTimeout(1, TimeUnit.DAYS);
        registerPage.register("firstName", "lastName", email,
                "invitedUser", "password", "password", null, false, null);
    }

    private void acceptInvitation(OrganizationResource organization, UserRepresentation user) throws MessagingException, IOException {
        String link = getInvitationLinkFromEmail(user.getFirstName(), user.getLastName());
        driver.navigate().to(link);
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
