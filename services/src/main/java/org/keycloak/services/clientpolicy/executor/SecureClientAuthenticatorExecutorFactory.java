/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.executor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.Config.Scope;
import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureClientAuthenticatorExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "secure-client-authenticator";

    public static final String ALLOWED_CLIENT_AUTHENTICATORS = "allowed-client-authenticators";
    public static final String DEFAULT_CLIENT_AUTHENTICATOR = "default-client-authenticator";

    private List<ProviderConfigProperty> configProperties = new ArrayList<>();

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new SecureClientAuthenticatorExecutor(session);
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        List<String> clientAuthProviders = factory.getProviderFactoriesStream(ClientAuthenticator.class)
                .map(ProviderFactory::getId)
                .collect(Collectors.toList());

        ProviderConfigProperty allowedClientAuthenticatorsProperty = new ProviderConfigProperty(
                ALLOWED_CLIENT_AUTHENTICATORS, "Allowed Client Authenticators", "List of available client authentication methods, which are allowed for clients to use. Other client authentication methods will not be allowed.",
                ProviderConfigProperty.MULTIVALUED_LIST_TYPE, null);
        allowedClientAuthenticatorsProperty.setOptions(clientAuthProviders);

        ProviderConfigProperty autoConfiguredClientAuthenticator = new ProviderConfigProperty(
                DEFAULT_CLIENT_AUTHENTICATOR, "Default Client Authenticator", "This client authentication method will be set as the authentication method to new clients during register/update request of the client in case that client does not have explicitly set other client authenticator method. If it is not set, then the client authenticator won't be set on new clients. Regardless the value of this option, client is still always validated to match with any of the allowed client authentication methods",
                ProviderConfigProperty.LIST_TYPE, JWTClientAuthenticator.PROVIDER_ID);
        autoConfiguredClientAuthenticator.setOptions(clientAuthProviders);

        configProperties = Arrays.asList(allowedClientAuthenticatorsProperty, autoConfiguredClientAuthenticator);
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "It makes the client enforce registering/updating secure client authentication.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

}
