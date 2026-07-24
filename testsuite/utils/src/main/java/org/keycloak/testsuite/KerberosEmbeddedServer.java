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

package org.keycloak.testsuite;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Properties;

import org.keycloak.util.ldap.LDAPEmbeddedServer;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.sasl.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.plain.PlainMechanismHandler;
import org.apache.kerby.kerberos.kerb.identity.backend.BackendConfig;
import org.apache.kerby.kerberos.kerb.server.KdcConfig;
import org.apache.kerby.kerberos.kerb.server.KdcConfigKey;
import org.apache.kerby.kerberos.kerb.server.KdcServer;
import org.jboss.logging.Logger;

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
    }


    @Override
    public void start() throws Exception {
        super.start();

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
        ldapServer.setSaslRealms(new java.util.ArrayList<String>());

        ldapServer.addSaslMechanismHandler(SupportedSaslMechanisms.PLAIN, new PlainMechanismHandler());
        ldapServer.addSaslMechanismHandler(SupportedSaslMechanisms.CRAM_MD5, new CramMd5MechanismHandler());
        ldapServer.addSaslMechanismHandler(SupportedSaslMechanisms.DIGEST_MD5, new DigestMd5MechanismHandler());
        ldapServer.addSaslMechanismHandler(SupportedSaslMechanisms.GSSAPI, new GssapiMechanismHandler());
        ldapServer.addSaslMechanismHandler(SupportedSaslMechanisms.NTLM, new NtlmMechanismHandler());
        ldapServer.addSaslMechanismHandler(SupportedSaslMechanisms.GSS_SPNEGO, new NtlmMechanismHandler());

        return ldapServer;
    }


    // In AM26 the ApacheDS KDC accessed the DirectoryService in-process (kdcServer.setDirectoryService()).
    // In AM27 (Kerby) the KDC connects to LDAP over the network via LdapIdentityBackend.
    protected KdcServer createAndStartKdcServer() throws Exception {
        KdcConfig kdcConfig = new KdcConfig();
        kdcConfig.setString(KdcConfigKey.KDC_SERVICE_NAME, "krbtgt/" + this.kerberosRealm + "@" + this.kerberosRealm);
        kdcConfig.setString(KdcConfigKey.KDC_REALM, this.kerberosRealm);
        kdcConfig.setString(KdcConfigKey.KDC_HOST, this.bindHost);
        kdcConfig.setInt(KdcConfigKey.KDC_UDP_PORT, this.kdcPort);
        kdcConfig.setBoolean(KdcConfigKey.KDC_ALLOW_TCP, true);
        kdcConfig.setInt(KdcConfigKey.KDC_TCP_PORT, this.kdcPort);
        kdcConfig.setBoolean(KdcConfigKey.KDC_ALLOW_UDP, true);
        // Kerby expects seconds (multiplies by 1000 internally in TicketIssuer).
        kdcConfig.setInt(KdcConfigKey.MAXIMUM_TICKET_LIFETIME, 86400);
        kdcConfig.setInt(KdcConfigKey.MAXIMUM_RENEWABLE_LIFETIME, 604800);
        kdcConfig.setBoolean(KdcConfigKey.PA_ENC_TIMESTAMP_REQUIRED, false);
        kdcConfig.setString(KdcConfigKey.ENCRYPTION_TYPES, this.kdcEncryptionTypes);

        // KDC_IDENTITY_BACKEND must be set on BackendConfig (not KdcConfig) — KdcUtil.getBackend()
        // reads it from there; if missing, it silently falls back to MemoryIdentityBackend.
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setString(KdcConfigKey.KDC_IDENTITY_BACKEND,
                "org.apache.kerby.kerberos.kdc.identitybackend.LdapIdentityBackend");
        backendConfig.setString("host", this.bindHost);
        backendConfig.setInt("port", this.bindPort);
        backendConfig.setString("admin_dn", "uid=admin,ou=system");
        backendConfig.setString("admin_pw", "secret");
        backendConfig.setString("base_dn", this.baseDN);

        // Use KdcServer directly instead of SimpleKdcServer: SimpleKdcServer's
        // constructor overrides config with defaults (random port, EXAMPLE.COM realm)
        // and init() calls createBuiltinPrincipals() which conflicts with LDIF entries.
        kdcServer = new KdcServer(kdcConfig, backendConfig);

        File workDir = new File(System.getProperty("java.io.tmpdir"), "keycloak-kdc-" + this.kdcPort);
        workDir.mkdirs();
        kdcServer.setWorkDir(workDir);

        kdcServer.init();
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
        try {
            kdcServer.stop();
        } catch (Exception e) {
            log.error("Error stopping KDC server", e);
        }
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
}
