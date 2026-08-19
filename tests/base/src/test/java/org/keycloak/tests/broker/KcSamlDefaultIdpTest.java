package org.keycloak.tests.broker;

import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

/**
 * Test of various scenarios related to the use of default IdP option
 * in the Identity Provider Redirector authenticator
 */
@KeycloakIntegrationTest
public class KcSamlDefaultIdpTest extends AbstractDefaultIdpTest {

    @InjectRealm
    ManagedRealm managedRealm;

    // KEYCLOAK-17368
    @Test
    public void testDefaultIdpSetTriedAndReturnedError() {
        testDefaultIdpSetTriedAndReturnedError("Unexpected error when authenticating with identity provider");
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlBrokerConfiguration();
    }
}
