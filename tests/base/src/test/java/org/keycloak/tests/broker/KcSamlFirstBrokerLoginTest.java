package org.keycloak.tests.broker;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest(config = org.keycloak.tests.broker.BrokerServerConfig.class)
public class KcSamlFirstBrokerLoginTest extends AbstractFirstBrokerLoginTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    @Test
    @Override
    public void testUpdateProfileIfNotMissingInformation() {
        // skip this test as this provider do not return name and surname so something is missing always
    }
}
