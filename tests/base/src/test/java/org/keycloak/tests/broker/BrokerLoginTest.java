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
        OAuthClient oauth = getOAuthClient();

        logInAsUserInIDP();

        updateAccountInformation();

        ManagedRealm consumerRealm = getConsumerRealm();
        UserRepresentation userRep = AccountHelper.getUserRepresentation(
                consumerRealm.admin(), getUserLogin());
        Assertions.assertNotNull(userRep, "There must be user " + getUserLogin() + " in consumer realm");
        userRep.setFirstName("Firstname");
        userRep.setLastName("Lastname");
        AccountHelper.updateUser(consumerRealm.admin(), getUserLogin(), userRep);

        int userCount = consumerRealm.admin().users().count();
        Assertions.assertTrue(userCount > 0, "There must be at least one user");

        boolean isUserFound = consumerRealm.admin().users().list().stream()
                .anyMatch(user -> user.getUsername().equals(getUserLogin()) && user.getEmail().equals(getUserEmail()));
        Assertions.assertTrue(isUserFound,
                "There must be user " + getUserLogin() + " in consumer realm");
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

        oauth.openLoginForm();

        logInWithBroker();

        LoginPage loginPage = getLoginPage();
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        Assertions.assertEquals(userCount, consumerRealm.admin().users().count());
    }
}
