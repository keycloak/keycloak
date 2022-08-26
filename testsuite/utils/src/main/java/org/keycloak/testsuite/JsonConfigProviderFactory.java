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

package org.keycloak.testsuite;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;
import org.keycloak.common.util.Resteasy;
import org.keycloak.common.util.SystemEnvProperties;
import org.keycloak.util.JsonSerialization;

public class JsonConfigProviderFactory extends org.keycloak.services.util.JsonConfigProviderFactory {

    public static final String SERVER_CONTEXT_CONFIG_PROPERTY_OVERRIDES = "keycloak.server.context.config.property-overrides";

    @Override
    protected Properties getProperties() {
        return new SystemEnvProperties(getPropertyOverrides());
    }

    private Map<String, String> getPropertyOverrides() {

        ServletContext context = Resteasy.getContextData(ServletContext.class);
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
