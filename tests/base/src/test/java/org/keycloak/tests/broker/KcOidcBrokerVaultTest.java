package org.keycloak.tests.broker;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testsuite.arquillian.annotation.EnableVault;

/**
 * @author Martin Kanis <mkanis@redhat.com>
 */
@EnableVault
@KeycloakIntegrationTest
public class KcOidcBrokerVaultTest extends AbstractBrokerTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerVaultConfiguration.INSTANCE;
    }
}
