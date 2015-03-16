package org.keycloak.testutils.ldap;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.keycloak.util.KerberosSerializationUtils;
import sun.security.jgss.GSSNameImpl;
import sun.security.jgss.krb5.Krb5NameElement;

/**
 * Factory for ApacheDS based LDAP and Kerberos servers
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class EmbeddedServersFactory {

    private static final String DEFAULT_BASE_DN = "dc=keycloak,dc=org";
    private static final String DEFAULT_BIND_HOST = "localhost";
    private static final int DEFAULT_BIND_PORT = 10389;
    private static final String DEFAULT_LDIF_FILE = "ldap/users.ldif";

    private static final String DEFAULT_KERBEROS_LDIF_FILE = "kerberos/users-kerberos.ldif";

    private static final String DEFAULT_KERBEROS_REALM = "KEYCLOAK.ORG";
    private static final int DEFAULT_KDC_PORT = 6088;
    private static final String DEFAULT_KDC_ENCRYPTION_TYPES = "aes128-cts-hmac-sha1-96, des-cbc-md5, des3-cbc-sha1-kd";

    private String baseDN;
    private String bindHost;
    private int bindPort;
    private String ldapSaslPrincipal;
    private String ldifFile;
    private String kerberosRealm;
    private int kdcPort;
    private String kdcEncryptionTypes;


    public static EmbeddedServersFactory readConfiguration() {
        EmbeddedServersFactory factory = new EmbeddedServersFactory();
        factory.readProperties();
        return factory;
    }


    protected void readProperties() {
        this.baseDN = System.getProperty("ldap.baseDN");
        this.bindHost = System.getProperty("ldap.host");
        String bindPort = System.getProperty("ldap.port");
        this.ldifFile = System.getProperty("ldap.ldif");
        this.ldapSaslPrincipal = System.getProperty("ldap.saslPrincipal");

        this.kerberosRealm = System.getProperty("kerberos.realm");
        String kdcPort = System.getProperty("kerberos.port");
        this.kdcEncryptionTypes = System.getProperty("kerberos.encTypes");

        if (baseDN == null || baseDN.isEmpty()) {
            baseDN = DEFAULT_BASE_DN;
        }
        if (bindHost == null || bindHost.isEmpty()) {
            bindHost = DEFAULT_BIND_HOST;
        }
        this.bindPort = (bindPort == null || bindPort.isEmpty()) ? DEFAULT_BIND_PORT : Integer.parseInt(bindPort);
        if (ldifFile == null || ldifFile.isEmpty()) {
            ldifFile = DEFAULT_LDIF_FILE;
        }

        if (kerberosRealm == null || kerberosRealm.isEmpty()) {
            kerberosRealm = DEFAULT_KERBEROS_REALM;
        }
        this.kdcPort = (kdcPort == null || kdcPort.isEmpty()) ? DEFAULT_KDC_PORT : Integer.parseInt(kdcPort);
        if (kdcEncryptionTypes == null || kdcEncryptionTypes.isEmpty()) {
            kdcEncryptionTypes = DEFAULT_KDC_ENCRYPTION_TYPES;
        }
    }


    public LDAPEmbeddedServer createLdapServer() {

        // Override LDIF file with default for embedded LDAP
        if (ldifFile.equals(DEFAULT_KERBEROS_LDIF_FILE)) {
            ldifFile = DEFAULT_LDIF_FILE;
        }

        return new LDAPEmbeddedServer(baseDN, bindHost, bindPort, ldifFile, ldapSaslPrincipal);
    }


    public KerberosEmbeddedServer createKerberosServer() {

        // Override LDIF file with default for embedded Kerberos
        if (ldifFile.equals(DEFAULT_LDIF_FILE)) {
            ldifFile = DEFAULT_KERBEROS_LDIF_FILE;
        }

        // Init ldap sasl principal just when creating kerberos server
        if (ldapSaslPrincipal == null || ldapSaslPrincipal.isEmpty()) {
            try {
                // Same algorithm like sun.security.krb5.PrincipalName constructor
                GSSName gssName = GSSManager.getInstance().createName("ldap@" + bindHost, GSSName.NT_HOSTBASED_SERVICE);
                GSSNameImpl gssName1 = (GSSNameImpl) gssName;
                Krb5NameElement krb5NameElement = (Krb5NameElement) gssName1.getElement(KerberosSerializationUtils.KRB5_OID);
                this.ldapSaslPrincipal = krb5NameElement.getKrb5PrincipalName().toString();
            } catch (GSSException uhe) {
                throw new RuntimeException(uhe);
            }
        }

        return new KerberosEmbeddedServer(baseDN, bindHost, bindPort, ldifFile, ldapSaslPrincipal, kerberosRealm, kdcPort, kdcEncryptionTypes);
    }
}
