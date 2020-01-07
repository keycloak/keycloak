package org.keycloak.testsuite.broker;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.pages.ConsentPage;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.keycloak.models.utils.DefaultAuthenticationFlows.IDP_REVIEW_PROFILE_CONFIG_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

/**
 * Contains just few basic tests. This is good class to override if you're testing custom IDP configuration and you need
 * to verify if login with IDP works as expected
 */
public abstract class AbstractBrokerTest extends AbstractInitializedBaseBrokerTest {

    public static final String ROLE_USER = "user";
    public static final String ROLE_MANAGER = "manager";
    public static final String ROLE_FRIENDLY_MANAGER = "friendly-manager";
    public static final String ROLE_USER_DOT_GUIDE = "user.guide";
    public static final String EMPTY_ATTRIBUTE_ROLE = "empty.attribute.role";

    @Page
    ConsentPage consentPage;

    @Test
    public void testLogInAsUserInIDP() {
        loginUser();

        testSingleLogout();
    }

    protected void loginUser() {
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        Assert.assertTrue("We must be on correct realm right now",
          driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

        UsersResource consumerUsers = adminClient.realm(bc.consumerRealmName()).users();

        int userCount = consumerUsers.count();
        Assert.assertTrue("There must be at least one user", userCount > 0);

        List<UserRepresentation> users = consumerUsers.search("", 0, userCount);

        boolean isUserFound = false;
        for (UserRepresentation user : users) {
            if (user.getUsername().equals(bc.getUserLogin()) && user.getEmail().equals(bc.getUserEmail())) {
                isUserFound = true;
                break;
            }
        }

        Assert.assertTrue("There must be user " + bc.getUserLogin() + " in realm " + bc.consumerRealmName(),
          isUserFound);
    }


    @Test
    public void loginWithExistingUser() {
        testLogInAsUserInIDP();

        Integer userCount = adminClient.realm(bc.consumerRealmName()).users().count();

        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));
        logInWithBroker(bc);

        assertEquals(accountPage.buildUri().toASCIIString().replace("master", "consumer") + "/", driver.getCurrentUrl());
        assertEquals(userCount, adminClient.realm(bc.consumerRealmName()).users().count());
    }


    protected void testSingleLogout() {
        log.debug("Testing single log out");

        driver.navigate().to(getAccountUrl(bc.providerRealmName()));

        Assert.assertTrue("Should be logged in the account page", driver.getTitle().endsWith("Account Management"));

        logoutFromRealm(bc.providerRealmName());

        Assert.assertTrue("Should be on " + bc.providerRealmName() + " realm", driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName()));

        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

        Assert.assertTrue("Should be on " + bc.consumerRealmName() + " realm on login page",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/protocol/openid-connect/"));
    }

    protected void createRolesForRealm(String realm) {
        RoleRepresentation managerRole = new RoleRepresentation(ROLE_MANAGER,null, false);
        RoleRepresentation friendlyManagerRole = new RoleRepresentation(ROLE_FRIENDLY_MANAGER,null, false);
        RoleRepresentation userRole = new RoleRepresentation(ROLE_USER,null, false);
        RoleRepresentation userGuideRole = new RoleRepresentation(ROLE_USER_DOT_GUIDE,null, false);
        RoleRepresentation emptyAttributeRole = new RoleRepresentation(EMPTY_ATTRIBUTE_ROLE, null, false);

        adminClient.realm(realm).roles().create(managerRole);
        adminClient.realm(realm).roles().create(friendlyManagerRole);
        adminClient.realm(realm).roles().create(userRole);
        adminClient.realm(realm).roles().create(userGuideRole);
        adminClient.realm(realm).roles().create(emptyAttributeRole);
    }

    static void enableUpdateProfileOnFirstLogin(AuthenticationExecutionInfoRepresentation execution, AuthenticationManagementResource flows) {
        if (execution.getProviderId() != null && execution.getProviderId().equals(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID)) {
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
            flows.updateExecutions(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW, execution);
        } else if (execution.getAlias() != null && execution.getAlias().equals(IDP_REVIEW_PROFILE_CONFIG_ALIAS)) {
            AuthenticatorConfigRepresentation config = flows.getAuthenticatorConfig(execution.getAuthenticationConfig());
            config.getConfig().put("update.profile.on.first.login", IdentityProviderRepresentation.UPFLM_ON);
            flows.updateAuthenticatorConfig(config.getId(), config);
        }
    }

    static void setUpMissingUpdateProfileOnFirstLogin(AuthenticationExecutionInfoRepresentation execution, AuthenticationManagementResource flows) {
        if (execution.getProviderId() != null && execution.getProviderId().equals(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID)) {
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
            flows.updateExecutions(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW, execution);
        } else if (execution.getAlias() != null && execution.getAlias().equals(IDP_REVIEW_PROFILE_CONFIG_ALIAS)) {
            AuthenticatorConfigRepresentation config = flows.getAuthenticatorConfig(execution.getAuthenticationConfig());
            config.getConfig().put("update.profile.on.first.login", IdentityProviderRepresentation.UPFLM_MISSING);
            flows.updateAuthenticatorConfig(config.getId(), config);
        }
    }

    static void enableRequirePassword(AuthenticationExecutionInfoRepresentation execution,
            AuthenticationManagementResource flows) {
        String id = execution.getAuthenticationConfig();

        if (id != null) {
            AuthenticatorConfigRepresentation authenticatorConfig = flows.getAuthenticatorConfig(id);

            if (authenticatorConfig != null) {
                Map<String, String> config = authenticatorConfig.getConfig();

                if (config != null && config.containsKey(IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION)) {
                    config.put(IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, Boolean.TRUE.toString());
                }

                flows.updateAuthenticatorConfig(authenticatorConfig.getId(), authenticatorConfig);
            }
        }
    }

    static void disableUpdateProfileOnFirstLogin(AuthenticationExecutionInfoRepresentation execution, AuthenticationManagementResource flows) {
        if (execution.getProviderId() != null && execution.getProviderId().equals(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID)) {
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
            flows.updateExecutions(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW, execution);
        } else if (execution.getAlias() != null && execution.getAlias().equals(IDP_REVIEW_PROFILE_CONFIG_ALIAS)) {
            AuthenticatorConfigRepresentation config = flows.getAuthenticatorConfig(execution.getAuthenticationConfig());
            config.getConfig().put("update.profile.on.first.login", IdentityProviderRepresentation.UPFLM_OFF);
            flows.updateAuthenticatorConfig(config.getId(), config);
        }
    }
}
