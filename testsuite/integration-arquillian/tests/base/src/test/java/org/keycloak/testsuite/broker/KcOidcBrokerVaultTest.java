package org.keycloak.testsuite.broker;

import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableVault;

/**
 * @author Martin Kanis <mkanis@redhat.com>
 */
@EnableVault
@AuthServerContainerExclude({AuthServerContainerExclude.AuthServer.QUARKUS, AuthServerContainerExclude.AuthServer.REMOTE})
public class KcOidcBrokerVaultTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerVaultConfiguration.INSTANCE;
    }
}
