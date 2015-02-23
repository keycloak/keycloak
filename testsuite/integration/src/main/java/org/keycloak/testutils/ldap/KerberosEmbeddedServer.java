package org.keycloak.testutils.ldap;

import java.io.IOException;
import java.lang.reflect.Field;

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
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosEmbeddedServer extends LDAPEmbeddedServer {

    private static final Logger log = Logger.getLogger(KerberosEmbeddedServer.class);

    private final String kerberosRealm;
    private final int kdcPort;

    private KdcServer kdcServer;


    public static void main(String[] args) throws Exception {
        EmbeddedServersFactory factory = EmbeddedServersFactory.readConfiguration();
        KerberosEmbeddedServer kerberosEmbeddedServer = factory.createKerberosServer();
        kerberosEmbeddedServer.init();
        kerberosEmbeddedServer.start();
    }


    protected KerberosEmbeddedServer(String baseDN, String bindHost, int bindPort, String ldifFile, String kerberosRealm, int kdcPort) {
        super(baseDN, bindHost, bindPort, ldifFile);
        this.kerberosRealm = kerberosRealm;
        this.kdcPort = kdcPort;
    }


    @Override
    public void init() throws Exception {
        super.init();

        log.info("Creating KDC server");
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

        ldapServer.setSaslHost( this.bindHost );
        ldapServer.setSaslPrincipal( "ldap/" + this.bindHost + "@" + this.kerberosRealm);

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
                return;
            }

            @Override
            public void clear() {
                return;
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
