package org.keycloak.testsuite.sessionlimits;

import org.keycloak.testsuite.broker.BrokerConfiguration;
import org.keycloak.testsuite.broker.KcSamlBrokerConfiguration;

public class KcSamlUserSessionLimitsBrokerTest extends AbstractUserSessionLimitsBrokerTest {
    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }
}
