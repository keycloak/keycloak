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

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.schema.LdapComparator;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.NormalizingComparator;
import org.apache.directory.api.ldap.model.schema.registries.ComparatorRegistry;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.loader.JarLdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.factory.AvlPartitionFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.i18n.I18n;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;

/**
 * Factory for a fast (mostly in-memory-only) ApacheDS DirectoryService. Use only for tests!!
 *
 * @author Josef Cacek
 */
class InMemoryDirectoryServiceFactory implements DirectoryServiceFactory {

    private static final Logger log = Logger.getLogger(InMemoryDirectoryServiceFactory.class);
    private static final int PAGE_SIZE = 30;

    private final DirectoryService directoryService;
    private final PartitionFactory partitionFactory;

    /**
     * Default constructor which creates {@link DefaultDirectoryService} instance and configures {@link AvlPartitionFactory} as
     * the {@link PartitionFactory} implementation.
     */
    public InMemoryDirectoryServiceFactory() {
        try {
            directoryService = new DefaultDirectoryService();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        directoryService.setShutdownHookEnabled(false);
        partitionFactory = new AvlPartitionFactory();
    }

    /**
     * Constructor which uses provided {@link DirectoryService} and {@link PartitionFactory} implementations.
     *
     * @param directoryService must be not-<code>null</code>
     * @param partitionFactory must be not-<code>null</code>
     */
    public InMemoryDirectoryServiceFactory(DirectoryService directoryService, PartitionFactory partitionFactory) {
        this.directoryService = directoryService;
        this.partitionFactory = partitionFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(String name) throws Exception {
        if ((directoryService != null) && directoryService.isStarted()) {
            return;
        }
        directoryService.setInstanceId(name);

        // instance layout
        InstanceLayout instanceLayout = new InstanceLayout(System.getProperty("java.io.tmpdir") + "/server-work-inmemory-" + name);
        if (instanceLayout.getInstanceDirectory().exists()) {
            try {
                FileUtils.deleteDirectory(instanceLayout.getInstanceDirectory());
            } catch (IOException e) {
                log.warn("couldn't delete the instance directory before initializing the DirectoryService", e);
            }
        }
        directoryService.setInstanceLayout(instanceLayout);

        // EhCache in disabled-like-mode
        Configuration ehCacheConfig = new Configuration();
        ehCacheConfig.setName(name);
        CacheConfiguration defaultCache = new CacheConfiguration(name + "-default", 1).eternal(false).timeToIdleSeconds(30)
                .timeToLiveSeconds(30).overflowToDisk(false);
        ehCacheConfig.addDefaultCache(defaultCache);
        CacheService cacheService = new CacheService(new CacheManager(ehCacheConfig));
        directoryService.setCacheService(cacheService);

        // Init the schema
        // SchemaLoader loader = new SingleLdifSchemaLoader();
        SchemaLoader loader = new JarLdifSchemaLoader();
        SchemaManager schemaManager = new DefaultSchemaManager(loader);
        schemaManager.loadAllEnabled();
        ComparatorRegistry comparatorRegistry = schemaManager.getComparatorRegistry();
        for (LdapComparator<?> comparator : comparatorRegistry) {
            if (comparator instanceof NormalizingComparator) {
                ((NormalizingComparator) comparator).setOnServer();
            }
        }
        directoryService.setSchemaManager(schemaManager);
        InMemorySchemaPartition inMemorySchemaPartition = new InMemorySchemaPartition(schemaManager);
        SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
        schemaPartition.setWrappedPartition(inMemorySchemaPartition);
        directoryService.setSchemaPartition(schemaPartition);
        List<Throwable> errors = schemaManager.getErrors();
        if (errors.size() != 0) {
            throw new Exception(I18n.err(I18n.ERR_317, Exceptions.printErrors(errors)));
        }

        // Init system partition
        Partition systemPartition = partitionFactory.createPartition(directoryService.getSchemaManager(),
                directoryService.getDnFactory(), "system",
                ServerDNConstants.SYSTEM_DN, 500,
                new File(directoryService.getInstanceLayout().getPartitionsDirectory(),
                        "system"));

        systemPartition.setSchemaManager(directoryService.getSchemaManager());
        partitionFactory.addIndex(systemPartition, SchemaConstants.OBJECT_CLASS_AT, 100);
        directoryService.setSystemPartition(systemPartition);

        // Find Normalization interceptor in chain and add our range emulated interceptor
        List<Interceptor> interceptors = directoryService.getInterceptors();
        int insertionPosition = -1;
        for (int pos = 0; pos < interceptors.size(); ++pos) {
            Interceptor interceptor = interceptors.get(pos);
            if (interceptor instanceof NormalizationInterceptor) {
                insertionPosition = pos;
            }
        }
        interceptors.add(insertionPosition + 1, new RangedAttributeInterceptor("member", PAGE_SIZE));
        directoryService.setInterceptors(interceptors);

        directoryService.startup();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectoryService getDirectoryService() throws Exception {
        return directoryService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PartitionFactory getPartitionFactory() throws Exception {
        return partitionFactory;
    }
}
