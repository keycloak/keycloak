package org.keycloak.tests.broker;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Protocol-agnostic broker login scenarios. Concrete OIDC/SAML tests get these {@code @Test} methods by
 * extending a protocol-specific subclass that supplies the realms and broker configuration.
 */
public abstract class AbstractBrokerLoginTest extends AbstractBrokerTest {

    @Test
    public void testLogInAsUserInIDP() {
        loginThroughIdpAndLogout();
    }

    // Shared brokered login + single-logout round-trip, used both as its own test and as the setup step for
    // testLoginWithExistingUser(), so neither test invokes the other's @Test method as a subroutine.
    protected void loginThroughIdpAndLogout() {
        loginUser();
        singleLogout();
    }

    protected void loginUser() {
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

    protected void singleLogout() {
        ManagedRealm consumerRealm = getConsumerRealm();
        ManagedRealm providerRealm = getProviderRealm();

        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess(), "Should be logged in");

        AccountHelper.logout(consumerRealm.admin(), getUserLogin());
        AccountHelper.logout(providerRealm.admin(), getUserLogin());

        oauth.openLoginForm();
        loginPage.assertCurrent();
    }

    @Test
    public void testLoginWithExistingUser() {
        ManagedRealm consumerRealm = getConsumerRealm();

        int userCountBefore = consumerRealm.admin().users().count();

        loginThroughIdpAndLogout();

        int userCount = consumerRealm.admin().users().count();
        Assertions.assertEquals(userCountBefore + 1, userCount,
                "First broker login should have created exactly one user in the consumer realm");

        oauth.openLoginForm();

        logInWithBroker();

        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        Assertions.assertEquals(userCount, consumerRealm.admin().users().count());
    }
}
