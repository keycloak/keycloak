package org.keycloak.tests.broker;

import java.util.List;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.broker.BrokerRunOnServerUtil.removeBrokerExpiredSessions;
import static org.keycloak.tests.broker.BrokerTestTools.waitForPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = org.keycloak.tests.broker.BrokerServerConfig.class)
public class KcOidcBrokerWithConsentTest extends AbstractInitializedBaseBrokerTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectTimeOffSet(enableForCaches = true)
    TimeOffSet timeOffSet;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

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
        Assertions.assertEquals(1, clients.size());
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
     * Referes to in old testsuite: org.keycloak.tests.broker.OIDCKeycloakServerBrokerWithConsentTest#testConsentDeniedWithExpiredClientSession
     */
    @Test
    public void testConsentDeniedWithExpiredClientSession() {
        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        // Set time offset
        timeOffSet.set(60);
        try {
            // User rejected consent
            grantPage.assertCurrent();
            grantPage.cancel();

            // Assert login page with "You took too long to login..." message
            Assertions.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());

        } finally {
            timeOffSet.set(0);
        }
    }

    /**
     * Referes to in old testsuite: org.keycloak.tests.broker.OIDCKeycloakServerBrokerWithConsentTest#testConsentDeniedWithExpiredAndClearedClientSession
     */
    @Test
    public void testConsentDeniedWithExpiredAndClearedClientSession() {
        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        logInWithBroker(bc);

        // Set time offset
        timeOffSet.set(60);
        try {
            testingClient.server(bc.providerRealmName()).run(removeBrokerExpiredSessions());

            // User rejected consent
            grantPage.assertCurrent();
            grantPage.cancel();

            // Assert login page with "You took too long to login..." message
            Assertions.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());
        } finally {
            timeOffSet.set(0);
        }
    }

    /**
     * Referes to in old testsuite: org.keycloak.tests.broker.OIDCKeycloakServerBrokerWithConsentTest#testLoginCancelConsent
     */
    @Test
    public void testLoginCancelConsent() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        logInWithBroker(bc);

        // User rejected consent
        grantPage.assertCurrent();
        grantPage.cancel();

        assertEquals("Sign in to " + bc.consumerRealmName(), driver.getTitle());
    }
}
