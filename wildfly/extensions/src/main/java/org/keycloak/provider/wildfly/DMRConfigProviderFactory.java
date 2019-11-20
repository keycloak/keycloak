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

package org.keycloak.provider.wildfly;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.servlet.ServletContext;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.Config;
import org.keycloak.common.util.Resteasy;
import org.keycloak.common.util.SystemEnvProperties;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.util.JsonConfigProviderFactory;
import org.keycloak.util.JsonSerialization;

public class DMRConfigProviderFactory extends JsonConfigProviderFactory {

    // This param name is defined again in Keycloak Server Subsystem class
    // org.keycloak.subsystem.server.extension.KeycloakServerDeploymentProcessor.  We have this value in
    // two places to avoid dependency between Keycloak Subsystem and Keycloak Wildfly Extensions module.
    public static final String KEYCLOAK_CONFIG_PARAM_NAME = "org.keycloak.server-subsystem.Config";

    public static final String SERVER_CONTEXT_CONFIG_PROPERTY_OVERRIDES = "keycloak.server.context.config.property-overrides";

    private static final Logger LOG = Logger.getLogger(DMRConfigProviderFactory.class);

    @Override
    public Optional<Config.ConfigProvider> create() {

        ServletContext context = Resteasy.getContextData(ServletContext.class);

        JsonNode node = null;

        try {
            String dmrConfig = loadDmrConfig(context);
            if (dmrConfig != null) {
                node = JsonSerialization.mapper.readTree(dmrConfig);
                ServicesLogger.LOGGER.loadingFrom("standalone.xml or domain.xml");
            }
        } catch (IOException e) {
            LOG.warn("Failed to load DMR config", e);
        }

        return createJsonProvider(node);

    }

    @Override
    protected Properties getProperties() {
        return new SystemEnvProperties(getPropertyOverrides());
    }

    private String loadDmrConfig(ServletContext context) {
        String dmrConfig = context.getInitParameter(KEYCLOAK_CONFIG_PARAM_NAME);
        if (dmrConfig == null) {
            return null;
        }

        ModelNode dmrConfigNode = ModelNode.fromString(dmrConfig);
        if (dmrConfigNode.asPropertyList().isEmpty()) {
            return null;
        }

        // note that we need to resolve expressions BEFORE we convert to JSON
        return dmrConfigNode.resolve().toJSONString(true);
    }

    private Map<String, String> getPropertyOverrides() {

        ServletContext context = ResteasyProviderFactory.getContextData(ServletContext.class);
        Map<String, String> propertyOverridesMap = new HashMap<>();
        String propertyOverrides = context.getInitParameter(SERVER_CONTEXT_CONFIG_PROPERTY_OVERRIDES);

        try {
            if (context.getInitParameter(SERVER_CONTEXT_CONFIG_PROPERTY_OVERRIDES) != null) {
                JsonNode jsonObj = JsonSerialization.mapper.readTree(propertyOverrides);
                jsonObj.fields().forEachRemaining(e -> propertyOverridesMap.put(e.getKey(), e.getValue().asText()));
            }
        } catch (IOException e) {
        }

        return propertyOverridesMap;

    }

}
