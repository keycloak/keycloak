package org.keycloak.testsuite.federation.ldap;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestConfiguration;
import org.keycloak.testsuite.util.VaultUtils;

import java.util.Map;

import static org.keycloak.models.LDAPConstants.BIND_CREDENTIAL;

/**
 * @author mhajas
 */
public class LDAPVaultCredentialsTest extends LDAPSyncTest {

    private static final String VAULT_EXPRESSION = "${vault.ldap_bindCredential}";

    @ArquillianResource
    protected ContainerController controller;

    @Override
    @Before
    public void beforeAbstractKeycloakTest() throws Exception {
        VaultUtils.enableVault(suiteContext, controller);
        reconnectAdminClient();

        super.beforeAbstractKeycloakTest();
    }

    @After
    public void afterLDAPVaultTest() throws Exception {
        VaultUtils.disableVault(suiteContext, controller);

        reconnectAdminClient();
    }

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule() {
        @Override
        public Map<String, String> getConfig() {

            Map<String, String> config = super.getConfig();
            // Replace secret with vault expression
            config.put(BIND_CREDENTIAL, VAULT_EXPRESSION);
            return config;
        }
    }.assumeTrue(LDAPTestConfiguration::isStartEmbeddedLdapServer);

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }
}
