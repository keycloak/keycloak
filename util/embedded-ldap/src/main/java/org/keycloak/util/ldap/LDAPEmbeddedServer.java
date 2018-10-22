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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.jboss.logging.Logger;
import org.keycloak.common.util.FindFile;
import org.keycloak.common.util.StreamUtil;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPEmbeddedServer {

    private static final Logger log = Logger.getLogger(LDAPEmbeddedServer.class);

    public static final String PROPERTY_BASE_DN = "ldap.baseDN";
    public static final String PROPERTY_BIND_HOST = "ldap.host";
    public static final String PROPERTY_BIND_PORT = "ldap.port";
    public static final String PROPERTY_BIND_LDAPS_PORT = "ldaps.port";
    public static final String PROPERTY_LDIF_FILE = "ldap.ldif";
    public static final String PROPERTY_SASL_PRINCIPAL = "ldap.saslPrincipal";
    public static final String PROPERTY_DSF = "ldap.dsf";

    private static final String DEFAULT_BASE_DN = "dc=keycloak,dc=org";
    private static final String DEFAULT_BIND_HOST = "localhost";
    private static final String DEFAULT_BIND_PORT = "10389";
    private static final String DEFAULT_BIND_LDAPS_PORT = "10636";
    private static final String DEFAULT_LDIF_FILE = "classpath:ldap/default-users.ldif";
    private static final String PROPERTY_ENABLE_SSL = "enableSSL";
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
    protected boolean enableSSL = false;
    protected String keystoreFile;
    protected String certPassword;

    protected DirectoryService directoryService;
    protected LdapServer ldapServer;


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
        this.enableSSL = Boolean.valueOf(readProperty(PROPERTY_ENABLE_SSL, "false"));
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
                ", ldapSaslPrincipal=" + ldapSaslPrincipal + ", directoryServiceFactory=" + directoryServiceFactory + ", ldif=" + ldifFile);

        this.directoryService = createDirectoryService();

        log.info("Importing LDIF: " + ldifFile);
        importLdif();

        log.info("Creating LDAP Server");
        this.ldapServer = createLdapServer();
    }


    public void start() throws Exception {
        log.info("Starting LDAP Server");
        ldapServer.start();
        log.info("LDAP Server started");
    }


    protected DirectoryService createDirectoryService() throws Exception {
        // Parse "keycloak" from "dc=keycloak,dc=org"
        String dcName = baseDN.split(",")[0];
        dcName = dcName.substring(dcName.indexOf("=") + 1);

        DirectoryServiceFactory dsf;
        if (this.directoryServiceFactory.equals(DSF_INMEMORY)) {
            dsf = new InMemoryDirectoryServiceFactory();
        } else if (this.directoryServiceFactory.equals(DSF_FILE)) {
            dsf = new FileDirectoryServiceFactory();
        } else {
            throw new IllegalStateException("Unknown value of directoryServiceFactory: " + this.directoryServiceFactory);
        }

        DirectoryService service = dsf.getDirectoryService();
        service.setAccessControlEnabled(false);
        service.setAllowAnonymousAccess(false);
        service.getChangeLog().setEnabled(false);

        dsf.init(dcName + "DS");

        SchemaManager schemaManager = service.getSchemaManager();

        PartitionFactory partitionFactory = dsf.getPartitionFactory();
        Partition partition = partitionFactory.createPartition(
                schemaManager,
                service.getDnFactory(),
                dcName,
                this.baseDN,
                1000,
                new File(service.getInstanceLayout().getPartitionsDirectory(), dcName));
        partition.setCacheService( service.getCacheService() );
        partition.initialize();

        partition.setSchemaManager( schemaManager );

        // Inject the partition into the DirectoryService
        service.addPartition( partition );

        // Last, process the context entry
        String entryLdif =
                "dn: " + baseDN + "\n" +
                        "dc: " + dcName + "\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n";
        importLdifContent(service, entryLdif);

        return service;
    }


    protected LdapServer createLdapServer() {
        LdapServer ldapServer = new LdapServer();

        ldapServer.setServiceName("DefaultLdapServer");
        ldapServer.setSearchBaseDn(this.baseDN);

        // Read the transports
        Transport ldap = new TcpTransport(this.bindHost, this.bindPort, 3, 50);
        ldapServer.addTransports( ldap );
        if (enableSSL) {
            Transport ldaps = new TcpTransport(this.bindHost, this.bindLdapsPort, 3, 50);
            ldaps.setEnableSSL(true);
            ldapServer.setKeystoreFile(keystoreFile);
            ldapServer.setCertificatePassword(certPassword);
            ldapServer.addTransports( ldaps );
        }

        // Associate the DS to this LdapServer
        ldapServer.setDirectoryService( directoryService );

        // Propagate the anonymous flag to the DS
        directoryService.setAllowAnonymousAccess(false);

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
        if (is == null) {
            throw new IllegalStateException("LDIF file not found on classpath or on file system. Location was: " + ldifFile);
        }

        final String ldifContent = StrSubstitutor.replace(StreamUtil.readString(is), map);
        log.info("Content of LDIF: " + ldifContent);
        final SchemaManager schemaManager = directoryService.getSchemaManager();

        importLdifContent(directoryService, ldifContent);
    }

    private static void importLdifContent(DirectoryService directoryService, String ldifContent) throws Exception {
        LdifReader ldifReader = new LdifReader(IOUtils.toInputStream(ldifContent));

        try {
            for (LdifEntry ldifEntry : ldifReader) {
                try {
                    directoryService.getAdminSession().add(new DefaultEntry(directoryService.getSchemaManager(), ldifEntry.getEntry()));
                } catch (LdapEntryAlreadyExistsException ignore) {
                    log.info("Entry " + ldifEntry.getDn() + " already exists. Ignoring");
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
