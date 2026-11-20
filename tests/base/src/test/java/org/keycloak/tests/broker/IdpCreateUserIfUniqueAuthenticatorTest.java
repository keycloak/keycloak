package org.keycloak.tests.broker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.authenticators.broker.IdpAutoLinkAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpReviewProfileAuthenticatorFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.UsernameTemplateMapper;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.services.validation.Validation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.AuthenticationExecutionBuilder;
import org.keycloak.testframework.realm.AuthenticationFlowBuilder;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.userprofile.UserProfileConstants;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticator}.
 */
@KeycloakIntegrationTest
public class IdpCreateUserIfUniqueAuthenticatorTest {

    private static final String IDP_ALIAS = "create-user-idp";
    private static final String CLIENT_ID = "broker-client";
    private static final String CLIENT_SECRET = "broker-secret";
    private static final String PROVIDER_USERNAME = "provider-user";

    @InjectRealm(ref = "provider", config = ProviderRealmConfig.class, lifecycle = LifeCycle.CLASS)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", config = ConsumerRealmConfig.class, lifecycle = LifeCycle.CLASS)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectRunOnServer(realmRef = "consumer")
    RunOnServerClient runOnServer;

    @InjectPage
    IdpReviewUserProfilePage idpReviewPage;

    @Test // #50903
    public void testIdpBrokerLoginRejectsUsernameLongerThanMaxLength() {
        // Use a mapper to inject username that is too long, don't persist length in the db.
        String oversizedUsername = "a".repeat(Validation.MAX_USERNAME_LENGTH + 1);
        addUsernameTemplateMapper("oversized-username-mapper", oversizedUsername);

        createProviderUser();
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);

        assertTrue(driver.getCurrentUrl().contains("/realms/" + providerRealm.getName() + "/"),
                "Expected redirect to provider realm login page");

        loginPage.fillLogin(PROVIDER_USERNAME, "password");
        loginPage.submit();
        assertEquals("login-idp-review-user-profile", driver.page().getCurrentPageId(), "Expected broker flow to restart at IDP review step after resetFlow(), but page was: "
                + driver.page().getCurrentPageId());
        consumerRealm.admin().users().search(oversizedUsername).forEach(u ->
                consumerRealm.cleanup().add(r -> r.users().get(u.getId()).remove()));
        List<UserRepresentation> created = consumerRealm.admin().users().search(oversizedUsername);
        assertTrue(created.isEmpty(), "No user with an oversized username should be created in the consumer realm");
    }

    @Test // #50903
    public void testIdpBrokerLoginAcceptsUsernameAtMaxLength() {
        String boundaryUsername = "a".repeat(Validation.MAX_USERNAME_LENGTH);
        addUsernameTemplateMapper("boundary-username-mapper", boundaryUsername);

        createProviderUser();
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);

        assertTrue(driver.getCurrentUrl().contains("/realms/" + providerRealm.getName() + "/"),
                "Expected redirect to provider realm login page");

        loginPage.fillLogin(PROVIDER_USERNAME, "password");
        loginPage.submit();

        assertNotEquals("login-idp-review-user-profile", driver.page().getCurrentPageId(), "Boundary-length username must not trigger the length guard resetFlow()");

        List<UserRepresentation> created = consumerRealm.admin().users().search(boundaryUsername, true);
        assertFalse(created.isEmpty(),
                "A user with a boundary-length username must be created in the consumer realm");
        created.forEach(u -> consumerRealm.cleanup().add(r -> r.users().get(u.getId()).remove()));
    }

    @Test // #50505
    public void testIdpBrokerLoginSetsDefaultValueForReadOnlyAttribute() {
        UPConfig originalConfig = consumerRealm.admin().users().userProfile().getConfiguration();
        UPConfig config = consumerRealm.admin().users().userProfile().getConfiguration();
        UPAttribute adminOnly = new UPAttribute("adminOnly", new UPAttributePermissions(Set.of(UserProfileConstants.ROLE_ADMIN), Set.of(UserProfileConstants.ROLE_ADMIN)));
        adminOnly.setDefaultValue("def");
        config.addOrReplaceAttribute(adminOnly);
        consumerRealm.admin().users().userProfile().update(config);
        consumerRealm.cleanup().add(r -> r.users().userProfile().update(originalConfig));

        createProviderUser();
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(PROVIDER_USERNAME, "password");
        loginPage.submit();

        List<UserRepresentation> created = consumerRealm.admin().users().search(PROVIDER_USERNAME, true);
        Assertions.assertEquals(1, created.size());
        String userId = created.get(0).getId();
        consumerRealm.cleanup().add(r -> r.users().get(userId).remove());

        // the admin API shows default values even if not stored, so check the stored value
        String stored = runOnServer.fetchString(session -> String.valueOf(
                session.users().getUserById(session.getContext().getRealm(), userId).getAttributes().get("adminOnly")));
        Assertions.assertEquals("[def]", stored);
    }

    @Test // 1131
    public void testAutoLinkBlockedWhenEmailNotVerifiedAndTrustEmailDisabled() {
        String flowAlias = "AutoLinkFlow-blocked";
        useAutoLinkFlow(flowAlias, createAutoLinkFlow(flowAlias), false);

        String victimId = createConsumerUser("victim-cve", "victim-cve@example.org");
        createProviderUser("attacker-cve", "victim-cve@example.org", false);

        loginViaBroker("attacker-cve");

        assertNotLinked(victimId);
        consumerRealm.admin().users().search("attacker-cve", true).forEach(
                u -> consumerRealm.admin().users().get(u.getId()).remove());
    }

    @Test // 1131 — happy path
    public void testAutoLinkSucceedsWhenEmailVerifiedAndTrustEmailEnabled() {
        String flowAlias = "AutoLinkFlow-trusted";
        useAutoLinkFlow(flowAlias, createAutoLinkFlow(flowAlias), true);

        String existingUserId = createConsumerUser("trusted-user", "trusted-user@example.org");
        createProviderUser("trusted-provider-user", "trusted-user@example.org", true);

        loginViaBroker("trusted-provider-user");

        assertLinked(existingUserId);
    }

    @Test // 1131
    public void testAutoLinkSucceedsWhenTrustEmailEnabledAndEmailNotVerified() {
        String flowAlias = "AutoLinkFlow-trust-unverified";
        useAutoLinkFlow(flowAlias, createAutoLinkFlow(flowAlias), true);

        String existingUserId = createConsumerUser("trust-unverified-user", "trust-unverified@example.org");
        createProviderUser("trust-unverified-provider", "trust-unverified@example.org", false);

        loginViaBroker("trust-unverified-provider");

        assertLinked(existingUserId);
    }

    @Test // 1131
    public void testAutoLinkBlockedWhenEmailChangedOnReviewProfilePage() {
        assertAutoLinkBlockedWhenEmailChangedOnReviewProfilePage(false, true);
    }

    @Test // 1131
    public void testAutoLinkBlockedWhenEmailChangedOnReviewProfilePageAndEmailNotVerified() {
        assertAutoLinkBlockedWhenEmailChangedOnReviewProfilePage(false, false);
    }

    @Test // 1131
    public void testAutoLinkBlockedWhenEmailChangedOnReviewProfilePageAndTrustEmailEnabled() {
        assertAutoLinkBlockedWhenEmailChangedOnReviewProfilePage(true, true);
    }

    @Test // 1131
    public void testAutoLinkBlockedWhenEmailChangedOnReviewProfilePageAndTrustEmailEnabledAndEmailNotVerified() {
        assertAutoLinkBlockedWhenEmailChangedOnReviewProfilePage(true, false);
    }

    private void assertAutoLinkBlockedWhenEmailChangedOnReviewProfilePage(boolean trustEmail, boolean emailVerified) {
        disableProviderVerifyProfile();

        String flowAlias = "AutoLinkFlow-review-blocked";
        useAutoLinkFlow(flowAlias, createAutoLinkFlowWithReviewProfile(flowAlias), trustEmail);

        String victimId = createConsumerUser("victim-review", "victim-review@example.org");
        // Missing firstName triggers the review profile page
        createProviderUser("attacker-review", "attacker-review@example.org", emailVerified, false);

        loginViaBroker("attacker-review");

        // Attacker changes email to victim's on the review page
        idpReviewPage.assertCurrent();
        idpReviewPage.update("Attacker", "User", "victim-review@example.org");

        assertNotLinked(victimId);
        consumerRealm.admin().users().search("attacker-review", true).forEach(
                u -> consumerRealm.admin().users().get(u.getId()).remove());
    }

    @Test // 1131
    public void testAutoLinkBlockedWhenUsernameChangedOnReviewProfilePage() {
        disableProviderVerifyProfile();

        String flowAlias = "AutoLinkFlow-review-username-blocked";
        useAutoLinkFlow(flowAlias, createAutoLinkFlowWithReviewProfile(flowAlias), true);

        String victimId = createConsumerUser("victim-username-review", "victim-username-review@example.org");
        // Missing firstName triggers the review profile page
        createProviderUser("attacker-username-review", "attacker-username-review@example.org", true, false);

        // Enable username editing on the consumer realm
        var realmRep = consumerRealm.admin().toRepresentation();
        realmRep.setEditUsernameAllowed(true);
        consumerRealm.admin().update(realmRep);
        consumerRealm.cleanup().add(r -> {
            var rep = r.toRepresentation();
            rep.setEditUsernameAllowed(false);
            r.update(rep);
        });

        loginViaBroker("attacker-username-review");

        // Attacker changes username to victim's on the review page
        idpReviewPage.assertCurrent();
        idpReviewPage.prepareUpdate().firstName("Attacker").lastName("User").username("victim-username-review").submit();

        assertNotLinked(victimId);
        consumerRealm.admin().users().search("attacker-username-review", true).forEach(
                u -> consumerRealm.admin().users().get(u.getId()).remove());
        consumerRealm.admin().users().search("victim-username-review", true).stream()
                .filter(u -> !u.getId().equals(victimId))
                .forEach(u -> consumerRealm.admin().users().get(u.getId()).remove());
    }

    @Test // 1131
    public void testAutoLinkSucceedsWhenEmailNotChangedOnReviewProfilePage() {
        assertAutoLinkWhenEmailNotChangedOnReviewProfilePage(false, true, true);
    }

    @Test // 1131
    public void testAutoLinkBlockedWhenEmailNotChangedOnReviewProfilePageAndEmailNotVerifiedAndTrustEmailDisabled() {
        assertAutoLinkWhenEmailNotChangedOnReviewProfilePage(false, false, false);
    }

    @Test // 1131
    public void testAutoLinkSucceedsWhenEmailNotChangedOnReviewProfilePageAndTrustEmailEnabled() {
        assertAutoLinkWhenEmailNotChangedOnReviewProfilePage(true, true, true);
    }

    @Test // 1131
    public void testAutoLinkSucceedsWhenEmailNotChangedOnReviewProfilePageAndTrustEmailEnabledAndEmailNotVerified() {
        assertAutoLinkWhenEmailNotChangedOnReviewProfilePage(true, false, true);
    }

    private void assertAutoLinkWhenEmailNotChangedOnReviewProfilePage(boolean trustEmail, boolean emailVerified, boolean links) {
        disableProviderVerifyProfile();

        String flowAlias = "AutoLinkFlow-review-success";
        useAutoLinkFlow(flowAlias, createAutoLinkFlowWithReviewProfile(flowAlias), trustEmail);

        String sharedEmail = "review-same@example.org";
        String existingUserId = createConsumerUser("review-same-user", sharedEmail);
        // Missing firstName triggers the review profile page
        createProviderUser("review-same-provider", sharedEmail, emailVerified, false);

        loginViaBroker("review-same-provider");

        // Fill in first/last name only not email
        idpReviewPage.assertCurrent();
        idpReviewPage.update("Test", "User");

        if (links) {
            assertLinked(existingUserId);
        } else {
            assertNotLinked(existingUserId);
        }
    }

    @Test // 1131
    public void testAutoLinkSucceedsWhenMatchedByUsername() {
        String flowAlias = "AutoLinkFlow-username-match";
        useAutoLinkFlow(flowAlias, createAutoLinkFlow(flowAlias), false);

        String existingUserId = createConsumerUser("username-match-user", "username-match@example.org");
        // Same username, different email — match is by username so email verification is irrelevant
        createProviderUser("username-match-user", "different-email@example.org", true);

        loginViaBroker("username-match-user");

        assertLinked(existingUserId);
    }

    @Test // 1131
    public void testAutoLinkSucceedsWhenMatchedByUsernameAndEmailNotVerified() {
        String flowAlias = "AutoLinkFlow-username-match-unverified";
        useAutoLinkFlow(flowAlias, createAutoLinkFlow(flowAlias), false);

        String existingUserId = createConsumerUser("username-unverified-user", "username-unverified@example.org");
        // email_verified=false, trustEmail=false — should still link because the match is by username
        createProviderUser("username-unverified-user", "different-unverified@example.org", false);

        loginViaBroker("username-unverified-user");

        assertLinked(existingUserId);
    }

    @Test // 1131
    public void testAutoLinkSucceedsWhenMatchedByVerifiedEmail() {
        String flowAlias = "AutoLinkFlow-email-match";
        useAutoLinkFlow(flowAlias, createAutoLinkFlow(flowAlias), false);

        String sharedEmail = "email-match@example.org";
        String existingUserId = createConsumerUser("email-match-user", sharedEmail);
        createProviderUser("email-match-provider", sharedEmail, true);

        loginViaBroker("email-match-provider");

        assertLinked(existingUserId);
    }

    @Test // 1131
    public void testAutoLinkBlockedWhenMappedUsernameMatchesAnotherEmail() {
        String flowAlias = "AutoLinkFlow-mapped-email-blocked";
        useAutoLinkFlow(flowAlias, createAutoLinkFlow(flowAlias), false);

        String victimEmail = "victim-mapped@example.org";
        String victimId = createConsumerUser("victim-mapped", victimEmail);
        createProviderUser("attacker-mapped", "attacker-mapped@example.org", true);
        // The duplicate is recorded as EMAIL, but the verified broker email belongs to the attacker.
        addUsernameTemplateMapper("victim-email-username", victimEmail);

        loginViaBroker("attacker-mapped");

        assertNotLinked(victimId);
    }

    @Test // 1131
    public void testAutoLinkBlockedWhenEmailVerifiedClaimIsAbsentAndTrustEmailDisabled() {
        assertAutoLinkWhenEmailVerifiedClaimIsAbsent(false, false);
    }

    @Test // 1131
    public void testAutoLinkSucceedsWhenEmailVerifiedClaimIsAbsentAndTrustEmailEnabled() {
        assertAutoLinkWhenEmailVerifiedClaimIsAbsent(true, true);
    }

    private void assertAutoLinkWhenEmailVerifiedClaimIsAbsent(boolean trustEmail, boolean links) {
        String flowAlias = "AutoLinkFlow-missing-claim";
        useAutoLinkFlow(flowAlias, createAutoLinkFlow(flowAlias), trustEmail);

        // Remove verification from both the ID token and UserInfo while retaining the email claim.
        var emailScope = providerRealm.admin().clientScopes().findAll().stream()
                .filter(scope -> "email".equals(scope.getName())).findFirst().orElseThrow();
        var mappers = providerRealm.admin().clientScopes().get(emailScope.getId()).getProtocolMappers();
        var verifiedMapper = mappers.getMappers().stream()
                .filter(mapper -> "email_verified".equals(mapper.getConfig().get("claim.name")))
                .findFirst().orElseThrow();
        providerRealm.cleanup().add(r -> {
            try (Response response = r.clientScopes().get(emailScope.getId()).getProtocolMappers()
                    .createMapper(verifiedMapper)) {
                assertEquals(201, response.getStatus());
            }
        });
        mappers.delete(verifiedMapper.getId());

        String sharedEmail = "missing-claim@example.org";
        String existingUserId = createConsumerUser("missing-claim-user", sharedEmail);
        createProviderUser("missing-claim-provider", sharedEmail, true);

        loginViaBroker("missing-claim-provider");

        if (links) {
            assertLinked(existingUserId);
        } else {
            assertNotLinked(existingUserId);
        }
    }

    @Test // 1131
    public void testBlockedAutoLinkContinuesToHandleExistingAccountWhenEmailNotVerified() {
        assertBlockedAutoLinkContinuesToHandleExistingAccount(false);
    }

    @Test // 1131
    public void testBlockedAutoLinkContinuesToHandleExistingAccountWhenEmailChangedAndTrustEmailEnabled() {
        assertBlockedAutoLinkContinuesToHandleExistingAccount(true);
    }

    private void assertBlockedAutoLinkContinuesToHandleExistingAccount(boolean emailEdited) {
        String flowAlias = "AutoLinkFlow-fallback";
        try (Response response = consumerRealm.admin().flows()
                .copy("first broker login", Map.of("newName", flowAlias))) {
            assertEquals(201, response.getStatus());
        }
        String flowId = consumerRealm.admin().flows().getFlows().stream()
                .filter(flow -> flowAlias.equals(flow.getAlias())).findFirst().orElseThrow().getId();
        useAutoLinkFlow(flowAlias, flowId, emailEdited);
        var linkingFlow = consumerRealm.admin().flows().getExecutions(flowAlias).stream()
                .filter(exec -> exec.getDisplayName().contains("User creation or linking"))
                .findFirst().orElseThrow();
        consumerRealm.admin().flows().addExecution(AuthenticationExecutionBuilder.create()
                .parentFlow(linkingFlow.getFlowId())
                .requirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString())
                .authenticator(IdpAutoLinkAuthenticatorFactory.PROVIDER_ID).priority(15)
                .authenticatorFlow(false).build());

        String victimEmail = "victim-fallback@example.org";
        String victimId = createConsumerUser("victim-fallback", victimEmail);
        if (emailEdited) {
            disableProviderVerifyProfile();
            createProviderUser("attacker-fallback", "attacker-fallback@example.org", true, false);
        } else {
            createProviderUser("attacker-fallback", victimEmail, false);
        }

        loginViaBroker("attacker-fallback");
        if (emailEdited) {
            idpReviewPage.assertCurrent();
            idpReviewPage.update("Attacker", "User", victimEmail);
        }

        assertEquals("login-login-idp-link-confirm", driver.page().getCurrentPageId(),
                "Rejected auto-link must continue to Handle Existing Account");
        assertTrue(consumerRealm.admin().users().get(victimId).getFederatedIdentity().isEmpty(),
                "Account must remain unlinked while ownership confirmation is pending");
        assertTrue(consumerRealm.admin().users().get(victimId).getUserSessions().isEmpty(),
                "No victim session may be authenticated before ownership confirmation");
    }

    /**
     * Creates a basic auto-link authentication flow containing idp-create-user-if-unique
     * (ALTERNATIVE) followed by idp-auto-link (ALTERNATIVE). Returns the new flow's ID.
     */
    private String createAutoLinkFlow(String alias) {
        AuthenticationFlowRepresentation flow = AuthenticationFlowBuilder.create()
                .alias(alias).description(alias)
                .providerId("basic-flow").topLevel(true).builtIn(false).build();
        consumerRealm.admin().flows().createFlow(flow);
        String flowId = consumerRealm.admin().flows().getFlows().stream()
                .filter(f -> alias.equals(f.getAlias())).findFirst().orElseThrow().getId();
        for (var entry : List.of(
                Map.entry(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, 1),
                Map.entry(IdpAutoLinkAuthenticatorFactory.PROVIDER_ID, 2))) {
            consumerRealm.admin().flows().addExecution(AuthenticationExecutionBuilder.create()
                    .parentFlow(flowId)
                    .requirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString())
                    .authenticator(entry.getKey()).priority(entry.getValue())
                    .authenticatorFlow(false).build());
        }
        return flowId;
    }

    private void addUsernameTemplateMapper(String mapperName, String usernameTemplate) {
        // adds to consumer IDP, replaces brokered username with mapperName
        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName(mapperName);
        mapper.setIdentityProviderAlias(IDP_ALIAS);
        mapper.setIdentityProviderMapper(UsernameTemplateMapper.PROVIDER_ID);
        mapper.setConfig(Map.of(
                UsernameTemplateMapper.TEMPLATE, usernameTemplate,
                "target", "LOCAL"
        ));
        try (Response response = consumerRealm.admin().identityProviders().get(IDP_ALIAS).addMapper(mapper)) {
            Assertions.assertEquals(201, response.getStatus());
            String mapperId = ApiUtil.getCreatedId(response);
            consumerRealm.cleanup().add(r -> r.identityProviders().get(IDP_ALIAS).delete(mapperId));
        }
    }

    private void createProviderUser() {
        UserRepresentation user = UserBuilder.create(PROVIDER_USERNAME)
                .password("password")
                .email(PROVIDER_USERNAME + "@example.com")
                .emailVerified(true)
                .firstName("Provider")
                .lastName("User")
                .build();
        try (Response response = providerRealm.admin().users().create(user)) {
            Assertions.assertEquals(201, response.getStatus());
            String userId = ApiUtil.getCreatedId(response);
            providerRealm.cleanup().add(r -> r.users().get(userId).remove());
        }
    }

    /**
     * Creates an auto-link flow with idp-review-profile (REQUIRED) at the top level,
     * and a REQUIRED subflow containing idp-create-user-if-unique (ALTERNATIVE) and
     * idp-auto-link (ALTERNATIVE).
     */
    private String createAutoLinkFlowWithReviewProfile(String alias) {
        AuthenticationFlowRepresentation flow = AuthenticationFlowBuilder.create()
                .alias(alias).description(alias)
                .providerId("basic-flow").topLevel(true).builtIn(false).build();
        consumerRealm.admin().flows().createFlow(flow);
        String flowId = consumerRealm.admin().flows().getFlows().stream()
                .filter(f -> alias.equals(f.getAlias())).findFirst().orElseThrow().getId();

        // Add idp-review-profile as REQUIRED at the top level
        consumerRealm.admin().flows().addExecution(AuthenticationExecutionBuilder.create()
                .parentFlow(flowId)
                .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString())
                .authenticator(IdpReviewProfileAuthenticatorFactory.PROVIDER_ID).priority(1)
                .authenticatorFlow(false).build());

        // Add a REQUIRED subflow for the ALTERNATIVE auto-link executions
        String subflowAlias = alias + "-auto-link-subflow";
        Map<String, Object> subflowData = new HashMap<>();
        subflowData.put("alias", subflowAlias);
        subflowData.put("type", "basic-flow");
        subflowData.put("description", "Auto-link subflow");
        subflowData.put("provider", "registration-page-form");
        consumerRealm.admin().flows().addExecutionFlow(alias, subflowData);

        // The subflow defaults to DISABLED — set it to REQUIRED and capture its flow ID
        String subflowId = null;
        for (AuthenticationExecutionInfoRepresentation exec :
                consumerRealm.admin().flows().getExecutions(alias)) {
            if (subflowAlias.equals(exec.getDisplayName())) {
                exec.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString());
                consumerRealm.admin().flows().updateExecutions(alias, exec);
                subflowId = exec.getFlowId();
                break;
            }
        }
        Assertions.assertNotNull(subflowId, "Subflow must exist after addExecutionFlow");

        // Add ALTERNATIVE executions inside the subflow
        for (var entry : List.of(
                Map.entry(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, 1),
                Map.entry(IdpAutoLinkAuthenticatorFactory.PROVIDER_ID, 2))) {
            consumerRealm.admin().flows().addExecution(AuthenticationExecutionBuilder.create()
                    .parentFlow(subflowId)
                    .requirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString())
                    .authenticator(entry.getKey()).priority(entry.getValue())
                    .authenticatorFlow(false).build());
        }
        return flowId;
    }

    private void useAutoLinkFlow(String flowAlias, String flowId, boolean trustEmail) {
        IdentityProviderRepresentation idpBefore = consumerRealm.admin()
                .identityProviders().get(IDP_ALIAS).toRepresentation();
        consumerRealm.cleanup().add(r -> {
            IdentityProviderRepresentation restore = r.identityProviders().get(IDP_ALIAS).toRepresentation();
            restore.setFirstBrokerLoginFlowAlias(idpBefore.getFirstBrokerLoginFlowAlias());
            restore.setTrustEmail(idpBefore.isTrustEmail());
            r.identityProviders().get(IDP_ALIAS).update(restore);
        });
        consumerRealm.cleanup().add(r -> r.flows().deleteFlow(flowId));

        IdentityProviderRepresentation idp = consumerRealm.admin()
                .identityProviders().get(IDP_ALIAS).toRepresentation();
        idp.setFirstBrokerLoginFlowAlias(flowAlias);
        idp.setTrustEmail(trustEmail);
        consumerRealm.admin().identityProviders().get(IDP_ALIAS).update(idp);
    }

    private String createConsumerUser(String username, String email) {
        UserRepresentation user = UserBuilder.create(username).password("password")
                .email(email).emailVerified(true).firstName("Test").lastName("User").build();
        try (Response resp = consumerRealm.admin().users().create(user)) {
            Assertions.assertEquals(201, resp.getStatus());
            String userId = ApiUtil.getCreatedId(resp);
            consumerRealm.cleanup().add(r -> r.users().get(userId).remove());
            return userId;
        }
    }

    private void createProviderUser(String username, String email, boolean emailVerified) {
        createProviderUser(username, email, emailVerified, true);
    }

    private void createProviderUser(String username, String email, boolean emailVerified, boolean includeFirstName) {
        UserBuilder builder = UserBuilder.create(username).password("password")
                .email(email).emailVerified(emailVerified).lastName("User");
        if (includeFirstName) {
            builder = builder.firstName("Test");
        }
        try (Response resp = providerRealm.admin().users().create(builder.build())) {
            Assertions.assertEquals(201, resp.getStatus());
            String userId = ApiUtil.getCreatedId(resp);
            providerRealm.cleanup().add(r -> r.users().get(userId).remove());
        }
    }

    private void loginViaBroker(String username) {
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(username, "password");
        loginPage.submit();
    }

    private void assertLinked(String userId) {
        assertTrue(oauth.parseLoginResponse().isSuccess(), "Auto-link must complete authorization");
        List<FederatedIdentityRepresentation> fedIds =
                consumerRealm.admin().users().get(userId).getFederatedIdentity();
        assertFalse(fedIds.isEmpty(), "Account must be linked after auto-link");
        assertTrue(fedIds.stream().anyMatch(f -> IDP_ALIAS.equals(f.getIdentityProvider())),
                "Linked identity must be from the expected IdP alias");
    }

    private void assertNotLinked(String userId) {
        List<FederatedIdentityRepresentation> fedIds =
                consumerRealm.admin().users().get(userId).getFederatedIdentity();
        assertEquals("login-error", driver.page().getCurrentPageId(),
                "An auto-link-only flow must fail instead of issuing an authorization code");
        assertTrue(fedIds.isEmpty(), "Account must not have a federated identity link");
        assertTrue(consumerRealm.admin().users().get(userId).getUserSessions().isEmpty(),
                "Blocked auto-link must not authenticate a session as the existing user");
    }

    private void disableProviderVerifyProfile() {
        for (RequiredActionProviderRepresentation action : providerRealm.admin().flows().getRequiredActions()) {
            if (UserModel.RequiredAction.VERIFY_PROFILE.name().equals(action.getAlias())) {
                if (action.isEnabled()) {
                    action.setEnabled(false);
                    providerRealm.admin().flows().updateRequiredAction(action.getAlias(), action);
                    providerRealm.cleanup().add(r -> {
                        for (RequiredActionProviderRepresentation a : r.flows().getRequiredActions()) {
                            if (UserModel.RequiredAction.VERIFY_PROFILE.name().equals(a.getAlias())) {
                                a.setEnabled(true);
                                r.flows().updateRequiredAction(a.getAlias(), a);
                            }
                        }
                    });
                }
                break;
            }
        }
    }

    public static class ProviderRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.clients(
                    ClientBuilder.create(CLIENT_ID)
                            .secret(CLIENT_SECRET)
                            .redirectUris("*")
                            .directAccessGrantsEnabled()
            );
        }
    }

    public static class ConsumerRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            String providerBase = "http://localhost:8080/realms/provider";

            IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
            idp.setAlias(IDP_ALIAS);
            idp.setProviderId(OIDCIdentityProviderFactory.PROVIDER_ID);
            idp.setEnabled(true);
            idp.setTrustEmail(true);
            idp.setConfig(Map.of(
                    "clientId",          CLIENT_ID,
                    "clientSecret",      CLIENT_SECRET,
                    "authorizationUrl",  providerBase + "/protocol/openid-connect/auth",
                    "tokenUrl",          providerBase + "/protocol/openid-connect/token",
                    "userInfoUrl",       providerBase + "/protocol/openid-connect/userinfo",
                    "jwksUrl",           providerBase + "/protocol/openid-connect/certs",
                    "defaultScope",      "openid email profile",
                    IdentityProviderModel.SYNC_MODE, "IMPORT"
            ));
            return realm.identityProviders(idp);
        }
    }
}
