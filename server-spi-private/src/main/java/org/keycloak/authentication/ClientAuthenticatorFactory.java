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

package org.keycloak.authentication;

import java.util.Map;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.provider.ConfiguredPerClientProvider;
import org.keycloak.provider.ProviderFactory;

/**
 * Factory for creating ClientAuthenticator instances.  This is a singleton and created when Keycloak boots.
 *
 * You must specify a file
 * META-INF/services/org.keycloak.authentication.ClientAuthenticatorFactory in the jar that this class is contained in
 * This file must have the fully qualified class name of all your ClientAuthenticatorFactory classes
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientAuthenticatorFactory extends ProviderFactory<ClientAuthenticator>, ConfigurableAuthenticatorFactory, ConfiguredPerClientProvider {
    ClientAuthenticator create();

    /**
     * Is this authenticator configurable globally?
     *
     * @return
     */
    @Override
    boolean isConfigurable();

    /**
     * Get configuration, which needs to be used for adapter ( keycloak.json ) of particular client. Some implementations
     * may return just template and user needs to edit the values according to his environment (For example fill the location of keystore file)
     *
     * @return
     */
    Map<String, Object> getAdapterConfiguration(ClientModel client);

    /**
     * Get authentication methods for the specified protocol
     *
     * @param loginProtocol corresponds to {@link org.keycloak.protocol.LoginProtocolFactory#getId}
     * @return name of supported client authenticator methods in the protocol specific "language"
     */
    Set<String> getProtocolAuthenticatorMethods(String loginProtocol);

    /**
     * Is this authenticator supports client secret?
     *
     * @return if it supports secret
     */
    default boolean supportsSecret() {
        return false;
    }
}
