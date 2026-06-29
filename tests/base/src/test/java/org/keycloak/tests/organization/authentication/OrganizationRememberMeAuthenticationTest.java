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

package org.keycloak.tests.organization.authentication;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.page.SelectOrganizationPage;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the Remember Me note is preserved through the identity-first
 * login flow rendered by the {@code OrganizationAuthenticator}, both for the
 * initial login and when reopening the form after the user session is destroyed.
 */
@KeycloakIntegrationTest
public class OrganizationRememberMeAuthenticationTest extends AbstractOrganizationTest {

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginUsernamePage usernamePage;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    SelectOrganizationPage selectOrganizationPage;

    @InjectEvents
    Events events;

    @Test
    public void testRememberMeWithOrganizationScope() {
        realm.updateWithCleanup(r -> r.setRememberMe(true));

        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization, "contractor@contractor.org", "Contractor", "User");
        String email = member.getEmail();

        oauth.scope("organization").openLoginForm();
        usernamePage.assertCurrent();
        usernamePage.rememberMe(true);
        assertTrue(usernamePage.isRememberMe());
        usernamePage.fillLoginWithUsernameOnly(email);
        usernamePage.submit();

        loginPage.assertCurrent();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        oauth.parseLoginResponse();

        List<UserSessionRepresentation> sessions = realm.admin().users().get(member.getId()).getUserSessions();
        assertEquals(1, sessions.size());
        assertTrue(sessions.get(0).isRememberMe(),
                "User session should have rememberMe enabled");

        EventRepresentation login1 = awaitNextEvent();
        EventAssertion.assertSuccess(login1)
                .type(EventType.LOGIN)
                .clientId("test-app")
                .userId(member.getId())
                .details(Details.USERNAME, email)
                .details(Details.REMEMBER_ME, "true");

        realm.admin().deleteSession(login1.getSessionId(), false);

        oauth.scope("organization").openLoginForm();
        usernamePage.assertCurrent();
        assertTrue(usernamePage.isRememberMe(),
                "rememberMe should be pre-checked after session expiry");
        assertEquals(email, usernamePage.getUsername());

        usernamePage.rememberMe(false);
        usernamePage.submit();

        loginPage.assertCurrent();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        oauth.parseLoginResponse();

        EventRepresentation login2 = awaitNextEvent();
        EventAssertion.assertSuccess(login2)
                .type(EventType.LOGIN)
                .clientId("test-app")
                .userId(member.getId())
                .details(Details.USERNAME, email)
                .withoutDetails(Details.REMEMBER_ME);

        realm.admin().deleteSession(login2.getSessionId(), false);

        oauth.scope("organization").openLoginForm();
        usernamePage.assertCurrent();
        assertFalse(usernamePage.isRememberMe(),
                "rememberMe should not be pre-checked after disabling it");
        assertEquals("", usernamePage.getUsername(),
                "username field should be empty after session expiry without rememberMe");
    }

    @Test
    public void testRememberMeNotCheckedWithOrganizationScope() {
        realm.updateWithCleanup(r -> r.setRememberMe(true));

        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization, "contractor@contractor.org", "Contractor", "User");
        String email = member.getEmail();

        oauth.scope("organization").openLoginForm();
        usernamePage.assertCurrent();
        assertFalse(usernamePage.isRememberMe());
        usernamePage.fillLoginWithUsernameOnly(email);
        usernamePage.submit();

        loginPage.assertCurrent();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        oauth.parseLoginResponse();

        List<UserSessionRepresentation> sessions = realm.admin().users().get(member.getId()).getUserSessions();
        assertEquals(1, sessions.size());
        assertFalse(sessions.get(0).isRememberMe(),
                "User session should not have rememberMe enabled");

        EventRepresentation login = awaitNextEvent();
        EventAssertion.assertSuccess(login)
                .type(EventType.LOGIN)
                .clientId("test-app")
                .userId(member.getId())
                .details(Details.USERNAME, email)
                .withoutDetails(Details.REMEMBER_ME);
    }

    @Test
    public void testRememberMePreservedThroughSelectOrganization() {
        realm.updateWithCleanup(r -> r.setRememberMe(true));

        OrganizationRepresentation orgA = createOrganization("orga");
        OrganizationRepresentation orgB = createOrganization("orgb");
        OrganizationResource orgAResource = realm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = realm.admin().organizations().get(orgB.getId());

        UserRepresentation member = addMember(orgAResource, "member@orga.org", "Member", "User");
        orgBResource.members().addMember(member.getId()).close();
        String email = member.getEmail();

        oauth.scope("organization").openLoginForm();
        usernamePage.assertCurrent();
        usernamePage.rememberMe(true);
        usernamePage.fillLoginWithUsernameOnly(email);
        usernamePage.submit();

        selectOrganizationPage.assertCurrent();
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        selectOrganizationPage.selectOrganization(orgA.getAlias());

        loginPage.assertCurrent();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        oauth.parseLoginResponse();

        // Regression check: the select-organization round-trip must not wipe the rememberMe authNote.
        List<UserSessionRepresentation> sessions = realm.admin().users().get(member.getId()).getUserSessions();
        assertEquals(1, sessions.size());
        assertTrue(sessions.get(0).isRememberMe(),
                "rememberMe should survive the select-organization round-trip");

        EventRepresentation login = awaitNextEvent();
        EventAssertion.assertSuccess(login)
                .type(EventType.LOGIN)
                .clientId("test-app")
                .userId(member.getId())
                .details(Details.USERNAME, email)
                .details(Details.REMEMBER_ME, "true");
    }

    // The login event is published asynchronously after the OAuth callback completes,
    // so a single events.poll() may return null between callback receipt and event-store persistence.
    private EventRepresentation awaitNextEvent() {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(50))
                .until(events::poll, Objects::nonNull);
    }
}
