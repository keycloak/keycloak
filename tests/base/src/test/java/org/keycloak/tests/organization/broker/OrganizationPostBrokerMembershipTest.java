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

package org.keycloak.tests.organization.broker;

import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationIdentityProviderLinkRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for V4 post-broker membership: domain-gated auto-membership with per-association config.
 *
 * Verifies {@code IdpAddOrganizationMemberAuthenticator} iterates all orgs linked to the
 * authenticating IdP, applies auto-membership (AM) and domain-gate checks, and uses the
 * per-link membership type (MT) configuration.
 *
 * Scenario numbering references discussion #49091.
 */
@KeycloakIntegrationTest
public class OrganizationPostBrokerMembershipTest {

    private static final String IDP_ALIAS = "shared-idp";
    private static final String CLIENT_ID = "broker-app";
    private static final String CLIENT_SECRET = "broker-secret";

    @InjectRealm(ref = "provider", config = AbstractOrganizationTest.ProviderRealmConf.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", config = AbstractOrganizationTest.OrganizationRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(ref = "consumer", realmRef = "consumer")
    OAuthClient oAuth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginUsernamePage loginUsernamePage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    // ==================== Group 1: SP-Initiated — New User ====================

    /**
     * Scenario 1.1: Single IdP linked to single org with matching domain.
     * AM=true, MT=MANAGED. User's email matches the org domain.
     */
    @Test
    public void testSingleOrgDomainMatchAutoAdded() {
        setupBroker(IDP_ALIAS);
        createProviderUser("alice", "alice@acme.com", "password");
        OrganizationRepresentation orgA = createOrg("org-a", "acme.com");
        linkIdpToOrg(orgA.getId(), IDP_ALIAS, true, "MANAGED");
        setDomainRouting(orgA.getId(), "acme.com", IDP_ALIAS);

        String userId = loginViaBrokerAndGetUserId("alice@acme.com", "alice", "password");

        assertIsMemberWithType(userId, orgA.getId(), MembershipType.MANAGED);
    }

    /**
     * Scenario 1.5: Same org with two domains routed to different IdPs, each with a different MT.
     * acme.com → idp-x (MT=MANAGED), partner.com → idp-y (MT=UNMANAGED).
     */
    @Test
    public void testDifferentIdpsSameOrgDifferentMembershipTypes() {
        String idpX = "idp-x";
        String idpY = "idp-y";
        setupBroker(idpX);
        setupBroker(idpY);
        createProviderUser("alice", "alice@acme.com", "password");
        createProviderUser("bob", "bob@partner.com", "password");

        OrganizationRepresentation orgA = createOrg("org-a", "acme.com", "partner.com");
        linkIdpToOrg(orgA.getId(), idpX, true, "MANAGED");
        linkIdpToOrg(orgA.getId(), idpY, true, "UNMANAGED");
        setDomainRouting(orgA.getId(), "acme.com", idpX);
        setDomainRouting(orgA.getId(), "partner.com", idpY);

        String aliceId = loginViaBrokerAndGetUserId("alice@acme.com", "alice", "password");
        assertIsMemberWithType(aliceId, orgA.getId(), MembershipType.MANAGED);

        logoutConsumerUser(aliceId);

        String bobId = loginViaBrokerAndGetUserId("bob@partner.com", "bob", "password");
        assertIsMemberWithType(bobId, orgA.getId(), MembershipType.UNMANAGED);
    }

    /**
     * Scenario 1.9: Same IdP linked to two orgs with different domains.
     * Domain gate blocks cross-org membership.
     */
    @Test
    public void testDomainGateBlocksCrossOrgMembership() {
        setupBroker(IDP_ALIAS);
        createProviderUser("alice", "alice@acme.com", "password");
        OrganizationRepresentation orgA = createOrg("org-a", "acme.com");
        OrganizationRepresentation orgB = createOrg("org-b", "globex.com");

        linkIdpToOrg(orgA.getId(), IDP_ALIAS, true, "MANAGED");
        linkIdpToOrg(orgB.getId(), IDP_ALIAS, true, "UNMANAGED");
        setDomainRouting(orgA.getId(), "acme.com", IDP_ALIAS);

        String userId = loginViaBrokerAndGetUserId("alice@acme.com", "alice", "password");

        assertIsMemberWithType(userId, orgA.getId(), MembershipType.MANAGED);
        assertIsNotMember(userId, orgB.getId());
    }

    // ==================== Group 2: SP-Initiated — Existing User ====================

    /**
     * Scenario 2.2 (simplified): Same IdP linked to two orgs.
     * Org-A has AM=true, Org-B has AM=false (domain-less so gate would pass).
     * AM=false prevents auto-membership regardless of domain gate.
     */
    @Test
    public void testAutoMembershipFalseSkipsOrg() {
        setupBroker(IDP_ALIAS);
        createProviderUser("alice", "alice@acme.com", "password");
        OrganizationRepresentation orgA = createOrg("org-a", "acme.com");
        OrganizationRepresentation orgB = createOrg("org-b");

        linkIdpToOrg(orgA.getId(), IDP_ALIAS, true, "UNMANAGED");
        linkIdpToOrg(orgB.getId(), IDP_ALIAS, false, "UNMANAGED");
        setDomainRouting(orgA.getId(), "acme.com", IDP_ALIAS);

        String userId = loginViaBrokerAndGetUserId("alice@acme.com", "alice", "password");

        assertIsMemberWithType(userId, orgA.getId(), MembershipType.UNMANAGED);
        assertIsNotMember(userId, orgB.getId());
    }

    /**
     * Scenario 2.4: Membership type NOT upgraded when user is already a member.
     * addManagedMember returns false if user is already in the org group — no MT upgrade.
     * Tested via RunOnServer because the org first-broker-login flow uses idp-confirm-link
     * (not auto-link), making browser-based existing-user tests impractical.
     */
    @Test
    public void testMembershipTypeNotUpgradedOnReLogin() {
        setupBroker(IDP_ALIAS);
        OrganizationRepresentation orgA = createOrg("org-a", "acme.com");
        linkIdpToOrg(orgA.getId(), IDP_ALIAS, true, "MANAGED");

        UserRepresentation userRep = new UserRepresentation();
        userRep.setEmail("invited@acme.com");
        userRep.setUsername("invited@acme.com");
        userRep.setEnabled(true);
        userRep.setEmailVerified(true);
        String userId;
        try (Response response = consumerRealm.admin().users().create(userRep)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            userId = ApiUtil.getCreatedId(response);
        }
        String finalUserId = userId;
        consumerRealm.cleanup().add(r -> {
            try { r.users().get(finalUserId).remove(); } catch (Exception ignored) {}
        });

        try (Response response = consumerRealm.admin().organizations().get(orgA.getId())
                .members().addMember(userId)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }
        assertIsMemberWithType(userId, orgA.getId(), MembershipType.UNMANAGED);

        String orgId = orgA.getId();
        runOnServer.run(session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            RealmModel realm = session.getContext().getRealm();
            org.keycloak.models.OrganizationModel org = provider.getById(orgId);
            UserModel user = session.users().getUserById(realm, finalUserId);

            boolean added = provider.addManagedMember(org, user);
            assertFalse(added, "addManagedMember should return false for existing member");
            assertFalse(provider.isManagedMember(org, user), "Member should still be UNMANAGED");
        });
    }

    // ==================== Group 3: IdP-Initiated (via kc_idp_hint) ====================

    /**
     * Scenario 3.1: IdP-initiated login with a single org linked to the IdP.
     * configureOrganization step 3 (single-org fallback) resolves the org.
     */
    @Test
    public void testIdpInitiatedSingleOrgFallback() {
        setupBroker(IDP_ALIAS);
        createProviderUser("alice", "alice@acme.com", "password");
        OrganizationRepresentation orgA = createOrg("org-a", "acme.com");
        linkIdpToOrg(orgA.getId(), IDP_ALIAS, true, "UNMANAGED");

        String userId = loginViaBrokerWithIdpHint(IDP_ALIAS, "alice", "password", "alice@acme.com");

        assertIsMemberWithType(userId, orgA.getId(), MembershipType.UNMANAGED);
    }

    /**
     * Scenario 3.2: IdP-initiated login with shared IdP. Email domain disambiguates.
     * configureOrganization step 2 resolves via email domain; domain gate blocks cross-org.
     */
    @Test
    public void testIdpInitiatedSharedIdpDomainDisambiguates() {
        setupBroker(IDP_ALIAS);
        createProviderUser("alice", "alice@acme.com", "password");
        OrganizationRepresentation orgA = createOrg("org-a", "acme.com");
        OrganizationRepresentation orgB = createOrg("org-b", "globex.com");

        linkIdpToOrg(orgA.getId(), IDP_ALIAS, true, "MANAGED");
        linkIdpToOrg(orgB.getId(), IDP_ALIAS, true, "UNMANAGED");

        String userId = loginViaBrokerWithIdpHint(IDP_ALIAS, "alice", "password", "alice@acme.com");

        assertIsMemberWithType(userId, orgA.getId(), MembershipType.MANAGED);
        assertIsNotMember(userId, orgB.getId());
    }

    /**
     * Scenario 3.4: IdP-initiated login where user's email domain is not owned by any org.
     * configureOrganization finds no match → no context.
     * Authenticator iterates all orgs, domain gate blocks all → no membership.
     */
    @Test
    public void testIdpInitiatedUnownedDomainNoMembership() {
        setupBroker(IDP_ALIAS);
        createProviderUser("charlie", "charlie@unknown.com", "password");
        OrganizationRepresentation orgA = createOrg("org-a", "acme.com");
        OrganizationRepresentation orgB = createOrg("org-b", "globex.com");

        linkIdpToOrg(orgA.getId(), IDP_ALIAS, true, "UNMANAGED");
        linkIdpToOrg(orgB.getId(), IDP_ALIAS, true, "UNMANAGED");

        String userId = loginViaBrokerWithIdpHint(IDP_ALIAS, "charlie", "password", "charlie@unknown.com");

        assertIsNotMember(userId, orgA.getId());
        assertIsNotMember(userId, orgB.getId());
    }

    // ==================== Group 7: Edge Cases ====================

    /**
     * Scenario 7.1: Same IdP linked to two orgs with different membership types.
     * Org-A: domain match, MT=MANAGED. Org-B: domain-less (gate passes), MT=UNMANAGED.
     */
    @Test
    public void testDifferentMembershipTypesAcrossOrgs() {
        setupBroker(IDP_ALIAS);
        createProviderUser("alice", "alice@acme.com", "password");
        OrganizationRepresentation orgA = createOrg("org-a", "acme.com");
        OrganizationRepresentation orgB = createOrg("org-b");

        linkIdpToOrg(orgA.getId(), IDP_ALIAS, true, "MANAGED");
        linkIdpToOrg(orgB.getId(), IDP_ALIAS, true, "UNMANAGED");
        setDomainRouting(orgA.getId(), "acme.com", IDP_ALIAS);

        String userId = loginViaBrokerAndGetUserId("alice@acme.com", "alice", "password");

        assertIsMemberWithType(userId, orgA.getId(), MembershipType.MANAGED);
        assertIsMemberWithType(userId, orgB.getId(), MembershipType.UNMANAGED);
    }

    /**
     * Scenario 7.6: Domain-less org with AM=true auto-adds any user regardless of email domain.
     * The domain gate is skipped when an org has no domains.
     */
    @Test
    public void testDomainlessOrgGateSkipped() {
        setupBroker(IDP_ALIAS);
        createProviderUser("alice", "alice@acme.com", "password");
        OrganizationRepresentation orgRouting = createOrg("org-routing", "acme.com");
        OrganizationRepresentation orgDomainless = createOrg("org-domainless");

        linkIdpToOrg(orgRouting.getId(), IDP_ALIAS, true, "UNMANAGED");
        linkIdpToOrg(orgDomainless.getId(), IDP_ALIAS, true, "UNMANAGED");
        setDomainRouting(orgRouting.getId(), "acme.com", IDP_ALIAS);

        String userId = loginViaBrokerAndGetUserId("alice@acme.com", "alice", "password");

        assertIsMemberWithType(userId, orgRouting.getId(), MembershipType.UNMANAGED);
        assertIsMemberWithType(userId, orgDomainless.getId(), MembershipType.UNMANAGED);
    }

    // ==================== Infrastructure helpers ====================

    private void setupBroker(String alias) {
        IdentityProviderRepresentation idp = AbstractOrganizationTest.createRealOrgBroker(alias, providerRealm);
        consumerRealm.admin().identityProviders().create(idp).close();
        consumerRealm.cleanup().add(r -> {
            try {
                r.identityProviders().get(alias).remove();
            } catch (Exception ignored) {}
        });
    }

    private void createProviderUser(String username, String email, String password) {
        UserRepresentation user = UserBuilder.create()
                .username(username)
                .password(password)
                .email(email)
                .emailVerified(true)
                .name(username.substring(0, 1).toUpperCase() + username.substring(1), "User")
                .enabled(true)
                .build();
        String userId;
        try (Response response = providerRealm.admin().users().create(user)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            userId = ApiUtil.getCreatedId(response);
        }
        String finalUserId = userId;
        providerRealm.cleanup().add(r -> {
            try { r.users().get(finalUserId).remove(); } catch (Exception ignored) {}
        });
    }

    private OrganizationRepresentation createOrg(String name, String... domains) {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setName(name);
        org.setAlias(name);
        for (String domain : domains) {
            OrganizationDomainRepresentation d = new OrganizationDomainRepresentation();
            d.setName(domain);
            org.addDomain(d);
        }
        String orgId;
        try (Response response = consumerRealm.admin().organizations().create(org)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            orgId = ApiUtil.getCreatedId(response);
        }
        String finalOrgId = orgId;
        consumerRealm.cleanup().add(r -> {
            try {
                r.organizations().get(finalOrgId).delete().close();
            } catch (Exception ignored) {}
        });
        return consumerRealm.admin().organizations().get(orgId).toRepresentation();
    }

    private void linkIdpToOrg(String orgId, String idpAlias, boolean autoMembership, String membershipType) {
        consumerRealm.admin().organizations().get(orgId)
                .identityProviders().addIdentityProvider(idpAlias).close();

        OrganizationIdentityProviderLinkRepresentation link = new OrganizationIdentityProviderLinkRepresentation();
        link.setAutoMembership(autoMembership);
        link.setMembershipType(membershipType);
        try (Response response = consumerRealm.admin().organizations().get(orgId)
                .identityProviders().get(idpAlias).update(link)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
    }

    private void setDomainRouting(String orgId, String domain, String idpAlias) {
        OrganizationRepresentation org = consumerRealm.admin().organizations().get(orgId).toRepresentation();
        org.getDomains().stream()
                .filter(d -> d.getName().equals(domain))
                .findFirst()
                .ifPresent(d -> {
                    d.setIdentityProviderAlias(idpAlias);
                    d.setAutoRedirect(true);
                });
        consumerRealm.admin().organizations().get(orgId).update(org).close();
    }

    private String loginViaBrokerAndGetUserId(String email, String username, String password) {
        oAuth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(email);
        loginUsernamePage.submit();

        assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains("/realms/" + providerRealm.getName() + "/"),
                "Should be on provider realm login page");

        loginPage.fillLogin(username, password);
        loginPage.submit();

        List<UserRepresentation> users = consumerRealm.admin().users().searchByEmail(email, true);
        assertEquals(1, users.size(), "Federated user should be created in consumer realm");

        String userId = users.get(0).getId();
        consumerRealm.cleanup().add(r -> {
            try {
                r.users().get(userId).remove();
            } catch (Exception ignored) {}
        });
        return userId;
    }

    private String loginViaBrokerWithIdpHint(String idpAlias, String username, String password, String email) {
        oAuth.openLoginForm();
        driver.driver().navigate().to(driver.getCurrentUrl() + "&kc_idp_hint=" + idpAlias);

        assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains("/realms/" + providerRealm.getName() + "/"),
                "Should be on provider realm login page via kc_idp_hint");

        loginPage.fillLogin(username, password);
        loginPage.submit();

        List<UserRepresentation> users = consumerRealm.admin().users().searchByEmail(email, true);
        assertEquals(1, users.size(), "Federated user should be created in consumer realm");

        String userId = users.get(0).getId();
        consumerRealm.cleanup().add(r -> {
            try {
                r.users().get(userId).remove();
            } catch (Exception ignored) {}
        });
        return userId;
    }

    private void logoutConsumerUser(String userId) {
        consumerRealm.admin().users().get(userId).logout();
        List<UserRepresentation> providerUsers = providerRealm.admin().users().search(
                consumerRealm.admin().users().get(userId).toRepresentation().getUsername());
        if (!providerUsers.isEmpty()) {
            providerRealm.admin().users().get(providerUsers.get(0).getId()).logout();
        }
    }

    private void assertIsMemberWithType(String userId, String orgId, MembershipType expectedType) {
        MemberRepresentation member = consumerRealm.admin().organizations().get(orgId)
                .members().member(userId).toRepresentation();
        assertNotNull(member, "User should be a member of org " + orgId);
        assertEquals(expectedType, member.getMembershipType(),
                "Membership type mismatch for org " + orgId);
    }

    private void assertIsNotMember(String userId, String orgId) {
        try {
            consumerRealm.admin().organizations().get(orgId)
                    .members().member(userId).toRepresentation();
            fail("User should not be a member of org " + orgId);
        } catch (NotFoundException expected) {
        }
    }
}
