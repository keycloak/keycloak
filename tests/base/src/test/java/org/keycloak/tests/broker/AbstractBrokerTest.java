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
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.TimeoutException;

/**
 * Protocol-agnostic base for the broker tests. Holds the injected clients/pages that are identical across
 * every broker test and the shared login/first-broker-login helpers. The realms differ per test (each
 * concrete test injects its own provider/consumer realm with a protocol-specific config), so they are left
 * as abstract accessors; everything else is inherited directly as protected fields.
 */
public abstract class AbstractBrokerTest {

    protected static final String PROVIDER_REALM = "provider";
    protected static final String CONSUMER_REALM = "consumer";
    protected static final String USER_LOGIN = "testuser";
    protected static final String USER_EMAIL = "user@localhost.com";
    protected static final String USER_PASSWORD = "password";
    protected static final String CONSUMER_BROKER_APP_CLIENT_ID = "broker-app";
    protected static final String CONSUMER_BROKER_APP_SECRET = "broker-app-secret";

    // Drive the login flows against the migrated broker-app client (as the legacy suite did) rather than the
    // framework default test-app; the supplier creates it in the consumer realm from BrokerAppClientConfig.
    @InjectOAuthClient(realmRef = "consumer", config = BrokerAppClientConfig.class)
    protected OAuthClient oauth;

    @InjectWebDriver
    protected ManagedWebDriver webDriver;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected IdpReviewUserProfilePage updateProfilePage;

    protected abstract ManagedRealm getProviderRealm();

    protected abstract ManagedRealm getConsumerRealm();

    protected abstract String getIdpAlias();

    // The provider user is created without a first/last name so the consumer's first-broker-login
    // review-profile page appears. The new-testsuite provider realm enables the VERIFY_PROFILE required
    // action by default, which would otherwise stop the incomplete user at the provider; disable it so
    // the incomplete profile reaches the consumer (the legacy suite did not enable VERIFY_PROFILE).
    @BeforeEach
    void relaxProviderProfileVerification() {
        for (RequiredActionProviderRepresentation action : getProviderRealm().admin().flows().getRequiredActions()) {
            if (UserModel.RequiredAction.VERIFY_PROFILE.name().equals(action.getAlias())) {
                action.setEnabled(false);
                getProviderRealm().admin().flows().updateRequiredAction(action.getAlias(), action);
            }
        }
    }

    // Tests that map custom claims onto user attributes (e.g. IdP mappers writing a non-standard
    // attribute) need the declarative user profile to accept attributes outside its managed schema,
    // otherwise they're silently dropped when the user is created or later updated (e.g. by the
    // first-broker-login review-profile form).
    @BeforeEach
    void enableUnmanagedAttributes() {
        UserProfileUtil.enableUnmanagedAttributes(getConsumerRealm().admin().users().userProfile());
        UserProfileUtil.enableUnmanagedAttributes(getProviderRealm().admin().users().userProfile());
    }

    protected String getUserLogin() {
        return USER_LOGIN;
    }

    protected String getUserPassword() {
        return USER_PASSWORD;
    }

    protected String getUserEmail() {
        return USER_EMAIL;
    }

    protected void logInWithBroker() {
        loginPage.clickSocial(getIdpAlias());
    }

    protected void logInWithIdp() {
        logInWithBroker();
    }

    protected void logInAsUserInIDP() {
        oauth.openLoginForm();
        logInWithBroker();
        logInAsUserInIDPForFirstTime();
    }

    protected void logInAsUserInIDPForFirstTime() {
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();
    }

    // Drives the consumer's first-broker-login review-profile page, which must appear the first time a
    // user logs in through a broker because the imported user is missing its first and last name. Fails
    // the test if the page does not show, mirroring the legacy assertCurrent() on the update-profile page.
    protected void updateAccountInformation() {
        Assertions.assertTrue(profilePageAppeared(),
                "The first-broker-login review-profile page was expected but did not appear");
        updateProfilePage.update("Firstname", "Lastname");
    }

    // Tolerant variant for flows that intentionally skip the review-profile page (e.g. when
    // update-profile-on-first-login is turned off). Fills the page only if it actually appears.
    protected void updateAccountInformationIfPresent() {
        if (profilePageAppeared()) {
            updateProfilePage.update("Firstname", "Lastname");
        }
    }

    private boolean profilePageAppeared() {
        Set<String> profilePageIds = Set.of("login-login-update-profile", "login-idp-review-user-profile");
        try {
            webDriver.waiting().until(d -> {
                String currentPageId = webDriver.page().getCurrentPageId();
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

    protected UserRepresentation createUser(String username, String email) {
        ManagedRealm consumerRealm = getConsumerRealm();
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setEmailVerified(true);
        user.setEnabled(true);
        consumerRealm.admin().users().create(user).close();
        return user;
    }

    // Mid-test helper used by singleLogout() to verify login state after logout.
    protected void logoutFromConsumerRealm() {
        AccountHelper.logout(getConsumerRealm().admin(), getUserLogin());
    }

    protected void assertNumFederatedIdentities(String username, int expected) {
        ManagedRealm consumerRealm = getConsumerRealm();
        List<UserRepresentation> users = consumerRealm.admin().users().search(username, true);
        Assertions.assertEquals(1, users.size(), "Expected exactly one user with username " + username);
        List<FederatedIdentityRepresentation> fedIdentities =
                consumerRealm.admin().users().get(users.get(0).getId()).getFederatedIdentity();
        Assertions.assertEquals(expected, fedIdentities.size());
    }
}
