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
public class SecureClientAuthEnforceExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "secure-client-authn-executor";

    public static final String IS_AUGMENT = "is-augment";
    public static final String CLIENT_AUTHNS = "client-authns";
    public static final String CLIENT_AUTHNS_AUGMENT = "client-authns-augment";

    private List<ProviderConfigProperty> configProperties = new ArrayList<>();

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new SecureClientAuthEnforceExecutor(session);
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        ProviderConfigProperty isAugmentProperty = new ProviderConfigProperty(
                IS_AUGMENT, "Augment Configuration", "If On, then the during client creation or update, the configuration of the client will be augmented to enforce the authentication method to new clients",
                ProviderConfigProperty.BOOLEAN_TYPE, false);

        List<String> clientAuthProviders = factory.getProviderFactoriesStream(ClientAuthenticator.class)
                .map(ProviderFactory::getId)
                .collect(Collectors.toList());

        ProviderConfigProperty clientAuthnsProperty = new ProviderConfigProperty(
                CLIENT_AUTHNS, "Client Authentication Methods", "List of available client authentication methods, which are allowed for clients to use. Other client authentication methods will not be allowed.",
                ProviderConfigProperty.MULTIVALUED_LIST_TYPE, null);
        clientAuthnsProperty.setOptions(clientAuthProviders);

        ProviderConfigProperty clientAuthnsAugment = new ProviderConfigProperty(
                CLIENT_AUTHNS_AUGMENT, "Augment Client Authentication Method", "If 'Augment Configuration' is ON, then this client authentication method will be set as the authentication method to new clients",
                ProviderConfigProperty.LIST_TYPE, JWTClientAuthenticator.PROVIDER_ID);
        clientAuthnsAugment.setOptions(clientAuthProviders);

        configProperties = Arrays.asList(isAugmentProperty, clientAuthnsProperty, clientAuthnsAugment);
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
