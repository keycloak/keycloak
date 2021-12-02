package org.keycloak.testsuite.federation.ldap;

import org.junit.ClassRule;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableVault;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestConfiguration;

import java.util.Map;

import static org.keycloak.models.LDAPConstants.BIND_CREDENTIAL;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

/**
 * @author mhajas
 */
@EnableVault
@AuthServerContainerExclude(value = REMOTE, details = "java.io.NotSerializableException: com.sun.jndi.ldap.LdapCtx")
public class LDAPVaultCredentialsTest extends LDAPSyncTest {

    private static final String VAULT_EXPRESSION = "${vault.ldap_bindCredential}";

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
