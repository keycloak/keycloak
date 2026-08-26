package org.keycloak.tests.broker;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

/**
 * @author Martin Kanis <mkanis@redhat.com>
 */
@KeycloakIntegrationTest(config = org.keycloak.tests.broker.BrokerVaultServerConfig.class)
public class KcOidcBrokerVaultTest extends AbstractBrokerTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerVaultConfiguration.INSTANCE;
    }
}
