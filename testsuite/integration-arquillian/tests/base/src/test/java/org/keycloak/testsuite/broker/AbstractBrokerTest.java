package org.keycloak.testsuite.broker;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.authentication.authenticators.broker.IdpConfirmLinkAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.pages.ConsentPage;
import org.keycloak.testsuite.util.AccountHelper;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;

import static org.keycloak.models.utils.DefaultAuthenticationFlows.IDP_REVIEW_PROFILE_CONFIG_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        oauth.client("broker-app", "password");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

        if (isUsingTransientSessions()) {
            UsersResource consumerUsers = adminClient.realm(bc.consumerRealmName()).users();

            List<UserRepresentation> userCount = consumerUsers.list();
            assertThat("There must be at no users", userCount, empty());
        } else {
            UserRepresentation userRep = AccountHelper.getUserRepresentation(
                adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
            userRep.setFirstName("Firstname");
            userRep.setLastName("Lastname");

            AccountHelper.updateUser(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin(), userRep);

            UsersResource consumerUsers = adminClient.realm(bc.consumerRealmName()).users();

            int userCount = consumerUsers.count();
            Assert.assertTrue("There must be at least one user", userCount > 0);
        }

        boolean isUserFound = getConsumerUserRepresentations()
          .anyMatch(user -> user.getUsername().equals(bc.getUserLogin()) && user.getEmail().equals(bc.getUserEmail()));

        Assert.assertTrue("There must be user " + bc.getUserLogin() + " in realm " + bc.consumerRealmName(),
          isUserFound);
    }

    protected boolean isUsingTransientSessions() {
        return "true".equals(identityProviderResource.toRepresentation().getConfig().getOrDefault(IdentityProviderModel.DO_NOT_STORE_USERS, "false"));
    }

    protected Stream<UserResource> getConsumerUserResources() {
        return getConsumerUserRepresentations()
          .map(UserRepresentation::getId)
          .map(adminClient.realm(bc.consumerRealmName()).users()::get);
    }

    protected UserRepresentation getConsumerUserRepresentation(String userName) {
        Objects.requireNonNull(userName);
        Iterator<UserRepresentation> it = getConsumerUserRepresentations()
          .peek(userRep -> log.debugf("UserRep: %s .. %s", userRep.getId(), userRep.getUsername()))
          .filter(userRep -> userName.equals(userRep.getUsername()))
          .iterator();

        assertTrue("At least one user expected with username " + userName, it.hasNext());
        UserRepresentation res = it.next();
        assertFalse("At most one user expected with username " + userName, it.hasNext());

        return res;
    }

    protected Stream<UserRepresentation> getConsumerUserRepresentations() {
        String consumerClientBrokerAppId = adminClient.realm(bc.consumerRealmName()).clients().findByClientId("broker-app").get(0).getId();
        List<UserSessionRepresentation> brokeredSessions = adminClient.realm(bc.consumerRealmName()).clients().get(consumerClientBrokerAppId).getUserSessions(0, 10);

        final List<UserRepresentation> persistentUsers = adminClient.realm(bc.consumerRealmName()).users().list();
        final Set<String> persistentUsersId = persistentUsers.stream().map(UserRepresentation::getId).collect(Collectors.toSet());
        return Stream.concat(persistentUsers.stream(),
          brokeredSessions.stream()
            .map(userSession -> userSession.getUserId())
            .filter(id -> ! persistentUsersId.contains(id))
            .map(adminClient.realm(bc.consumerRealmName()).users()::get)
            .map(UserResource::toRepresentation)
        );
    }

    @Test
    public void loginWithExistingUser() {
        Integer userCountBefore = adminClient.realm(bc.consumerRealmName()).users().count();

        testLogInAsUserInIDP();

        Integer userCount = adminClient.realm(bc.consumerRealmName()).users().count();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        if (isUsingTransientSessions()) {
            // Assert that there has been no persistent user created
            assertThat(userCount, is(userCountBefore));
        } else {
            logInWithBroker(bc);

            assertThat(driver.getCurrentUrl(), containsString(getConsumerRoot() + "/auth/realms/master/app/"));
            assertEquals(userCount, adminClient.realm(bc.consumerRealmName()).users().count());
        }
    }


    protected void testSingleLogout() {
        log.debug("Testing single log out");

        oauth.realm(bc.consumerRealmName());
        oauth.client("broker-app", "secret");
        oauth.openLoginForm();

        Assert.assertTrue("Should be logged in", driver.getTitle().endsWith("AUTH_RESPONSE"));

        logoutFromConsumerRealm();
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        loginPage.open(bc.consumerRealmName());

        Assert.assertTrue("Should be on " + bc.consumerRealmName() + " realm on login page",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/protocol/openid-connect/"));
    }

    protected void logoutFromConsumerRealm() {
        if (isUsingTransientSessions()) {
            getConsumerUserResources().forEach(UserResource::logout);
        } else {
            AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        }
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

    public static void disableUpdateProfileOnFirstLogin(AuthenticationExecutionInfoRepresentation execution, AuthenticationManagementResource flows) {
        if (execution.getProviderId() != null && execution.getProviderId().equals(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID)) {
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
            flows.updateExecutions(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW, execution);
        } else if (execution.getAlias() != null && execution.getAlias().equals(IDP_REVIEW_PROFILE_CONFIG_ALIAS)) {
            AuthenticatorConfigRepresentation config = flows.getAuthenticatorConfig(execution.getAuthenticationConfig());
            config.getConfig().put("update.profile.on.first.login", IdentityProviderRepresentation.UPFLM_OFF);
            flows.updateAuthenticatorConfig(config.getId(), config);
        }
    }

    static void disableExistingUser(AuthenticationExecutionInfoRepresentation execution, AuthenticationManagementResource flows) {
        if (execution.getProviderId() != null && (execution.getProviderId().equals(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID) || execution.getProviderId().equals(IdpConfirmLinkAuthenticatorFactory.PROVIDER_ID))) {
            execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED.name());
            flows.updateExecutions(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW, execution);
        }
    }
}
