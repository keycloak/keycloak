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
package org.keycloak.broker.provider;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Pedro Igor
 */
public interface IdentityProviderFactory<T extends IdentityProvider> extends ProviderFactory<T>, ConfiguredProvider {

    /**
     * <p>A friendly name for this factory.</p>
     *
     * @return
     */
    String getName();

    /**
     * <p>Creates an {@link IdentityProvider} based on the configuration contained in
     * <code>model</code>.</p>
     *
     * @param session
     * @param model The configuration to be used to create the identity provider.
     * @return
     */
    T create(KeycloakSession session, IdentityProviderModel model);

    /**
     * <p>Creates an {@link IdentityProvider} based on the configuration from
     * <code>inputStream</code>.</p>
     *
     * @param session
     * @param config The configuration for the provider
     * @return
     */
    Map<String, String> parseConfig(KeycloakSession session, String config);

    /**
     * <p>Creates a provider specific {@link IdentityProviderModel} instance.
     * 
     * <p>Providers may want to implement their own {@link IdentityProviderModel} type so that validations
     * can be performed when managing the provider configuration
     * 
     * @return the provider specific instance
     */
    IdentityProviderModel createConfig();

    default List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    default String getHelpText() { return ""; }
}
