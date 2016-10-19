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

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.schema.LdapComparator;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.NormalizingComparator;
import org.apache.directory.api.ldap.model.schema.registries.ComparatorRegistry;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.factory.LdifPartitionFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.i18n.I18n;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Slightly modified version of {@link DefaultDirectoryServiceFactory} which allows persistence among restarts and uses LDIF partitions by default
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class FileDirectoryServiceFactory implements DirectoryServiceFactory {

    /** A logger for this class */
    private static final Logger LOG = Logger.getLogger(FileDirectoryServiceFactory.class);

    /** The directory service. */
    private DirectoryService directoryService;

    /** The partition factory. */
    private PartitionFactory partitionFactory;


    public FileDirectoryServiceFactory()
    {
        try
        {
            // creating the instance here so that
            // we we can set some properties like accesscontrol, anon access
            // before starting up the service
            directoryService = new DefaultDirectoryService();

            // no need to register a shutdown hook during tests because this
            // starts a lot of threads and slows down test execution
            directoryService.setShutdownHookEnabled( false );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }

        try
        {
            String typeName = System.getProperty( "apacheds.partition.factory" );

            if ( typeName != null )
            {
                Class<? extends PartitionFactory> type = ( Class<? extends PartitionFactory> ) Class.forName( typeName );
                partitionFactory = type.newInstance();
            }
            else
            {
                // partitionFactory = new JdbmPartitionFactory();
                partitionFactory = new LdifPartitionFactory();
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Error instantiating custom partiton factory", e );
            throw new RuntimeException( e );
        }
    }


    public FileDirectoryServiceFactory( DirectoryService directoryService, PartitionFactory partitionFactory )
    {
        this.directoryService = directoryService;
        this.partitionFactory = partitionFactory;
    }


    /**
     * {@inheritDoc}
     */
    public void init( String name ) throws Exception
    {
        if ( ( directoryService != null ) && directoryService.isStarted() ) {
            return;
        }

        build(name);
    }


    /**
     * Build the working directory
     */
    private void buildInstanceDirectory( String name ) throws IOException
    {
        String instanceDirectory = System.getProperty( "workingDirectory" );

        if ( instanceDirectory == null )
        {
            instanceDirectory = System.getProperty( "java.io.tmpdir" ) + "/server-work-" + name;
        }

        InstanceLayout instanceLayout = new InstanceLayout( instanceDirectory );

        /*if ( instanceLayout.getInstanceDirectory().exists() )
        {
            try
            {
                FileUtils.deleteDirectory(instanceLayout.getInstanceDirectory());
            }
            catch ( IOException e )
            {
                LOG.warn( "couldn't delete the instance directory before initializing the DirectoryService", e );
            }
        }*/

        directoryService.setInstanceLayout( instanceLayout );
    }


    /**
     * Inits the schema and schema partition.
     */
    private void initSchema() throws Exception
    {
        File workingDirectory = directoryService.getInstanceLayout().getPartitionsDirectory();

        // Extract the schema on disk (a brand new one) and load the registries
        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( workingDirectory );

        try
        {
            extractor.extractOrCopy();
        }
        catch ( IOException ioe )
        {
            // The schema has already been extracted, bypass
        }

        SchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse
        // and normalize their suffix Dn
        schemaManager.loadAllEnabled();

        // Tell all the normalizer comparators that they should not normalize anything
        ComparatorRegistry comparatorRegistry = schemaManager.getComparatorRegistry();

        for ( LdapComparator<?> comparator : comparatorRegistry )
        {
            if ( comparator instanceof NormalizingComparator)
            {
                ( ( NormalizingComparator ) comparator ).setOnServer();
            }
        }

        directoryService.setSchemaManager( schemaManager );

        // Init the LdifPartition
        LdifPartition ldifPartition = new LdifPartition( schemaManager, directoryService.getDnFactory() );
        ldifPartition.setPartitionPath( new File( workingDirectory, "schema" ).toURI() );
        SchemaPartition schemaPartition = new SchemaPartition( schemaManager );
        schemaPartition.setWrappedPartition( ldifPartition );
        directoryService.setSchemaPartition( schemaPartition );

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            throw new Exception( I18n.err(I18n.ERR_317, Exceptions.printErrors(errors)) );
        }
    }


    /**
     * Inits the system partition.
     *
     * @throws Exception the exception
     */
    private void initSystemPartition() throws Exception
    {
        // change the working directory to something that is unique
        // on the system and somewhere either under target directory
        // or somewhere in a temp area of the machine.

        // Inject the System Partition
        Partition systemPartition = partitionFactory.createPartition( directoryService.getSchemaManager(),
                directoryService.getDnFactory(),
                "system", ServerDNConstants.SYSTEM_DN, 500,
                new File( directoryService.getInstanceLayout().getPartitionsDirectory(), "system" ) );
        systemPartition.setSchemaManager(directoryService.getSchemaManager());

        partitionFactory.addIndex(systemPartition, SchemaConstants.OBJECT_CLASS_AT, 100 );

        directoryService.setSystemPartition( systemPartition );
    }


    /**
     * Builds the directory server instance.
     *
     * @param name the instance name
     */
    private void build( String name ) throws Exception
    {
        directoryService.setInstanceId( name );
        buildInstanceDirectory( name );

        CacheService cacheService = new CacheService();
        cacheService.initialize( directoryService.getInstanceLayout() );

        directoryService.setCacheService( cacheService );

        // Init the service now
        initSchema();
        initSystemPartition();

        directoryService.startup();
    }


    /**
     * {@inheritDoc}
     */
    public DirectoryService getDirectoryService() throws Exception
    {
        return directoryService;
    }


    /**
     * {@inheritDoc}
     */
    public PartitionFactory getPartitionFactory() throws Exception
    {
        return partitionFactory;
    }
}
