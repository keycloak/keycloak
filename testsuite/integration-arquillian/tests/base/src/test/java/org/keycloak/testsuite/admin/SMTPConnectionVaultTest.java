package org.keycloak.testsuite.admin;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.keycloak.testsuite.util.VaultUtils;

/**
 * @author Martin Kanis <mkanis@redhat.com>
 */
public class SMTPConnectionVaultTest extends SMTPConnectionTest {

    @ArquillianResource
    protected ContainerController controller;

    @Before
    public void beforeSMTPConnectionVaultTest() throws Exception {
        VaultUtils.enableVault(suiteContext, controller);
        reconnectAdminClient();

        super.before();
    }

    @Override
    public void before() {
    }

    @After
    public void afterLDAPVaultTest() throws Exception {
        VaultUtils.disableVault(suiteContext, controller);
        reconnectAdminClient();
    }

    @Override
    public String setSmtpPassword() {
        return "${vault.smtp_password}";
    }
}