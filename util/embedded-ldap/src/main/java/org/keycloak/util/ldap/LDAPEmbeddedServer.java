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

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.keycloak.common.util.FindFile;
import org.keycloak.common.util.StreamUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.JdbmPartitionFactory;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.extended.PwdModifyHandler;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPEmbeddedServer {

    private static final Logger log = Logger.getLogger(LDAPEmbeddedServer.class);
    private static final int PAGE_SIZE = 30;

    public static final String PROPERTY_BASE_DN = "ldap.baseDN";
    public static final String PROPERTY_BIND_HOST = "ldap.host";
    public static final String PROPERTY_BIND_PORT = "ldap.port";
    public static final String PROPERTY_BIND_LDAPS_PORT = "ldaps.port";
    public static final String PROPERTY_LDIF_FILE = "ldap.ldif";
    public static final String PROPERTY_SASL_PRINCIPAL = "ldap.saslPrincipal";
    public static final String PROPERTY_DSF = "ldap.dsf";
    public static final String PROPERTY_ENABLE_ACCESS_CONTROL = "enableAccessControl";
    public static final String PROPERTY_ENABLE_ANONYMOUS_ACCESS = "enableAnonymousAccess";
    public static final String PROPERTY_ENABLE_SSL = "enableSSL";
    public static final String PROPERTY_ENABLE_STARTTLS = "enableStartTLS";
    public static final String PROPERTY_SET_CONFIDENTIALITY_REQUIRED = "setConfidentialityRequired";

    private static final String DEFAULT_BASE_DN = "dc=keycloak,dc=org";
    private static final String DEFAULT_BIND_HOST = "localhost";
    private static final String DEFAULT_BIND_PORT = "10389";
    private static final String DEFAULT_BIND_LDAPS_PORT = "10636";
    private static final String DEFAULT_LDIF_FILE = "classpath:ldap/default-users.ldif";
    private static final String PROPERTY_KEYSTORE_FILE = "keystoreFile";
    private static final String PROPERTY_CERTIFICATE_PASSWORD = "certificatePassword";

    public static final String DSF_INMEMORY = "mem";
    public static final String DSF_FILE = "file";
    public static final String DEFAULT_DSF = DSF_FILE;

    protected Properties defaultProperties;

    protected String baseDN;
    protected String bindHost;
    protected int bindPort;
    protected int bindLdapsPort;
    protected String ldifFile;
    protected String ldapSaslPrincipal;
    protected String directoryServiceFactory;
    protected boolean enableAccessControl = false;
    protected boolean enableAnonymousAccess = false;
    protected boolean enableSSL = false;
    protected boolean enableStartTLS = false;
    protected boolean setConfidentialityRequired = false;
    protected String keystoreFile;
    protected String certPassword;

    protected DirectoryService directoryService;
    protected LdapServer ldapServer;

    public int getBindPort() {
        return bindPort;
    }

    public int getBindLdapsPort() {
        return bindLdapsPort;
    }

    public static void main(String[] args) throws Exception {
        Properties defaultProperties = new Properties();
        defaultProperties.put(PROPERTY_DSF, DSF_FILE);

        execute(args, defaultProperties);
    }

    public static void execute(String[] args, Properties defaultProperties) throws Exception {
        final LDAPEmbeddedServer ldapEmbeddedServer = new LDAPEmbeddedServer(defaultProperties);
        ldapEmbeddedServer.init();
        ldapEmbeddedServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    ldapEmbeddedServer.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
    }

    public LDAPEmbeddedServer(Properties defaultProperties) {
        this.defaultProperties = defaultProperties;

        this.baseDN = readProperty(PROPERTY_BASE_DN, DEFAULT_BASE_DN);
        this.bindHost = readProperty(PROPERTY_BIND_HOST, DEFAULT_BIND_HOST);
        String bindPort = readProperty(PROPERTY_BIND_PORT, DEFAULT_BIND_PORT);
        this.bindPort = Integer.parseInt(bindPort);
        String bindLdapsPort = readProperty(PROPERTY_BIND_LDAPS_PORT, DEFAULT_BIND_LDAPS_PORT);
        this.bindLdapsPort = Integer.parseInt(bindLdapsPort);
        this.ldifFile = readProperty(PROPERTY_LDIF_FILE, DEFAULT_LDIF_FILE);
        this.ldapSaslPrincipal = readProperty(PROPERTY_SASL_PRINCIPAL, null);
        this.directoryServiceFactory = readProperty(PROPERTY_DSF, DEFAULT_DSF);
        this.enableAccessControl = Boolean.valueOf(readProperty(PROPERTY_ENABLE_ACCESS_CONTROL, "false"));
        this.enableAnonymousAccess = Boolean.valueOf(readProperty(PROPERTY_ENABLE_ANONYMOUS_ACCESS, "false"));
        this.enableSSL = Boolean.valueOf(readProperty(PROPERTY_ENABLE_SSL, "false"));
        this.enableStartTLS = Boolean.valueOf(readProperty(PROPERTY_ENABLE_STARTTLS, "false"));
        this.setConfidentialityRequired = Boolean.valueOf(readProperty(PROPERTY_SET_CONFIDENTIALITY_REQUIRED, "false"));
        this.keystoreFile = readProperty(PROPERTY_KEYSTORE_FILE, null);
        this.certPassword = readProperty(PROPERTY_CERTIFICATE_PASSWORD, null);
    }

    protected String readProperty(String propertyName, String defaultValue) {
        String value = System.getProperty(propertyName);

        if (value == null || value.isEmpty()) {
            value = (String) this.defaultProperties.get(propertyName);
        }

        if (value == null || value.isEmpty()) {
            value = defaultValue;
        }

        return value;
    }


    public void init() throws Exception {
        log.info("Creating LDAP Directory Service. Config: baseDN=" + baseDN + ", bindHost=" + bindHost + ", bindPort=" + bindPort +
                ", ldapSaslPrincipal=" + ldapSaslPrincipal + ", directoryServiceFactory=" + directoryServiceFactory + ", ldif=" + ldifFile +
                ", enableSSL=" + enableSSL + ", enableStartTLS: " + enableStartTLS + ", keystoreFile: " + keystoreFile + ", default java keystore type: " + KeyStore.getDefaultType());

        this.directoryService = createDirectoryService();

        log.info("Importing LDIF: " + ldifFile);
        importLdif();

        log.info("Creating LDAP server..");
        this.ldapServer = createLdapServer();
    }


    public void start() throws Exception {
        log.info("Starting LDAP server..");
        ldapServer.start();
        // Verify the server started properly
        if (ldapServer.isStarted() && ldapServer.getDirectoryService().isStarted()) {
            log.info("LDAP server started.");
        } else if(!ldapServer.isStarted()) {
            throw new RuntimeException("Failed to start the LDAP server!");
        } else if (!ldapServer.getDirectoryService().isStarted()) {
            throw new RuntimeException("Failed to start the directory service for the LDAP server!");
        }
    }


    protected DirectoryService createDirectoryService() throws Exception {
        // Parse "keycloak" from "dc=keycloak,dc=org"
        String dcName = baseDN.split(",")[0];
        dcName = dcName.substring(dcName.indexOf("=") + 1);

        if (this.directoryServiceFactory.equals(DSF_INMEMORY)) {
            // this used to be AvlPartitionFactory but it didn't prove stable in testing;
            // sometimes the search returned an OPERATIONS_ERROR, sometimes after retrieving a list of entries
            // and deleting them one by one, an entry was missing before it was deleted and either the search or the deletion failed.
            // This happened in approximately one out of 100 test runs for RoleModelTest.
            // This all happened with ApacheDS 2.0.0.AM26. Once changed to JdbmPartitionFactory, the problems disappeared.
            // https://issues.apache.org/jira/browse/DIRSERVER-2369
            System.setProperty( "apacheds.partition.factory", JdbmPartitionFactoryFast.class.getName());
        } else if (this.directoryServiceFactory.equals(DSF_FILE)) {
            System.setProperty( "apacheds.partition.factory", JdbmPartitionFactory.class.getName());
        } else {
            throw new IllegalStateException("Unknown value of directoryServiceFactory: " + this.directoryServiceFactory);
        }

        DefaultDirectoryServiceFactory dsf = new DefaultDirectoryServiceFactory();
        DirectoryService service = dsf.getDirectoryService();
        service.setAccessControlEnabled(enableAccessControl);
        service.setAllowAnonymousAccess(enableAnonymousAccess);
        service.getChangeLog().setEnabled(false);

        dsf.init(dcName + "DS");

        Partition partition = dsf.getPartitionFactory().createPartition(
            service.getSchemaManager(),
            service.getDnFactory(),
            dcName,
            this.baseDN,
            1000,
            new File(service.getInstanceLayout().getPartitionsDirectory(), dcName));
        partition.initialize();

        partition.setSchemaManager(service.getSchemaManager());

        // Inject the partition into the DirectoryService
        service.addPartition( partition );

        // Last, process the context entry
        String entryLdif =
                "dn: " + baseDN + "\n" +
                        "dc: " + dcName + "\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n";
        importLdifContent(service, entryLdif);


        if (this.directoryServiceFactory.equals(DSF_INMEMORY)) {
            // Find Normalization interceptor in chain and add our range emulated interceptor
            List<Interceptor> interceptors = service.getInterceptors();
            int insertionPosition = -1;
            for (int pos = 0; pos < interceptors.size(); ++pos) {
                Interceptor interceptor = interceptors.get(pos);
                if (interceptor instanceof NormalizationInterceptor) {
                    insertionPosition = pos;
                }
            }
            RangedAttributeInterceptor interceptor = new RangedAttributeInterceptor("member", PAGE_SIZE);
            interceptor.init(service);
            interceptors.add(insertionPosition + 1, interceptor);
            service.setInterceptors(interceptors);
        }

        return service;
    }


    protected LdapServer createLdapServer() {
        LdapServer ldapServer = new LdapServer();

        ldapServer.setServiceName("DefaultLdapServer");
        ldapServer.setSearchBaseDn(this.baseDN);
        // Tolerate plaintext LDAP connections from clients by default
        ldapServer.setConfidentialityRequired(this.setConfidentialityRequired);

        // Read the transports
        Transport ldap = new TcpTransport(this.bindHost, this.bindPort, 3, 50);
        ldapServer.addTransports( ldap );
        if (enableSSL || enableStartTLS) {
            ldapServer.setKeystoreFile(keystoreFile);
            ldapServer.setCertificatePassword(certPassword);
            if (enableSSL) {
                Transport ldaps = new TcpTransport(this.bindHost, this.bindLdapsPort, 3, 50);
                ldaps.setEnableSSL(true);
                ldapServer.addTransports( ldaps );
                if (ldaps.isSSLEnabled()) {
                    log.info("Enabled SSL support on the LDAP server.");
                }
            }
            if (enableStartTLS) {
                try {
                    ldapServer.addExtendedOperationHandler(new StartTlsHandler());
                } catch (Exception e) {
                    throw new IllegalStateException("Cannot add the StartTLS extension handler: ", e);
                }
                for (ExtendedOperationHandler eoh : ldapServer.getExtendedOperationHandlers()) {
                    if (eoh.getOid().equals(StartTlsHandler.EXTENSION_OID)) {
                        log.info("Enabled StartTLS support on the LDAP server.");
                        break;
                    }
                }
            }
        }

        // Require the LDAP server to accept only encrypted connections if confidentiality requested
        if (setConfidentialityRequired) {
            ldapServer.setConfidentialityRequired(true);
            if (ldapServer.isConfidentialityRequired()) {
                log.info("Configured the LDAP server to accepts only requests with a secured connection.");
            }
        }

        // Associate the DS to this LdapServer
        ldapServer.setDirectoryService( directoryService );

        // Support for extended password modify as described in https://tools.ietf.org/html/rfc3062
        try {
            ldapServer.addExtendedOperationHandler(new PwdModifyHandler());
        } catch (LdapException le) {
            throw new IllegalStateException("It wasn't possible to add PwdModifyHandler");
        }

        if (enableAccessControl) {
            if (enableAnonymousAccess) {
                throw new IllegalStateException("Illegal to enable both the access control subsystem and the anonymous access at the same time! See: http://directory.apache.org/apacheds/gen-docs/latest/apidocs/src-html/org/apache/directory/server/core/DefaultDirectoryService.html#line.399 for details.");
            } else {
                directoryService.setAccessControlEnabled(true);
                if (directoryService.isAccessControlEnabled()) {
                    log.info("Enabled basic access control checks on the LDAP server.");
                }
            }
        } else {
            if (enableAnonymousAccess) {
                directoryService.setAllowAnonymousAccess(true);
                // Since per ApacheDS JavaDoc: http://directory.apache.org/apacheds/gen-docs/latest/apidocs/src-html/org/apache/directory/server/core/DefaultDirectoryService.html#line.399
                // "if the access control subsystem is enabled then access to some entries may not
                // be allowed even when full anonymous access is enabled", disable the access control
                // subsystem together with enabling anonymous access to prevent this
                directoryService.setAccessControlEnabled(false);
                if (directoryService.isAllowAnonymousAccess() && !directoryService.isAccessControlEnabled()) {
                    log.info("Enabled anonymous access on the LDAP server.");
                }
            }
        }

        return ldapServer;
    }


    private void importLdif() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("hostname", this.bindHost);
        if (this.ldapSaslPrincipal != null) {
            map.put("ldapSaslPrincipal", this.ldapSaslPrincipal);
        }

        // Find LDIF file on filesystem or classpath ( if it's like classpath:ldap/users.ldif )
        InputStream is = FindFile.findFile(ldifFile);

        final String ldifContent = StrSubstitutor.replace(StreamUtil.readString(is), map);
        log.info("Content of LDIF: " + ldifContent);

        importLdifContent(directoryService, ldifContent);
    }

    private static void importLdifContent(DirectoryService directoryService, String ldifContent) throws Exception {
        LdifReader ldifReader = new LdifReader(IOUtils.toInputStream(ldifContent));

        try {
            for (LdifEntry ldifEntry : ldifReader) {
                try {
                    directoryService.getAdminSession().add(new DefaultEntry(directoryService.getSchemaManager(), ldifEntry.getEntry()));
                } catch (LdapEntryAlreadyExistsException ignore) {
                    log.info("Entry " + ldifEntry.getDn() + " already exists. Ignoring.");
                }
            }
        } finally {
            ldifReader.close();
        }
    }


    public void stop() throws Exception {
        stopLdapServer();
        shutdownDirectoryService();
    }


    protected void stopLdapServer() {
        log.info("Stopping LDAP server.");
        ldapServer.stop();
    }


    protected void shutdownDirectoryService() throws Exception {
        log.info("Stopping Directory service.");
        directoryService.shutdown();

        // Delete workfiles just for 'inmemory' implementation used in tests. Normally we want LDAP data to persist
        File instanceDir = directoryService.getInstanceLayout().getInstanceDirectory();
        if (this.directoryServiceFactory.equals(DSF_INMEMORY)) {
            log.infof("Removing Directory service workfiles: %s", instanceDir.getAbsolutePath());
            FileUtils.deleteDirectory(instanceDir);
        } else {
            log.info("Working LDAP directory not deleted. Delete it manually if you want to start with fresh LDAP data. Directory location: " + instanceDir.getAbsolutePath());
        }
    }

}
