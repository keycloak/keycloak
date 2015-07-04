package org.keycloak.testsuite.rule;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.jboss.logging.Logger;
import org.keycloak.testsuite.federation.LDAPTestConfiguration;
import org.keycloak.util.ldap.KerberosEmbeddedServer;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

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
    protected LDAPEmbeddedServer createServer() {
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_DSF, LDAPEmbeddedServer.DSF_INMEMORY);
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_LDIF_FILE, "classpath:kerberos/users-kerberos.ldif");

        return new KerberosEmbeddedServer(defaultProperties);
    }
}
