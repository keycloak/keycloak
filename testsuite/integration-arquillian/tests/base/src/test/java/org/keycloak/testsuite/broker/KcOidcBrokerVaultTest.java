package org.keycloak.testsuite.broker;

/**
 * @author Martin Kanis <mkanis@redhat.com>
 */
public class KcOidcBrokerVaultTest extends KcOidcBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerVaultConfiguration.INSTANCE;
    }
}
