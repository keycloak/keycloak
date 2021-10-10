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

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.jboss.marshalling.core.JBossUserMarshaller;
import org.infinispan.manager.DefaultCacheManager;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ManagedCacheManagerProvider;
import org.keycloak.Config;
import org.keycloak.util.Environment;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class QuarkusCacheManagerProvider implements ManagedCacheManagerProvider {

    private static final Logger log = Logger.getLogger(QuarkusCacheManagerProvider.class);

    @Override
    public <C> C getCacheManager(Config.Scope config) {
        try {
            ConfigurationBuilderHolder builder = new ParserRegistry().parse(loadConfiguration(config));

            if (builder.getNamedConfigurationBuilders().get("sessions").clustering().cacheMode().isClustered()) {
                configureTransportStack(config, builder);
            }

            // For Infinispan 10, we go with the JBoss marshalling.
            // TODO: This should be replaced later with the marshalling recommended by infinispan. Probably protostream.
            // See https://infinispan.org/docs/stable/titles/developing/developing.html#marshalling for the details
            builder.getGlobalConfigurationBuilder().serialization().marshaller(new JBossUserMarshaller());

            return (C) new DefaultCacheManager(builder, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private URL loadConfiguration(Config.Scope config) {
        String pathPrefix;
        String homeDir = Environment.getHomeDir();
        
        if (homeDir == null) {
            log.warn("Keycloak home directory not set");
            pathPrefix = "";
        } else {
            pathPrefix = homeDir + "/conf/";
        }

        // Always try to use "configFile" if explicitly specified
        String configFile = config.get("configFile");
        if (configFile != null) {
            Path configPath = Paths.get(pathPrefix + configFile);
            String path;

            if (configPath.toFile().exists()) {
                path = configPath.toFile().getAbsolutePath();
            } else {
                path = configPath.getFileName().toString();
            }

            log.infof("Loading cluster configuration from %s", configPath);
            URL url = FileLookupFactory.newInstance().lookupFileLocation(path, Thread.currentThread().getContextClassLoader());
            
            if (url == null) {
                throw new IllegalArgumentException("Could not load cluster configuration file at [" + configPath + "]");
            }

            return url;
        } else {
            throw new IllegalArgumentException("Option 'configFile' needs to be specified");
        }
    }

    private void configureTransportStack(Config.Scope config, ConfigurationBuilderHolder builder) {
        String transportStack = config.get("stack");
        
        if (transportStack != null) {
            builder.getGlobalConfigurationBuilder().transport().defaultTransport()
                    .addProperty("configurationFile", "default-configs/default-jgroups-" + transportStack + ".xml");
        }
    }
}
