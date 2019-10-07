package org.keycloak.testsuite.broker;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.keycloak.testsuite.util.VaultUtils;

/**
 * @author Martin Kanis <mkanis@redhat.com>
 */
public class KcOidcBrokerVaultTest extends KcOidcBrokerTest {

    @ArquillianResource
    protected ContainerController controller;

    @Before
    public void beforeKcOidcBrokerVaultTest() throws Exception {
        VaultUtils.enableVault(suiteContext, controller);
        reconnectAdminClient();
        super.beforeBrokerTest();
    }

    @Override
    public void beforeBrokerTest() {}

    @After
    public void afterLDAPVaultTest() throws Exception {
        VaultUtils.disableVault(suiteContext, controller);
        reconnectAdminClient();
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerVaultConfiguration.INSTANCE;
    }
}
