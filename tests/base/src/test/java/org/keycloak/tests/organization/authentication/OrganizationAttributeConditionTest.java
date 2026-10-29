/*
 * Copyright 2026 Andrzej Świętek
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

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalCredentialAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.Constants;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.organization.authentication.authenticators.conditional.ConditionalOrganizationAttributeAuthenticatorFactory;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginConfigTotpPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginTotpPage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.organization.authentication.authenticators.conditional.ConditionalOrganizationAttributeAuthenticatorFactory.CONF_ATTRIBUTE_EXPECTED_VALUE;
import static org.keycloak.organization.authentication.authenticators.conditional.ConditionalOrganizationAttributeAuthenticatorFactory.CONF_ATTRIBUTE_NAME;
import static org.keycloak.organization.authentication.authenticators.conditional.ConditionalOrganizationAttributeAuthenticatorFactory.CONF_NOT;
import static org.keycloak.organization.authentication.authenticators.conditional.ConditionalOrganizationAttributeAuthenticatorFactory.CONF_ORGANIZATION_SOURCE;
import static org.keycloak.organization.authentication.authenticators.conditional.ConditionalOrganizationAttributeAuthenticatorFactory.SOURCE_ANY_MEMBERSHIP;
import static org.keycloak.organization.authentication.authenticators.conditional.ConditionalOrganizationAttributeAuthenticatorFactory.SOURCE_SELECTED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@KeycloakIntegrationTest
public class OrganizationAttributeConditionTest extends AbstractOrganizationTest {

    private static final String FLOW = "org-otp";
    private static final String FORMS = FLOW + " forms";
    private static final String ORGANIZATION_OTP = "Organization OTP";
    private static final String FLAG = "requireOtp";
    private static final String OTP_SECRET = "DJmQfC73VGFhw7D4QJ8A";

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginUsernamePage loginUsernamePage;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginConfigTotpPage loginConfigTotpPage;

    @InjectPage
    LoginTotpPage loginTotpPage;

    @BeforeEach
    public void resetOAuthScope() {
        oauth.scope(null);
    }

    @Test
    public void testMemberOfFlaggedOrganizationIsAskedForOtp() {
        configureOrganizationOtpFlow(conditionConfig(SOURCE_ANY_MEMBERSHIP, false));
        addMember(flaggedOrganization(), "member@flagged.org");

        login("member@flagged.org");

        loginConfigTotpPage.assertCurrent();
    }

    @Test
    public void testDefaultOrganizationOptionChecksEveryMembership() {
        configureOrganizationOtpFlow(Map.of(
                CONF_ATTRIBUTE_NAME, FLAG,
                CONF_ATTRIBUTE_EXPECTED_VALUE, "true"));
        addMember(flaggedOrganization(), "member@flagged.org");

        login("member@flagged.org");

        loginConfigTotpPage.assertCurrent();
    }

    @Test
    public void testMemberOfFlaggedOrganizationWithOtpIsAskedForOtpOnce() {
        configureOrganizationOtpFlow(conditionConfig(SOURCE_ANY_MEMBERSHIP, false));
        String memberId = createUser(user("enrolled@flagged.org").totpSecret(OTP_SECRET));
        addExistingMember(flaggedOrganization(), memberId);

        login("enrolled@flagged.org");
        loginTotpPage.assertCurrent();
        loginTotpPage.login(new TimeBasedOTP().generateTOTP(OTP_SECRET));

        assertLoggedIn();
    }

    @Test
    public void testMemberOfOrganizationWithoutFlagIsNotAskedForOtp() {
        configureOrganizationOtpFlow(conditionConfig(SOURCE_ANY_MEMBERSHIP, false));
        flaggedOrganization();
        addMember(organization("plain"), "member@plain.org");

        login("member@plain.org");

        assertLoggedIn();
    }

    @Test
    public void testNegatedConditionAsksMemberOfOrganizationWithoutFlagForOtp() {
        configureOrganizationOtpFlow(conditionConfig(SOURCE_ANY_MEMBERSHIP, true));
        flaggedOrganization();
        addMember(organization("plain"), "member@plain.org");

        login("member@plain.org");

        loginConfigTotpPage.assertCurrent();
    }

    @Test
    public void testSelectedFlaggedOrganizationAsksForOtp() {
        configureOrganizationOtpFlow(conditionConfig(SOURCE_SELECTED, false));
        OrganizationResource flagged = flaggedOrganization();
        OrganizationResource plain = organization("plain");
        String memberId = addMember(plain, "member@plain.org").getId();
        addExistingMember(flagged, memberId);

        oauth.scope("organization:flagged");
        login("member@plain.org");

        loginConfigTotpPage.assertCurrent();
    }

    @Test
    public void testSelectedOrganizationDecides() {
        configureOrganizationOtpFlow(conditionConfig(SOURCE_SELECTED, false));
        OrganizationResource flagged = flaggedOrganization();
        OrganizationResource plain = organization("plain");
        String memberId = addMember(flagged, "member@flagged.org").getId();
        addExistingMember(plain, memberId);

        oauth.scope("organization:plain");
        login("member@flagged.org");

        assertLoggedIn();
    }

    @Test
    public void testSelectedOrganizationWithoutMembershipDoesNotMatch() {
        configureOrganizationOtpFlow(conditionConfig(SOURCE_SELECTED, false));
        flaggedOrganization();
        // the email domain makes the browser flow record the flagged organization for a user who is not its member
        createUser(user("outsider@flagged.org"));

        login("outsider@flagged.org");

        assertLoggedIn();
    }

    @Test
    public void testNegatedSelectedConditionWithoutMembershipMatches() {
        configureOrganizationOtpFlow(conditionConfig(SOURCE_SELECTED, true));
        flaggedOrganization();
        createUser(user("outsider@flagged.org"));

        login("outsider@flagged.org");

        loginConfigTotpPage.assertCurrent();
    }

    @Test
    public void testMultiValuedAttributeMatchesAnyValue() {
        configureOrganizationOtpFlow(conditionConfig(SOURCE_ANY_MEMBERSHIP, false));
        addMember(organizationWithAttribute("flagged", List.of("false", "true")), "member@flagged.org");

        login("member@flagged.org");

        loginConfigTotpPage.assertCurrent();
    }

    @Test
    public void testMemberOfDisabledFlaggedOrganizationIsNotAskedForOtp() {
        configureOrganizationOtpFlow(conditionConfig(SOURCE_ANY_MEMBERSHIP, false));
        OrganizationResource flagged = flaggedOrganization();
        addMember(flagged, "member@other.org");
        OrganizationRepresentation representation = flagged.toRepresentation();
        representation.setEnabled(false);
        updateOrganization(flagged, representation);

        login("member@other.org");

        assertLoggedIn();
    }

    @Test
    public void testOrganizationsDisabledInRealmDoesNotMatch() {
        configureOrganizationOtpFlow(conditionConfig(SOURCE_ANY_MEMBERSHIP, false));
        addMember(flaggedOrganization(), "member@flagged.org");

        withOrganizationsDisabled(() -> loginWithPassword("member@flagged.org"));
    }

    @Test
    public void testNegatedConditionWithOrganizationsDisabledInRealmDoesNotMatch() {
        configureOrganizationOtpFlow(conditionConfig(SOURCE_ANY_MEMBERSHIP, true));
        addMember(organization("plain"), "member@plain.org");

        withOrganizationsDisabled(() -> loginWithPassword("member@plain.org"));
    }

    @Test
    public void testAttributeValueIsMatchedExactly() {
        configureOrganizationOtpFlow(conditionConfig(SOURCE_ANY_MEMBERSHIP, false));
        addMember(organizationWithAttribute("flagged", List.of("TRUE", " true", "true ")), "member@flagged.org");

        login("member@flagged.org");

        assertLoggedIn();
    }

    @Test
    public void testNegatedConditionWithoutAttributeNameDoesNotMatch() {
        configureOrganizationOtpFlow(Map.of(
                CONF_ATTRIBUTE_NAME, "",
                CONF_ATTRIBUTE_EXPECTED_VALUE, "true",
                CONF_NOT, Boolean.TRUE.toString()));
        addMember(organization("plain"), "member@plain.org");

        login("member@plain.org");

        assertLoggedIn();
    }

    @Test
    public void testNegatedConditionWithoutExpectedValueDoesNotMatch() {
        configureOrganizationOtpFlow(Map.of(
                CONF_ATTRIBUTE_NAME, FLAG,
                CONF_NOT, Boolean.TRUE.toString()));
        addMember(organization("plain"), "member@plain.org");

        login("member@plain.org");

        assertLoggedIn();
    }

    @Test
    public void testUnknownOrganizationOptionDoesNotMatch() {
        configureOrganizationOtpFlow(conditionConfig("Selected", false));
        addMember(flaggedOrganization(), "member@flagged.org");

        login("member@flagged.org");

        assertLoggedIn();
    }

    @Test
    public void testNegatedUnknownOrganizationOptionDoesNotMatch() {
        configureOrganizationOtpFlow(conditionConfig("Selected", true));
        flaggedOrganization();
        addMember(organization("plain"), "member@plain.org");

        login("member@plain.org");

        assertLoggedIn();
    }

    private OrganizationResource flaggedOrganization() {
        return organizationWithAttribute("flagged", List.of("true"));
    }

    private OrganizationResource organizationWithAttribute(String name, List<String> flagValues) {
        OrganizationResource organization = organization(name);
        OrganizationRepresentation representation = organization.toRepresentation();
        representation.setAttributes(Map.of(FLAG, flagValues));
        updateOrganization(organization, representation);
        return organization;
    }

    private OrganizationResource organization(String name) {
        return realm.admin().organizations().get(createOrganization(name).getId());
    }

    private static void addExistingMember(OrganizationResource organization, String userId) {
        expectStatus(organization.members().addMember(userId), "add member " + userId, Status.CREATED);
    }

    private static void updateOrganization(OrganizationResource organization, OrganizationRepresentation representation) {
        expectStatus(organization.update(representation), "update organization " + representation.getName(), Status.NO_CONTENT);
    }

    private static void expectStatus(Response response, String action, Status expected) {
        try (response) {
            ApiUtil.expectStatus(response, action, expected);
        }
    }

    private UserBuilder user(String email) {
        return UserBuilder.create(email)
                .email(email)
                .emailVerified(true)
                .enabled(true)
                .name("Test", "User")
                .password(memberPassword);
    }

    private String createUser(UserBuilder user) {
        try (Response response = realm.admin().users().create(user.build())) {
            String userId = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> removeUserIfPresent(r, userId));
            return userId;
        }
    }

    private static void removeUserIfPresent(RealmResource realm, String userId) {
        try {
            realm.users().get(userId).remove();
        } catch (NotFoundException ignored) {}
    }

    private void login(String email) {
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(email);
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
    }

    private void loginWithPassword(String email) {
        oauth.openLoginForm();
        loginPage.fillLogin(email, memberPassword);
        loginPage.submit();

        assertLoggedIn();
    }

    private void withOrganizationsDisabled(Runnable test) {
        setOrganizationsEnabled(false);
        try {
            test.run();
        } finally {
            // organizations can only be removed by the cleanup while the realm has them enabled
            setOrganizationsEnabled(true);
        }
    }

    private void setOrganizationsEnabled(boolean enabled) {
        RealmRepresentation representation = realm.admin().toRepresentation();
        representation.setOrganizationsEnabled(enabled);
        realm.admin().update(representation);
    }

    private void assertLoggedIn() {
        assertThat(oauth.parseLoginResponse().getCode(), notNullValue());
    }

    private static Map<String, String> conditionConfig(String source, boolean negate) {
        return Map.of(
                CONF_ATTRIBUTE_NAME, FLAG,
                CONF_ATTRIBUTE_EXPECTED_VALUE, "true",
                CONF_ORGANIZATION_SOURCE, source,
                CONF_NOT, Boolean.toString(negate));
    }

    private void configureOrganizationOtpFlow(Map<String, String> conditionConfig) {
        copyBrowserFlow();
        addConditionalSubFlow();
        addOrganizationCondition(conditionConfig);
        addOtpNotConfiguredCondition();
        addOtpForm();
        useAsBrowserFlow(FLOW);
    }

    private void copyBrowserFlow() {
        expectStatus(realm.admin().flows().copy(DefaultAuthenticationFlows.BROWSER_FLOW, Map.of("newName", FLOW)), "copy the browser flow", Status.CREATED);
        realm.cleanup().add(OrganizationAttributeConditionTest::removeCopiedFlow);
    }

    private static void removeCopiedFlow(RealmResource realm) {
        RealmRepresentation representation = realm.toRepresentation();
        representation.setBrowserFlow(DefaultAuthenticationFlows.BROWSER_FLOW);
        realm.update(representation);
        realm.flows().getFlows().stream()
                .filter(flow -> FLOW.equals(flow.getAlias()))
                .findAny()
                .ifPresent(flow -> realm.flows().deleteFlow(flow.getId()));
    }

    private void addConditionalSubFlow() {
        realm.admin().flows().addExecutionFlow(FORMS, Map.of("alias", ORGANIZATION_OTP, "provider", "registration-page-form", "type", "basic-flow"));
        setRequirement(FORMS, ORGANIZATION_OTP, Requirement.CONDITIONAL);
    }

    private void addOrganizationCondition(Map<String, String> conditionConfig) {
        addRequiredExecution(ConditionalOrganizationAttributeAuthenticatorFactory.PROVIDER_ID, "organization-otp-condition", conditionConfig);
    }

    private void addOtpNotConfiguredCondition() {
        addRequiredExecution(ConditionalCredentialAuthenticatorFactory.PROVIDER_ID, "organization-otp-not-used", Map.of(
                ConditionalCredentialAuthenticatorFactory.CONF_CREDENTIALS,
                String.join(Constants.CFG_DELIMITER, OTPCredentialModel.TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS),
                ConditionalCredentialAuthenticatorFactory.CONF_INCLUDED, Boolean.FALSE.toString()));
    }

    private void addOtpForm() {
        realm.admin().flows().addExecution(ORGANIZATION_OTP, Map.of("provider", OTPFormAuthenticatorFactory.PROVIDER_ID));
        setRequirement(ORGANIZATION_OTP, OTPFormAuthenticatorFactory.PROVIDER_ID, Requirement.REQUIRED);
    }

    private void addRequiredExecution(String providerId, String configAlias, Map<String, String> configValues) {
        AuthenticationManagementResource flows = realm.admin().flows();
        flows.addExecution(ORGANIZATION_OTP, Map.of("provider", providerId));
        AuthenticationExecutionInfoRepresentation execution = setRequirement(ORGANIZATION_OTP, providerId, Requirement.REQUIRED);
        AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
        config.setAlias(configAlias);
        config.setConfig(configValues);
        expectStatus(flows.newExecutionConfig(execution.getId(), config), "configure execution " + providerId, Status.CREATED);
    }

    private void useAsBrowserFlow(String alias) {
        RealmRepresentation representation = realm.admin().toRepresentation();
        representation.setBrowserFlow(alias);
        realm.admin().update(representation);
    }

    private AuthenticationExecutionInfoRepresentation setRequirement(String flowAlias, String providerOrAlias, Requirement requirement) {
        AuthenticationManagementResource flows = realm.admin().flows();
        AuthenticationExecutionInfoRepresentation found = flows.getExecutions(flowAlias).stream()
                .filter(execution -> isExecution(execution, providerOrAlias))
                .findAny()
                .orElseThrow();
        found.setRequirement(requirement.name());
        flows.updateExecutions(flowAlias, found);
        return found;
    }

    private static boolean isExecution(AuthenticationExecutionInfoRepresentation execution, String providerOrAlias) {
        return providerOrAlias.equals(execution.getProviderId()) || providerOrAlias.equals(execution.getDisplayName());
    }
}
