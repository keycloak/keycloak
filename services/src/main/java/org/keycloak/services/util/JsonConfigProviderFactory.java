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

package org.keycloak.services.util;

import org.keycloak.config.ConfigProviderFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.util.SystemEnvProperties;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;

public abstract class JsonConfigProviderFactory implements ConfigProviderFactory {

    private static final Logger LOG = Logger.getLogger(JsonConfigProviderFactory.class);

    @Override
    public Optional<Config.ConfigProvider> create() {

        JsonNode node = null;

        try {
            String configDir = System.getProperty("jboss.server.config.dir");
            if (configDir != null) {
                File f = new File(configDir + File.separator + "keycloak-server.json");
                if (f.isFile()) {
                    ServicesLogger.LOGGER.loadingFrom(f.getAbsolutePath());
                    node = JsonSerialization.mapper.readTree(f);
                }
            }

            if (node == null) {
                URL resource = Thread.currentThread().getContextClassLoader().getResource("META-INF/keycloak-server.json");
                if (resource != null) {
                    ServicesLogger.LOGGER.loadingFrom(resource);
                    node = JsonSerialization.mapper.readTree(resource);
                }
            }
        } catch (IOException e) {
            LOG.warn("Failed to load JSON config", e);
        }

        return createJsonProvider(node);

    }

    protected Optional<Config.ConfigProvider> createJsonProvider(JsonNode node) {
        return Optional.ofNullable(node).map(n -> new JsonConfigProvider(n, getProperties()));
    }

    protected Properties getProperties() {
        return new SystemEnvProperties();
    }

}
