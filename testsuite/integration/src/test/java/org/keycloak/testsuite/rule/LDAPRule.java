package org.keycloak.testsuite.rule;

import java.util.Map;

import org.junit.rules.ExternalResource;
import org.keycloak.testsuite.ldap.EmbeddedServersFactory;
import org.keycloak.testsuite.ldap.LDAPTestConfiguration;
import org.keycloak.testsuite.ldap.LDAPEmbeddedServer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPRule extends ExternalResource {

    public static final String LDAP_CONNECTION_PROPERTIES_LOCATION = "ldap/ldap-connection.properties";

    protected LDAPTestConfiguration ldapTestConfiguration;
    protected LDAPEmbeddedServer ldapEmbeddedServer;

    @Override
    protected void before() throws Throwable {
        String connectionPropsLocation = getConnectionPropertiesLocation();
        ldapTestConfiguration = LDAPTestConfiguration.readConfiguration(connectionPropsLocation);

        if (ldapTestConfiguration.isStartEmbeddedLdapLerver()) {
            EmbeddedServersFactory factory = EmbeddedServersFactory.readConfiguration();
            ldapEmbeddedServer = createServer(factory);
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
                ldapTestConfiguration = null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error tearDown Embedded LDAP server.", e);
        }
    }

    protected String getConnectionPropertiesLocation() {
        return LDAP_CONNECTION_PROPERTIES_LOCATION;
    }

    protected LDAPEmbeddedServer createServer(EmbeddedServersFactory factory) {
        return factory.createLdapServer();
    }

    public Map<String, String> getConfig() {
        return ldapTestConfiguration.getLDAPConfig();
    }
}
