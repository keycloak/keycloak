package org.keycloak.util.ldap;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.kerberos.KerberosConfig;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.sasl.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.plain.PlainMechanismHandler;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.KerberosUtils;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.jboss.logging.Logger;
import org.keycloak.util.KerberosSerializationUtils;
import sun.security.jgss.GSSNameImpl;
import sun.security.jgss.krb5.Krb5NameElement;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosEmbeddedServer extends LDAPEmbeddedServer {

    private static final Logger log = Logger.getLogger(KerberosEmbeddedServer.class);

    public static final String PROPERTY_KERBEROS_REALM = "kerberos.realm";
    public static final String PROPERTY_KDC_PORT = "kerberos.port";
    public static final String PROPERTY_KDC_ENCTYPES = "kerberos.encTypes";

    private static final String DEFAULT_KERBEROS_LDIF_FILE = "classpath:kerberos/default-users.ldif";

    private static final String DEFAULT_KERBEROS_REALM = "KEYCLOAK.ORG";
    private static final String DEFAULT_KDC_PORT = "6088";
    private static final String DEFAULT_KDC_ENCRYPTION_TYPES = "aes128-cts-hmac-sha1-96, des-cbc-md5, des3-cbc-sha1-kd";

    private final String kerberosRealm;
    private final int kdcPort;
    private final String kdcEncryptionTypes;

    private KdcServer kdcServer;


    public static void main(String[] args) throws Exception {
        Properties defaultProperties = new Properties();
        defaultProperties.put(PROPERTY_DSF, DSF_FILE);

        execute(args, defaultProperties);
    }

    public static void execute(String[] args, Properties defaultProperties) throws Exception {
        final KerberosEmbeddedServer kerberosEmbeddedServer = new KerberosEmbeddedServer(defaultProperties);
        kerberosEmbeddedServer.init();
        kerberosEmbeddedServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    kerberosEmbeddedServer.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
    }


    public KerberosEmbeddedServer(Properties defaultProperties) {
        super(defaultProperties);

        this.ldifFile = readProperty(PROPERTY_LDIF_FILE, DEFAULT_KERBEROS_LDIF_FILE);

        this.kerberosRealm = readProperty(PROPERTY_KERBEROS_REALM, DEFAULT_KERBEROS_REALM);
        String kdcPort = readProperty(PROPERTY_KDC_PORT, DEFAULT_KDC_PORT);
        this.kdcPort = Integer.parseInt(kdcPort);
        this.kdcEncryptionTypes = readProperty(PROPERTY_KDC_ENCTYPES, DEFAULT_KDC_ENCRYPTION_TYPES);

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
    }


    @Override
    public void init() throws Exception {
        super.init();

        log.info("Creating KDC server. kerberosRealm: " + kerberosRealm + ", kdcPort: " + kdcPort + ", kdcEncryptionTypes: " + kdcEncryptionTypes);
        createAndStartKdcServer();
    }


    @Override
    protected DirectoryService createDirectoryService() throws Exception {
        DirectoryService directoryService = super.createDirectoryService();

        directoryService.addLast(new KeyDerivationInterceptor());
        return directoryService;
    }


    @Override
    protected LdapServer createLdapServer() {
        LdapServer ldapServer = super.createLdapServer();

        ldapServer.setSaslHost(this.bindHost);
        ldapServer.setSaslPrincipal( this.ldapSaslPrincipal);
        ldapServer.setSaslRealms(new ArrayList<String>());

        ldapServer.addSaslMechanismHandler(SupportedSaslMechanisms.PLAIN, new PlainMechanismHandler());
        ldapServer.addSaslMechanismHandler(SupportedSaslMechanisms.CRAM_MD5, new CramMd5MechanismHandler());
        ldapServer.addSaslMechanismHandler(SupportedSaslMechanisms.DIGEST_MD5, new DigestMd5MechanismHandler());
        ldapServer.addSaslMechanismHandler(SupportedSaslMechanisms.GSSAPI, new GssapiMechanismHandler());
        ldapServer.addSaslMechanismHandler(SupportedSaslMechanisms.NTLM, new NtlmMechanismHandler());
        ldapServer.addSaslMechanismHandler(SupportedSaslMechanisms.GSS_SPNEGO, new NtlmMechanismHandler());

        return ldapServer;
    }


    protected KdcServer createAndStartKdcServer() throws Exception {
        KerberosConfig kdcConfig = new KerberosConfig();
        kdcConfig.setServicePrincipal("krbtgt/" + this.kerberosRealm + "@" + this.kerberosRealm);
        kdcConfig.setPrimaryRealm(this.kerberosRealm);
        kdcConfig.setMaximumTicketLifetime(60000 * 1440);
        kdcConfig.setMaximumRenewableLifetime(60000 * 10080);
        kdcConfig.setPaEncTimestampRequired(false);
        Set<EncryptionType> encryptionTypes = convertEncryptionTypes();
        kdcConfig.setEncryptionTypes(encryptionTypes);

        kdcServer = new NoReplayKdcServer(kdcConfig);
        kdcServer.setSearchBaseDn(this.baseDN);

        UdpTransport udp = new UdpTransport(this.bindHost, this.kdcPort);
        kdcServer.addTransports(udp);

        kdcServer.setDirectoryService(directoryService);

        // Launch the server
        kdcServer.start();

        return kdcServer;
    }


    public void stop() throws Exception {
        stopLdapServer();
        stopKerberosServer();
        shutdownDirectoryService();
    }


    protected void stopKerberosServer() {
        log.info("Stopping Kerberos server.");
        kdcServer.stop();
    }


    private Set<EncryptionType> convertEncryptionTypes() {
        Set<EncryptionType> encryptionTypes = new HashSet<EncryptionType>();
        String[] configEncTypes = kdcEncryptionTypes.split(",");

        for ( String enc : configEncTypes ) {
            enc = enc.trim();
            for ( EncryptionType type : EncryptionType.getEncryptionTypes() ) {
                if ( type.getName().equalsIgnoreCase( enc ) ) {
                    encryptionTypes.add( type );
                }
            }
        }

        encryptionTypes = KerberosUtils.orderEtypesByStrength(encryptionTypes);
        return encryptionTypes;
    }


    /**
     * Replacement of apacheDS KdcServer class with disabled ticket replay cache.
     *
     * @author Dominik Pospisil <dpospisi@redhat.com>
     */
    class NoReplayKdcServer extends KdcServer {

        NoReplayKdcServer(KerberosConfig kdcConfig) {
            super(kdcConfig);
        }

        /**
         *
         * Dummy implementation of the ApacheDS kerberos replay cache. Essentially disables kerbores ticket replay checks.
         * https://issues.jboss.org/browse/JBPAPP-10974
         *
         * @author Dominik Pospisil <dpospisi@redhat.com>
         */
        private class DummyReplayCache implements ReplayCache {

            @Override
            public boolean isReplay(KerberosPrincipal serverPrincipal, KerberosPrincipal clientPrincipal, KerberosTime clientTime,
                                    int clientMicroSeconds) {
                return false;
            }

            @Override
            public void save(KerberosPrincipal serverPrincipal, KerberosPrincipal clientPrincipal, KerberosTime clientTime,
                             int clientMicroSeconds) {
            }

            @Override
            public void clear() {
            }

        }

        /**
         * @throws java.io.IOException if we cannot bind to the sockets
         */
        @Override
        public void start() throws IOException, LdapInvalidDnException {
            super.start();

            try {

                // override initialized replay cache with a dummy implementation
                Field replayCacheField = KdcServer.class.getDeclaredField("replayCache");
                replayCacheField.setAccessible(true);
                replayCacheField.set(this, new DummyReplayCache());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }
}
