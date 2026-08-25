package org.keycloak.tests.broker;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public interface BrokerLoginTest extends BrokerConfigSupport {

    @Test
    default void testLogInAsUserInIDP() {
        loginUser();
        testSingleLogout();
    }

    default void loginUser() {
        logInAsUserInIDP();

        updateAccountInformation();

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

    default void testSingleLogout() {
        OAuthClient oauth = getOAuthClient();
        ManagedRealm consumerRealm = getConsumerRealm();
        ManagedRealm providerRealm = getProviderRealm();

        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess(), "Should be logged in");

        AccountHelper.logout(consumerRealm.admin(), getUserLogin());
        AccountHelper.logout(providerRealm.admin(), getUserLogin());

        oauth.openLoginForm();
        LoginPage loginPage = getLoginPage();
        loginPage.assertCurrent();
    }

    @Test
    default void loginWithExistingUser() {
        ManagedRealm consumerRealm = getConsumerRealm();
        OAuthClient oauth = getOAuthClient();

        int userCountBefore = consumerRealm.admin().users().count();

        testLogInAsUserInIDP();

        int userCount = consumerRealm.admin().users().count();
        Assertions.assertEquals(userCountBefore + 1, userCount,
                "First broker login should have created exactly one user in the consumer realm");

        oauth.openLoginForm();

        logInWithBroker();

        LoginPage loginPage = getLoginPage();
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        Assertions.assertEquals(userCount, consumerRealm.admin().users().count());
    }
}
