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

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.util.UriUtils;
import org.keycloak.cookie.CookieType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAuthenticationTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.authentication.PushButtonAuthenticatorFactory;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.updaters.OrganizationAttributeUpdater;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.MailUtils.EmailBody;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class OrganizationInvitationLinkTest extends AbstractOrganizationTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected InfoPage infoPage;

    @Page
    protected RegisterPage registerPage;

    @Before
    public void setDriverTimeout() {
        driver.manage().timeouts().pageLoadTimeout(Duration.ofMinutes(1));
    }

    @Before
    public void disableSelfRegistration() {
        RealmRepresentation representation = testRealm().toRepresentation();
        representation.setRegistrationAllowed(false);
        testRealm().update(representation);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        Map<String, String> smtpConfig = testRealm.getSmtpServer();
        super.configureTestRealm(testRealm);
        testRealm.setRegistrationAllowed(false);
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
    public void testInviteExistingUserCustomRedirectUrl() throws IOException, MessagingException {
        UserRepresentation user = createUser("invited", "invited@myemail.com");

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());

        try (
            OrganizationAttributeUpdater oau = new OrganizationAttributeUpdater(organization).setRedirectUrl(OAuthClient.APP_AUTH_ROOT).update();
            Response response = organization.members().inviteExistingUser(user.getId());
        ) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));

            acceptInvitation(organization, user, "AUTH_RESPONSE");
        }
    }

    @Test
    public void testRedirectAfterClickingSecondTimeOnInvitation() throws IOException, MessagingException {
        UserRepresentation user = createUser("invited", "invited@myemail.com");

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());

        try (
                OrganizationAttributeUpdater oau = new OrganizationAttributeUpdater(organization).setRedirectUrl(OAuthClient.APP_AUTH_ROOT).update();
                Response response = organization.members().inviteExistingUser(user.getId());
        ) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));

            acceptInvitation(organization, user, "AUTH_RESPONSE");

            String link = getInvitationLinkFromEmail(user.getFirstName(), user.getLastName());
            driver.navigate().to(link);

            assertThat(driver.getPageSource(), containsString("You are already a member of the neworg organization."));

            infoPage.clickBackToApplicationLink();
            // redirect to the redirectUrl of the organization
            assertThat(driver.getTitle(), containsString("AUTH_RESPONSE"));
        }
    }

    @Test
    public void testInviteExistingUserWithEmail() throws IOException, MessagingException {
        UserRepresentation user = createUser("invitedWithMatchingEmail", "invitedWithMatchingEmail@myemail.com");

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());

        organization.members().inviteUser(user.getEmail(), "Homer", "Simpson").close();

        acceptInvitation(organization, user);
    }

    @Test
    public void testInviteExistingUserWithEmailCustomRedirectUrl() throws IOException, MessagingException {
        UserRepresentation user = createUser("invitedWithMatchingEmail", "invitedWithMatchingEmail@myemail.com");

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());

        try (
            OrganizationAttributeUpdater oau = new OrganizationAttributeUpdater(organization).setRedirectUrl(OAuthClient.APP_AUTH_ROOT).update();
            Response response = organization.members().inviteUser(user.getEmail(), "Homer", "Simpson");
        ) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));

            acceptInvitation(organization, user, "AUTH_RESPONSE");
        }
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
        MemberRepresentation member = organization.members().member(users.get(0).getId()).toRepresentation();
        Assert.assertNotNull(member);
        assertThat(member.getMembershipType(), equalTo(MembershipType.MANAGED));
        getCleanup().addCleanup(() -> testRealm().users().get(users.get(0).getId()).remove());

        // authenticated to the account console
        Assert.assertTrue(driver.getPageSource().contains("Account Management"));
        Assert.assertNotNull(driver.manage().getCookieNamed(CookieType.IDENTITY.getName()));
    }

    @Test
    public void testInviteNewUserRegistrationCustomRegistrationFlow() throws IOException, MessagingException {
        String registrationFlowAlias = "custom-registration-flow";
        HashMap<String, Object> params = new HashMap<>();
        params.put("newName", registrationFlowAlias);
        testRealm().flows().copy(DefaultAuthenticationFlows.REGISTRATION_FLOW, params).close();
        String flowId = AbstractAuthenticationTest.findFlowByAlias(registrationFlowAlias, testRealm().flows().getFlows()).getId();
        AuthenticationExecutionRepresentation execution = new AuthenticationExecutionRepresentation();
        execution.setParentFlow(flowId);
        execution.setAuthenticator(PushButtonAuthenticatorFactory.PROVIDER_ID);
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString());
        testRealm().flows().addExecution(execution).close();
        RealmRepresentation realm = testRealm().toRepresentation();
        assertThat(realm.isRegistrationAllowed(), is(false));
        realm.setRegistrationFlow(registrationFlowAlias);
        testRealm().update(realm);
        getCleanup().addCleanup(() -> {
            realm.setRegistrationFlow(DefaultAuthenticationFlows.REGISTRATION_FLOW);
        });

        String email = "inviteduser@email";
        String firstName = "Homer";
        String lastName = "Simpson";

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        organization.members().inviteUser(email, firstName, lastName).close();

        registerUser(organization, email);
        driver.findElement(By.name("submit1")).click();
        List<UserRepresentation> users = testRealm().users().searchByEmail(email, true);
        assertThat(users, Matchers.not(empty()));
        // user is a member
        MemberRepresentation member = organization.members().member(users.get(0).getId()).toRepresentation();
        Assert.assertNotNull(member);
        assertThat(member.getMembershipType(), equalTo(MembershipType.MANAGED));
        getCleanup().addCleanup(() -> testRealm().users().get(users.get(0).getId()).remove());

        // authenticated to the account console
        Assert.assertTrue(driver.getPageSource().contains("Account Management"));
        Assert.assertNotNull(driver.manage().getCookieNamed(CookieType.IDENTITY.getName()));
    }

    @Test
    public void testInviteNewUserRegistrationCustomRedirectUrl() throws IOException, MessagingException {
        String email = "inviteduser@email";
        String firstName = "Homer";
        String lastName = "Simpson";

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        try (
            OrganizationAttributeUpdater oau = new OrganizationAttributeUpdater(organization).setRedirectUrl(OAuthClient.APP_AUTH_ROOT).update();
            Response response = organization.members().inviteUser(email, firstName, lastName);
        ) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));

            registerUser(organization, email);

            List<UserRepresentation> users = testRealm().users().searchByEmail(email, true);
            assertThat(users, Matchers.not(empty()));
            // user is a member
            MemberRepresentation member = organization.members().member(users.get(0).getId()).toRepresentation();
            Assert.assertNotNull(member);
            assertThat(member.getMembershipType(), equalTo(MembershipType.MANAGED));
            getCleanup().addCleanup(() -> testRealm().users().get(users.get(0).getId()).remove());

            // authenticated to the app
            assertThat(driver.getTitle(), containsString("AUTH_RESPONSE"));
        }
    }

    @Test
    public void testRegistrationEnabledWhenInvitingNewUser() throws Exception {
        String email = "inviteduser@email";

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        try (
                RealmAttributeUpdater rau = new RealmAttributeUpdater(testRealm()).setRegistrationAllowed(Boolean.TRUE).update();
                Response response = organization.members().inviteUser(email, null, null)
            ) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));

            registerUser(organization, email);

            // authenticated to the account console
            Assert.assertTrue(driver.getPageSource().contains("Account Management"));
            Assert.assertNotNull(driver.manage().getCookieNamed(CookieType.IDENTITY.getName()));

            List<MemberRepresentation> memberByEmail = organization.members().search(email, Boolean.TRUE, null, null);
            assertThat(memberByEmail, Matchers.hasSize(1));
            assertThat(memberByEmail.get(0).getMembershipType(), equalTo(MembershipType.MANAGED));
        }
    }

    @Test
    public void testRegistrationEnabledWhenInvitingNewUserWithLocalization() throws Exception {
        String email = "inviteduser@email";

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        try (
                RealmAttributeUpdater rau = new RealmAttributeUpdater(testRealm()).setRegistrationAllowed(Boolean.TRUE).update();
                Response response = organization.members().inviteUser(email, null, null)
        ) {
            RealmRepresentation realmRep = testRealm().toRepresentation();
            Boolean internationalizationEnabled = realmRep.isInternationalizationEnabled();
            realmRep.setInternationalizationEnabled(true);
            realmRep.setSupportedLocales(Set.of("en", "pt-BR"));
            testRealm().update(realmRep);
            getCleanup().addCleanup(() -> {
                realmRep.setInternationalizationEnabled(internationalizationEnabled);
                testRealm().update(realmRep);
            });

            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));

            String link = getInvitationLinkFromEmail();
            driver.navigate().to(link);
            Assert.assertFalse(organization.members().list(-1, -1).stream().anyMatch(actual -> email.equals(actual.getEmail())));
            registerPage.assertCurrent(organizationName);
            registerPage.openLanguage("Portuguese");
            Assert.assertTrue(driver.getPageSource().contains("Campos obrigatórios"));
            registerPage.register("firstName", "lastName", email,
                    "invitedUser", "password", "password", null, false, null);
            // authenticated to the account console
            Assert.assertTrue(driver.getPageSource().contains("Account Management"));
            Assert.assertNotNull(driver.manage().getCookieNamed(CookieType.IDENTITY.getName()));

            List<MemberRepresentation> memberByEmail = organization.members().search(email, Boolean.TRUE, null, null);
            assertThat(memberByEmail, Matchers.hasSize(1));
            assertThat(memberByEmail.get(0).getMembershipType(), equalTo(MembershipType.MANAGED));
        }
    }

    @Test
    public void testEmailDoesNotChangeOnRegistration() throws IOException, MessagingException {
        String email = "inviteduser@email";

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        organization.members().inviteUser(email, null, null).close();

        registerUser(organization, email, "invalid@email.com");

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
        getCleanup().addUserId(user.getId());
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
        assertThat(text, Matchers.containsString(("This link will expire within 12 hours")));
        assertThat(text, Matchers.containsString(("<p>If you don&#39;t want to join the organization, just ignore this message.</p>")));

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

            String link = getInvitationLinkFromEmail();
            driver.navigate().to(link);

            assertThat(driver.getPageSource(), Matchers.containsString("Action expired."));
            assertThat(testRealm().users().searchByEmail(email, true), Matchers.empty());
        } finally {
            resetTimeOffset();
        }
    }

    private void registerUser(OrganizationResource organization, String email) throws MessagingException, IOException {
        registerUser(organization, email, email);
    }

    private void registerUser(OrganizationResource organization, String expectedEmail, String email) throws MessagingException, IOException {
        String link = getInvitationLinkFromEmail();
        driver.navigate().to(link);
        Assert.assertFalse(organization.members().list(-1, -1).stream().anyMatch(actual -> email.equals(actual.getEmail())));
        registerPage.assertCurrent(organizationName);
        assertThat(registerPage.getEmail(), equalTo(expectedEmail));
        registerPage.register("firstName", "lastName", email,
                "invitedUser", "password", "password", null, false, null);
    }

    private void acceptInvitation(OrganizationResource organization, UserRepresentation user) throws MessagingException, IOException {
        acceptInvitation(organization, user, "Account Management");
    }

    private void acceptInvitation(OrganizationResource organization, UserRepresentation user, String pageTitle) throws MessagingException, IOException {
        String link = getInvitationLinkFromEmail(user.getFirstName(), user.getLastName());
        driver.navigate().to(link);
        // not yet a member
        Assert.assertFalse(organization.members().list(-1, -1).stream().anyMatch(actual -> user.getId().equals(actual.getId())));
        // confirm the intent of membership
        assertThat(driver.getPageSource(), containsString("You are about to join organization " + organizationName));
        assertThat(infoPage.getInfo(), containsString("By clicking on the link below, you will become a member of the " + organizationName + " organization:"));
        infoPage.clickToContinue();
        // redirect to the redirectUrl and eventually force the user to authenticate if not already
        assertThat(driver.getTitle(), containsString(pageTitle));
        // now a member
        Assert.assertNotNull(organization.members().member(user.getId()).toRepresentation());
    }
}
