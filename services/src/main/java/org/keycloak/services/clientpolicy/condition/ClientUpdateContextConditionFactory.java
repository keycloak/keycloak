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

package org.keycloak.services.clientpolicy.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class ClientUpdateContextConditionFactory implements ClientPolicyConditionProviderFactory {

    public static final String PROVIDER_ID = "clientupdatecontext-condition";

    public static final String UPDATE_CLIENT_SOURCE = "update-client-source";

    public static final String BY_AUTHENTICATED_USER = "ByAuthenticatedUser";
    public static final String BY_ANONYMOUS = "ByAnonymous";
    public static final String BY_INITIAL_ACCESS_TOKEN = "ByInitialAccessToken";
    public static final String BY_REGISTRATION_ACCESS_TOKEN = "ByRegistrationAccessToken";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty(UPDATE_CLIENT_SOURCE, null, null, ProviderConfigProperty.MULTIVALUED_LIST_TYPE, BY_AUTHENTICATED_USER);
        List<String> updateProfileValues = Arrays.asList(BY_AUTHENTICATED_USER, BY_ANONYMOUS, BY_INITIAL_ACCESS_TOKEN, BY_REGISTRATION_ACCESS_TOKEN);
        property.setOptions(updateProfileValues);
        configProperties.add(property);
    }

    @Override
    public ClientPolicyConditionProvider create(KeycloakSession session) {
        return new ClientUpdateContextCondition(session);
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
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
        return "The condition checks the context how is client created/updated to determine whether the policy is applied. For example it checks if client is created with admin REST API or OIDC dynamic client registration. And for the letter case if it is ANONYMOUS client registration or AUTHENTICATED client registration with Initial access token or Registration access token and so on.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

}
