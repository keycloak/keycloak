package org.keycloak.testsuite.broker;

public class KcSamlBrokerTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }
}
