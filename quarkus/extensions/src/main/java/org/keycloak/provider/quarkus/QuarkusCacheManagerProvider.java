/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.provider.quarkus;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.cluster.ManagedCacheManagerProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class QuarkusCacheManagerProvider implements ManagedCacheManagerProvider {

    private static final Logger log = Logger.getLogger(QuarkusCacheManagerProvider.class); 
    
    private static final String DEFAULT_CONFIGURATION_FILE_NAME = "cluster.xml";

    @Override
    public <C> C getCacheManager(Config.Scope config) {
        try {
            InputStream configurationStream = loadConfiguration(config);
            ConfigurationBuilderHolder builder = new ParserRegistry().parse(configurationStream);

            if (builder.getNamedConfigurationBuilders().get("sessions").clustering().cacheMode().isClustered()) {
                configureTransportStack(config, builder);
            }

            return (C) new DefaultCacheManager(builder, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream loadConfiguration(Config.Scope config) throws FileNotFoundException {
        String homeDir = System.getProperty("keycloak.home.dir");
        
        if (homeDir == null) {
            log.warn("Keycloak home directory not set.");
            return loadDefaultConfiguration(config);
        }

        Path configPath = Paths.get(homeDir + "/conf/" + getConfigFileName(config));
        
        if (configPath.toFile().exists()) {
            log.debugf("Loading cluster configuration from %s", configPath);
            return FileLookupFactory.newInstance()
                    .lookupFileStrict(configPath.toUri(), Thread.currentThread().getContextClassLoader());
        }

        log.infof("Clustering configuration file not found at %s.", configPath);

        return loadDefaultConfiguration(config);
    }

    private InputStream loadDefaultConfiguration(Config.Scope config) throws FileNotFoundException {
        if (config.getBoolean("clustered", false)) {
            log.debugf("Using default clustered cache configuration.");
            return FileLookupFactory.newInstance()
                    .lookupFileStrict("default-clustered-cache.xml", Thread.currentThread().getContextClassLoader());    
        }

        log.debug("Using default local cache configuration.");

        return FileLookupFactory.newInstance()
                .lookupFileStrict("default-local-cache.xml", Thread.currentThread().getContextClassLoader());
    }

    private String getConfigFileName(Config.Scope config) {
        String configFile = config.get("configFile");
        return configFile == null ? DEFAULT_CONFIGURATION_FILE_NAME : configFile;
    }

    private void configureTransportStack(Config.Scope config, ConfigurationBuilderHolder builder) {
        String transportStack = config.get("stack");
        
        if (transportStack != null) {
            builder.getGlobalConfigurationBuilder().transport().defaultTransport()
                    .addProperty("configurationFile", "default-configs/default-jgroups-" + transportStack + ".xml");
        }
    }
}
