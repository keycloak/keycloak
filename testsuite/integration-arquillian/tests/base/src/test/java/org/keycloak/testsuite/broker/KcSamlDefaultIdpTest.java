package org.keycloak.testsuite.broker;

import org.junit.Test;

/**
 * Test of various scenarios related to the use of default IdP option
 * in the Identity Provider Redirector authenticator
 */
public class KcSamlDefaultIdpTest extends AbstractDefaultIdpTest {

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
