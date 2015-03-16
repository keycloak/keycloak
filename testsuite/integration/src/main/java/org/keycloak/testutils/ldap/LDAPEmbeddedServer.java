package org.keycloak.testutils.ldap;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.jboss.logging.Logger;
import org.keycloak.util.StreamUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPEmbeddedServer {

    private static final Logger log = Logger.getLogger(LDAPEmbeddedServer.class);

    protected final String baseDN;
    protected final String bindHost;
    protected final int bindPort;
    protected final String ldifFile;
    protected final String ldapSaslPrincipal;

    protected DirectoryService directoryService;
    protected LdapServer ldapServer;


    public static void main(String[] args) throws Exception {
        EmbeddedServersFactory factory = EmbeddedServersFactory.readConfiguration();
        LDAPEmbeddedServer ldapEmbeddedServer = factory.createLdapServer();
        ldapEmbeddedServer.init();
        ldapEmbeddedServer.start();
    }

    public LDAPEmbeddedServer(String baseDN, String bindHost, int bindPort, String ldifFile, String ldapSaslPrincipal) {
        this.baseDN = baseDN;
        this.bindHost = bindHost;
        this.bindPort = bindPort;
        this.ldifFile = ldifFile;
        this.ldapSaslPrincipal = ldapSaslPrincipal;
    }


    public void init() throws Exception {
        log.info("Creating LDAP Directory Service. Config: baseDN=" + baseDN + ", bindHost=" + bindHost + ", bindPort=" + bindPort +
                ", ldapSaslPrincipal=" + ldapSaslPrincipal);

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
        String dcName = baseDN.split(",")[0].substring(3);

        InMemoryDirectoryServiceFactory dsf = new InMemoryDirectoryServiceFactory();

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
        DSAnnotationProcessor.injectEntries(service, entryLdif);

        return service;
    }


    protected LdapServer createLdapServer() {
        LdapServer ldapServer = new LdapServer();

        ldapServer.setServiceName("DefaultLdapServer");
        ldapServer.setSearchBaseDn(this.baseDN);

        // Read the transports
        Transport ldap = new TcpTransport(this.bindHost, this.bindPort, 3, 50);
        ldapServer.addTransports( ldap );

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

        // For now, assume that LDIF file is on classpath
        InputStream is = getClass().getClassLoader().getResourceAsStream(ldifFile);
        if (is == null) {
            throw new IllegalStateException("LDIF file not found on classpath. Location was: " + ldifFile);
        }

        final String ldifContent = StrSubstitutor.replace(StreamUtil.readString(is), map);
        log.info("Content of LDIF: " + ldifContent);
        final SchemaManager schemaManager = directoryService.getSchemaManager();

        for (LdifEntry ldifEntry : new LdifReader(IOUtils.toInputStream(ldifContent))) {
            try {
                directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
            } catch (LdapEntryAlreadyExistsException ignore) {
                log.debug("Entry " + ldifEntry.getNewRdn() + " already exists. Ignoring");
            }
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

        log.info("Removing Directory service workfiles.");
        FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
    }

}
