package org.keycloak.testsuite.broker;

import java.util.List;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.removeBrokerExpiredSessions;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.junit.Assert.assertEquals;

public class KcOidcBrokerWithConsentTest extends AbstractInitializedBaseBrokerTest {

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

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
        realmRep.setAccessCodeLifespanLogin(30);
        realmRep.setAccessCodeLifespan(300);
        realmRep.setAccessCodeLifespanUserAction(30);
        realmWithBroker.update(realmRep);
    }

    /**
     * Referes to in old testsuite: org.keycloak.testsuite.broker.OIDCKeycloakServerBrokerWithConsentTest#testConsentDeniedWithExpiredClientSession
     */
    @Test
    public void testConsentDeniedWithExpiredClientSession() {
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

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
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

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
     * Referes to in old testsuite: org.keycloak.testsuite.broker.OIDCKeycloakServerBrokerWithConsentTest#testLoginCancelConsent
     */
    @Test
    public void testLoginCancelConsent() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        // User rejected consent
        grantPage.assertCurrent();
        grantPage.cancel();

        assertEquals("Sign in to " + bc.consumerRealmName(), driver.getTitle());
    }
}
