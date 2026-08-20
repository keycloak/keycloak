package org.keycloak.tests.broker;

import java.util.List;
import java.util.Set;

import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.TimeoutException;

public interface BrokerConfigSupport {

    String PROVIDER_REALM = "provider";
    String CONSUMER_REALM = "consumer";
    String USER_LOGIN = "testuser";
    String USER_EMAIL = "user@localhost.com";
    String USER_PASSWORD = "password";
    String CONSUMER_BROKER_APP_CLIENT_ID = "broker-app";
    String CONSUMER_BROKER_APP_SECRET = "broker-app-secret";

    ManagedRealm getProviderRealm();
    ManagedRealm getConsumerRealm();
    OAuthClient getOAuthClient();
    ManagedWebDriver getWebDriver();
    LoginPage getLoginPage();
    IdpReviewUserProfilePage getUpdateProfilePage();

    String getIdpAlias();

    default String getUserLogin() {
        return USER_LOGIN;
    }

    default String getUserPassword() {
        return USER_PASSWORD;
    }

    default String getUserEmail() {
        return USER_EMAIL;
    }

    default void logInWithBroker() {
        LoginPage loginPage = getLoginPage();
        loginPage.clickSocial(getIdpAlias());
    }

    default void logInWithIdp() {
        logInWithBroker();
    }

    default void logInAsUserInIDP() {
        OAuthClient oauth = getOAuthClient();
        oauth.openLoginForm();
        logInWithBroker();
        logInAsUserInIDPForFirstTime();
    }

    default void logInAsUserInIDPForFirstTime() {
        LoginPage loginPage = getLoginPage();
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();
    }

    // Handles the consumer's first-broker-login review-profile page, which appears when a
    // user logs in via a broker for the first time. Uses a timeout catch for flows where
    // the page doesn't appear (e.g. when disableUpdateProfileOnFirstLogin() was called).
    default void updateAccountInformation() {
        waitForProfilePageAndUpdate();
    }

    default void waitForProfilePageAndUpdate() {
        Set<String> profilePageIds = Set.of("login-login-update-profile", "login-idp-review-user-profile");
        try {
            getWebDriver().waiting().until(d -> {
                String currentPageId = getWebDriver().page().getCurrentPageId();
                if (currentPageId != null && profilePageIds.contains(currentPageId)) {
                    return true;
                }
                return null;
            });
            getUpdateProfilePage().update("Firstname", "Lastname");
        } catch (TimeoutException e) {
            // page did not appear - this is expected in some flows
        }
    }

    default UserRepresentation createUser(String username, String email) {
        ManagedRealm consumerRealm = getConsumerRealm();
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setEmailVerified(true);
        user.setEnabled(true);
        consumerRealm.admin().users().create(user).close();
        return user;
    }

    // Mid-test helper used by testSingleLogout() to verify login state after logout.
    default void logoutFromConsumerRealm() {
        AccountHelper.logout(getConsumerRealm().admin(), getUserLogin());
    }

    default void assertUserCreatedInConsumerRealm() {
        ManagedRealm consumerRealm = getConsumerRealm();
        int userCount = consumerRealm.admin().users().count();
        Assertions.assertTrue(userCount > 0, "There must be at least one user");
        boolean isUserFound = consumerRealm.admin().users().list().stream()
                .anyMatch(user -> user.getUsername().equals(getUserLogin()) && user.getEmail().equals(getUserEmail()));
        Assertions.assertTrue(isUserFound,
                "There must be user " + getUserLogin() + " in consumer realm");
    }

    default void disableUpdateProfileOnFirstLogin() {
        var flows = getConsumerRealm().admin().flows();
        for (AuthenticationExecutionInfoRepresentation execution :
                flows.getExecutions(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW)) {
            if (IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID.equals(execution.getProviderId())) {
                execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
                flows.updateExecutions(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW, execution);
            } else if (execution.getAlias() != null
                    && execution.getAlias().equals(DefaultAuthenticationFlows.IDP_REVIEW_PROFILE_CONFIG_ALIAS)) {
                AuthenticatorConfigRepresentation config = flows.getAuthenticatorConfig(execution.getAuthenticationConfig());
                config.getConfig().put("update.profile.on.first.login", IdentityProviderRepresentation.UPFLM_OFF);
                flows.updateAuthenticatorConfig(config.getId(), config);
            }
        }
    }

    default void assertNumFederatedIdentities(String username, int expected) {
        ManagedRealm consumerRealm = getConsumerRealm();
        List<UserRepresentation> users = consumerRealm.admin().users().search(username, true);
        Assertions.assertEquals(1, users.size(), "Expected exactly one user with username " + username);
        List<FederatedIdentityRepresentation> fedIdentities =
                consumerRealm.admin().users().get(users.get(0).getId()).getFederatedIdentity();
        Assertions.assertEquals(expected, fedIdentities.size());
    }
}
