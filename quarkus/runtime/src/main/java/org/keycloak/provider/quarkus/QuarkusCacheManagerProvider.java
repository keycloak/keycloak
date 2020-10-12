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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
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
            String configurationAsString = loadConfigurationToString(config);
            ConfigurationBuilderHolder builder = new ParserRegistry().parse(configurationAsString);

            if (builder.getNamedConfigurationBuilders().get("sessions").clustering().cacheMode().isClustered()) {
                configureTransportStack(config, builder);
            }

            return (C) new DefaultCacheManager(builder, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String loadConfigurationToString(Config.Scope config) throws FileNotFoundException {
        BufferedReader configurationReader = new BufferedReader(new InputStreamReader(loadConfiguration(config), StandardCharsets.UTF_8));
        return configurationReader.lines().collect(Collectors.joining(System.lineSeparator()));
    }

    private InputStream loadConfiguration(Config.Scope config) throws FileNotFoundException {
        String pathPrefix;
        String homeDir = Environment.getHomeDir();
        
        if (homeDir == null) {
            log.warn("Keycloak home directory not set.");
            pathPrefix = "";
        } else {
            pathPrefix = homeDir + "/conf/";
        }

        // Always try to use "configFile" if explicitly specified
        String configFile = config.get("configFile");
        if (configFile != null) {
            Path configPath = Paths.get(pathPrefix + configFile);

            if (configPath.toFile().exists()) {
                log.infof("Loading cache configuration from %s", configPath);
                return FileLookupFactory.newInstance()
                        .lookupFileStrict(configPath.toUri(), Thread.currentThread().getContextClassLoader());
            } else {
                log.infof("Loading cache configuration from %s", configPath);
                return FileLookupFactory.newInstance()
                        .lookupFileStrict(configPath.getFileName().toString(), Thread.currentThread().getContextClassLoader());
            }
        } else {
            throw new IllegalStateException("Option 'configFile' needs to be specified");
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
