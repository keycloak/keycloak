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

    public static final String LDAP_CONNECTION_PROPERTIES_LOCATION = "ldap/ldap-connection.properties";

    protected LDAPConfiguration ldapConfiguration;
    protected LDAPEmbeddedServer ldapEmbeddedServer;

    @Override
    protected void before() throws Throwable {
        String connectionPropsLocation = getConnectionPropertiesLocation();
        ldapConfiguration = LDAPConfiguration.readConfiguration(connectionPropsLocation);

        if (ldapConfiguration.isStartEmbeddedLdapLerver()) {
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
                ldapConfiguration = null;
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
        return ldapConfiguration.getLDAPConfig();
    }
}
