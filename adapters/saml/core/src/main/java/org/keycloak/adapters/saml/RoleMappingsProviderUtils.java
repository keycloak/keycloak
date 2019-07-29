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

package org.keycloak.adapters.saml;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.jboss.logging.Logger;
import org.keycloak.adapters.saml.config.SP;
import org.keycloak.adapters.saml.config.parsers.ResourceLoader;

/**
 * Utility class that allows for the instantiation and configuration of role mappings providers.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class RoleMappingsProviderUtils {

    private static final Logger logger = Logger.getLogger(RoleMappingsProviderUtils.class);

    /**
     * Loads the available implementations of {@link RoleMappingsProvider} and selects the provider that matches the id
     * that was configured in {@code keycloak-saml.xml}. The selected provider is then initialized with the specified
     * {@link SamlDeployment}, {@link ResourceLoader} and configuration as specified in {@code keycloak-saml.xml}. If no
     * provider was configured for the SP then {@code null} is returned.
     *
     * @param deployment a reference to the {@link SamlDeployment} that is being built.
     * @param loader a reference to the {@link ResourceLoader} that allows the provider implementation to load additional
     *               resources from the SP application WAR.
     * @param providerConfig the provider configuration properties as configured in {@code keycloak-saml.xml}. Can contain
 *                   an empty properties object if no configuration properties were specified for the provider.
     * @return the instantiated and initialized {@link RoleMappingsProvider} or {@code null} if no provider was configured
     *               for the SP.
     */
    public static RoleMappingsProvider bootstrapRoleMappingsProvider(final SamlDeployment deployment, final ResourceLoader loader, final SP.RoleMappingsProviderConfig providerConfig) {
        String providerId;
        if (providerConfig == null || providerConfig.getId() == null) {
            return null;
        } else {
            providerId = providerConfig.getId();
        }

        // load the available role mappings providers and check if one corresponds to the specified id.
        Map<String, RoleMappingsProvider> roleMappingsProviders = new HashMap<>();
        loadProviders(roleMappingsProviders, RoleMappingsProviderUtils.class.getClassLoader());
        loadProviders(roleMappingsProviders, Thread.currentThread().getContextClassLoader());

        RoleMappingsProvider provider = roleMappingsProviders.get(providerId);
        if (provider == null) {
            throw new RuntimeException("Couldn't find RoleMappingsProvider implementation class with id: " + providerId +
                    ". Loaded role mappings providers: " + roleMappingsProviders.keySet());
        }

        provider.init(deployment, loader, providerConfig != null ? providerConfig.getConfiguration() : new Properties());
        return provider;
    }

    /**
     * Loads the {@code RoleMappingsProvider} implementations using the specified {@code ClassLoader}.
     *
     * @param providers the {@code Map} used to store the loaded providers by id.
     * @param classLoader the {@code ClassLoader} that is to be used to load to provider implementations.
     */
    private static void loadProviders(Map<String, RoleMappingsProvider> providers, ClassLoader classLoader) {
        for (RoleMappingsProvider provider : ServiceLoader.load(RoleMappingsProvider.class, classLoader)) {
            logger.debugf("Loaded RoleMappingsProvider %s", provider.getId());
            providers.put(provider.getId(), provider);
        }
    }
}
