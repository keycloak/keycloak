package org.keycloak.testsuite.rule;

import java.io.File;
import java.net.URL;

import org.jboss.logging.Logger;
import org.keycloak.testsuite.ldap.EmbeddedServersFactory;
import org.keycloak.testsuite.ldap.LDAPTestConfiguration;
import org.keycloak.testsuite.ldap.LDAPEmbeddedServer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosRule extends LDAPRule {

    private static final Logger log = Logger.getLogger(KerberosRule.class);

    private final String configLocation;

    public KerberosRule(String configLocation) {
        this.configLocation = configLocation;

        // Global kerberos configuration
        URL krb5ConfURL = LDAPTestConfiguration.class.getResource("/kerberos/test-krb5.conf");
        String krb5ConfPath = new File(krb5ConfURL.getFile()).getAbsolutePath();
        log.info("Krb5.conf file location is: " + krb5ConfPath);
        System.setProperty("java.security.krb5.conf", krb5ConfPath);
    }

    @Override
    protected String getConnectionPropertiesLocation() {
        return configLocation;
    }

    @Override
    protected LDAPEmbeddedServer createServer(EmbeddedServersFactory factory) {
        return factory.createKerberosServer();
    }
}
