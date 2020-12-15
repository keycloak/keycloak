package org.keycloak.testsuite.broker;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.removeBrokerExpiredSessions;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

public class KcOidcBrokerWithConsentTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @Override
    public void beforeBrokerTest() {
        super.beforeBrokerTest();
        // Require broker to show consent screen
        RealmResource brokeredRealm = adminClient.realm(bc.providerRealmName());
        List<ClientRepresentation> clients = brokeredRealm.clients().findByClientId("brokerapp");
        org.junit.Assert.assertEquals(1, clients.size());
        ClientRepresentation brokerApp = clients.get(0);
        brokerApp.setConsentRequired(true);
        brokeredRealm.clients().get(brokerApp.getId()).update(brokerApp);


        // Change timeouts on realm-with-broker to lower values
        RealmResource realmWithBroker = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation realmRep = realmWithBroker.toRepresentation();
        realmRep.setAccessCodeLifespanLogin(30);;
        realmRep.setAccessCodeLifespan(30);
        realmRep.setAccessCodeLifespanUserAction(30);
        realmWithBroker.update(realmRep);
    }

    /**
     * Referes to in old testsuite: org.keycloak.testsuite.broker.OIDCKeycloakServerBrokerWithConsentTest#testConsentDeniedWithExpiredClientSession
     */
    @Test
    public void testConsentDeniedWithExpiredClientSession() {
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        // Set time offset
        invokeTimeOffset(60);
        try {
            // User rejected consent
            grantPage.assertCurrent();
            grantPage.cancel();

            // Assert login page with "You took too long to login..." message
            org.junit.Assert.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());

        } finally {
            invokeTimeOffset(0);
        }
    }

    /**
     * Referes to in old testsuite: org.keycloak.testsuite.broker.OIDCKeycloakServerBrokerWithConsentTest#testConsentDeniedWithExpiredAndClearedClientSession
     */
    @Test
    public void testConsentDeniedWithExpiredAndClearedClientSession() {
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        // Set time offset
        invokeTimeOffset(60);
        try {

            testingClient.server(bc.providerRealmName()).run(removeBrokerExpiredSessions());

            // User rejected consent
            grantPage.assertCurrent();
            grantPage.cancel();

            // Assert login page with "You took too long to login..." message
            Assert.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());

        } finally {
            invokeTimeOffset(0);
        }
    }

    /**
     * Referes to in old testsuite: org.keycloak.testsuite.broker.OIDCKeycloakServerBrokerWithConsentTest#testAccountManagementLinkingAndExpiredClientSession
     */
    @Test
    public void testAccountManagementLinkingAndExpiredClientSession() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        createUser(bc.consumerRealmName(), "consumer", "password", "FirstName", "LastName", "consumer@localhost.com");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        loginPage.login("consumer", "password");

        accountPage.federatedIdentity();
        accountFederatedIdentityPage.clickAddProvider(bc.getIDPAlias());

        this.loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        // Set time offset
        invokeTimeOffset(60);
        try {
            // User rejected consent
            grantPage.assertCurrent();
            grantPage.cancel();

            // Assert account error page with "staleCodeAccount" error displayed
            accountFederatedIdentityPage.assertCurrent();
            Assert.assertEquals("The page expired. Please try one more time.", accountFederatedIdentityPage.getError());


            // Try to link one more time
            accountFederatedIdentityPage.clickAddProvider(bc.getIDPAlias());

            this.loginPage.login(bc.getUserLogin(), bc.getUserPassword());

            invokeTimeOffset(120);

            // User granted consent
            grantPage.assertCurrent();
            grantPage.accept();

            // Assert account error page with "staleCodeAccount" error displayed
            accountFederatedIdentityPage.assertCurrent();
            Assert.assertEquals("The page expired. Please try one more time.", accountFederatedIdentityPage.getError());

        } finally {
            invokeTimeOffset(0);
        }
    }

    /**
     * Referes to in old testsuite: org.keycloak.testsuite.broker.OIDCKeycloakServerBrokerWithConsentTest#testLoginCancelConsent
     */
    @Test
    public void testLoginCancelConsent() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        // User rejected consent
        grantPage.assertCurrent();
        grantPage.cancel();

        assertEquals("Sign in to " + bc.consumerRealmName(), driver.getTitle());
    }

    /**
     * Referes to in old testsuite: org.keycloak.testsuite.broker.OIDCKeycloakServerBrokerWithConsentTest#testAccountManagementLinkingCancelConsent
     */
    @Test
    public void testAccountManagementLinkingCancelConsent() throws Exception {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        createUser(bc.consumerRealmName(), "consumer", "password", "FirstName", "LastName", "consumer@localhost.com");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        loginPage.login("consumer", "password");

        accountPage.federatedIdentity();

        accountFederatedIdentityPage.clickAddProvider(bc.getIDPAlias());
        this.loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        // User rejected consent
        grantPage.assertCurrent();
        grantPage.cancel();

        // Assert account error page with "consentDenied" error displayed
        accountFederatedIdentityPage.assertCurrent();
        Assert.assertEquals("Consent denied.", accountFederatedIdentityPage.getError());
    }
}
