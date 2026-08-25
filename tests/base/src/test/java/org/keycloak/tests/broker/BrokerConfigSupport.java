package org.keycloak.tests.broker;

import java.util.List;
import java.util.Set;

import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

    // The provider user is created without a first/last name so the consumer's first-broker-login
    // review-profile page appears. The new-testsuite provider realm enables the VERIFY_PROFILE required
    // action by default, which would otherwise stop the incomplete user at the provider; disable it so
    // the incomplete profile reaches the consumer (the legacy suite did not enable VERIFY_PROFILE).
    @BeforeEach
    default void relaxProviderProfileVerification() {
        for (RequiredActionProviderRepresentation action : getProviderRealm().admin().flows().getRequiredActions()) {
            if (UserModel.RequiredAction.VERIFY_PROFILE.name().equals(action.getAlias())) {
                action.setEnabled(false);
                getProviderRealm().admin().flows().updateRequiredAction(action.getAlias(), action);
            }
        }
    }

    @BeforeEach
    default void enableUnmanagedAttributes() {
        UserProfileUtil.enableUnmanagedAttributes(getConsumerRealm().admin().users().userProfile());
        UserProfileUtil.enableUnmanagedAttributes(getProviderRealm().admin().users().userProfile());
    }

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

    // Drives the consumer's first-broker-login review-profile page, which must appear the first time a
    // user logs in through a broker because the imported user is missing its first and last name. Fails
    // the test if the page does not show, mirroring the legacy assertCurrent() on the update-profile page.
    default void updateAccountInformation() {
        Assertions.assertTrue(profilePageAppeared(),
                "The first-broker-login review-profile page was expected but did not appear");
        getUpdateProfilePage().update("Firstname", "Lastname");
    }

    // Tolerant variant for flows that intentionally skip the review-profile page (e.g. when
    // update-profile-on-first-login is turned off). Fills the page only if it actually appears.
    default void updateAccountInformationIfPresent() {
        if (profilePageAppeared()) {
            getUpdateProfilePage().update("Firstname", "Lastname");
        }
    }

    private boolean profilePageAppeared() {
        Set<String> profilePageIds = Set.of("login-login-update-profile", "login-idp-review-user-profile");
        ManagedWebDriver driver = getWebDriver();
        try {
            driver.waiting().until(d -> {
                String currentPageId = driver.page().getCurrentPageId();
                if (currentPageId != null && profilePageIds.contains(currentPageId)) {
                    return true;
                }
                return null;
            });
            return true;
        } catch (TimeoutException e) {
            return false;
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
        UserRepresentation userRep = AccountHelper.getUserRepresentation(
                consumerRealm.admin(), getUserLogin());
        Assertions.assertNotNull(userRep, "There must be user " + getUserLogin() + " in consumer realm");
        // The review-profile page filled in these names via the UI; assert they were actually persisted
        // rather than overwriting them here, so a broken UI flow is caught instead of masked.
        Assertions.assertEquals("Firstname", userRep.getFirstName(),
                "First name should have been persisted by the review-profile page");
        Assertions.assertEquals("Lastname", userRep.getLastName(),
                "Last name should have been persisted by the review-profile page");

        int userCount = consumerRealm.admin().users().count();
        Assertions.assertTrue(userCount > 0, "There must be at least one user");

        // userRep was fetched above via an exact username search, so assert its email directly instead of
        // scanning the (paginated) users().list(), which could miss the user beyond the first page.
        Assertions.assertEquals(getUserEmail(), userRep.getEmail(),
                "There must be user " + getUserLogin() + " with the expected email in consumer realm");
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
        // Turning off the review-profile step is not enough: the new-testsuite consumer realm enables the
        // VERIFY_PROFILE required action by default, which would still intercept the incomplete imported user
        // (the provider user has no first/last name on purpose). Disable it so login reaches the callback,
        // mirroring the legacy suite, which did not enable VERIFY_PROFILE at all.
        for (RequiredActionProviderRepresentation action : getConsumerRealm().admin().flows().getRequiredActions()) {
            if (UserModel.RequiredAction.VERIFY_PROFILE.name().equals(action.getAlias())) {
                action.setEnabled(false);
                getConsumerRealm().admin().flows().updateRequiredAction(action.getAlias(), action);
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
