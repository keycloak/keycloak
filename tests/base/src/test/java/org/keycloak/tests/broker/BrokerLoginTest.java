package org.keycloak.tests.broker;

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
        assertUserCreatedInConsumerRealm();
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
