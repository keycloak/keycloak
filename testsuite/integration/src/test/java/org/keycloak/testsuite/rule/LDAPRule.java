package org.keycloak.testsuite.rule;

import java.util.Map;

import org.junit.rules.ExternalResource;
import org.keycloak.testutils.ldap.EmbeddedServersFactory;
import org.keycloak.testutils.ldap.LDAPConfiguration;
import org.keycloak.testutils.ldap.LDAPEmbeddedServer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPRule extends ExternalResource {

    private LDAPConfiguration ldapConfiguration;
    private LDAPEmbeddedServer ldapEmbeddedServer;

    @Override
    protected void before() throws Throwable {
        ldapConfiguration = LDAPConfiguration.readConfiguration();

        if (ldapConfiguration.isStartEmbeddedLdapLerver()) {
            EmbeddedServersFactory factory = EmbeddedServersFactory.readConfiguration();
            ldapEmbeddedServer = factory.createLdapServer();
            ldapEmbeddedServer.init();
            ldapEmbeddedServer.start();
        }
    }

    @Override
    protected void after() {
        try {
            if (ldapEmbeddedServer != null) {
                ldapEmbeddedServer.stop();
                ldapEmbeddedServer = null;
                ldapConfiguration = null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error tearDown Embedded LDAP server.", e);
        }
    }

    public Map<String, String> getLdapConfig() {
        return ldapConfiguration.getLDAPConfig();
    }
}
