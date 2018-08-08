/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.util.ldap;

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
import org.jboss.logging.Logger;

import javax.security.auth.kerberos.KerberosPrincipal;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosEmbeddedServer extends LDAPEmbeddedServer {

    private static final Logger log = Logger.getLogger(KerberosEmbeddedServer.class);

    public static final String PROPERTY_KERBEROS_REALM = "kerberos.realm";
    public static final String PROPERTY_KDC_PORT = "kerberos.port";
    public static final String PROPERTY_KDC_ENCTYPES = "kerberos.encTypes";

    private static final String DEFAULT_KERBEROS_LDIF_FILE = "classpath:kerberos/default-users.ldif";

    public static final String DEFAULT_KERBEROS_REALM = "KEYCLOAK.ORG";
    public static final String DEFAULT_KERBEROS_REALM_2 = "KC2.COM";

    private static final String DEFAULT_KDC_PORT = "6088";
    private static final String DEFAULT_KDC_ENCRYPTION_TYPES = "aes128-cts-hmac-sha1-96, des-cbc-md5, des3-cbc-sha1-kd";

    private final String kerberosRealm;
    private final int kdcPort;
    private final String kdcEncryptionTypes;

    private KdcServer kdcServer;


    public static void main(String[] args) throws Exception {
        Properties defaultProperties = new Properties();
        defaultProperties.put(PROPERTY_DSF, DSF_FILE);

        String kerberosRealm = System.getProperty("keycloak.kerberos.realm", DEFAULT_KERBEROS_REALM);
        configureDefaultPropertiesForRealm(kerberosRealm, defaultProperties);

        execute(args, defaultProperties);
    }


    public static void configureDefaultPropertiesForRealm(String kerberosRealm, Properties properties) {
        log.infof("Using kerberos realm: %s", kerberosRealm);
        if (DEFAULT_KERBEROS_REALM.equals(kerberosRealm)) {
            // No more configs
        } else if (DEFAULT_KERBEROS_REALM_2.equals(kerberosRealm)) {
            properties.put(PROPERTY_BASE_DN, "dc=kc2,dc=com");
            properties.put(PROPERTY_BIND_PORT, "11389");
            properties.put(PROPERTY_BIND_LDAPS_PORT, "11636");
            properties.put(PROPERTY_LDIF_FILE, "classpath:kerberos/default-users-kc2.ldif");
            properties.put(PROPERTY_KERBEROS_REALM, DEFAULT_KERBEROS_REALM_2);
            properties.put(PROPERTY_KDC_PORT, "7088");
        } else {
            throw new IllegalArgumentException("Valid values for kerberos realm are [ " + DEFAULT_KERBEROS_REALM + " , "
                    + DEFAULT_KERBEROS_REALM_2 + " ]");
        }
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
            String hostname = getHostnameForSASLPrincipal(bindHost);
            this.ldapSaslPrincipal = "ldap/" + hostname + "@" + this.kerberosRealm;
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


    // Forked from sun.security.krb5.PrincipalName constructor
    private String getHostnameForSASLPrincipal(String hostName) {
        try {
            // RFC4120 does not recommend canonicalizing a hostname.
            // However, for compatibility reason, we will try
            // canonicalize it and see if the output looks better.

            String canonicalized = (InetAddress.getByName(hostName)).
                    getCanonicalHostName();

            // Looks if canonicalized is a longer format of hostName,
            // we accept cases like
            //     bunny -> bunny.rabbit.hole
            if (canonicalized.toLowerCase(Locale.ENGLISH).startsWith(
                    hostName.toLowerCase(Locale.ENGLISH)+".")) {
                hostName = canonicalized;
            }
        } catch (UnknownHostException | SecurityException e) {
            // not canonicalized or no permission to do so, use old
        }
        return hostName.toLowerCase(Locale.ENGLISH);
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
