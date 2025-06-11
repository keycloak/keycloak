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

import org.keycloak.OAuth2Constants;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientPolicyConditionRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientScopesConditionFactory extends AbstractClientPolicyConditionProviderFactory {

    public static final String PROVIDER_ID = "client-scopes";

    public static final String SCOPES = "scopes";
    public static final String TYPE = "type";
    public static final String DEFAULT = "Default";
    public static final String OPTIONAL = "Optional";
    public static final String ANY = "Any";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        addCommonConfigProperties(configProperties);

        ProviderConfigProperty property = new ProviderConfigProperty(SCOPES, PROVIDER_ID + "-condition.label", PROVIDER_ID + "-condition.tooltip", ProviderConfigProperty.MULTIVALUED_STRING_TYPE, OAuth2Constants.OFFLINE_ACCESS);
        configProperties.add(property);
        property = new ProviderConfigProperty(TYPE, "Scope Type",
                "If set to 'Default', condition evaluates to true if client has some default scopes of the values specified by the 'Expected Scopes' property. " +
                        "If set to 'Optional', condition evaluates to true if client has some optional scopes of the values specified by the 'Expected Scopes' property and at the same time, the scope were used as a value of 'scope' parameter in the request. " +
                        "If set to 'Any', condition evaluates to true if either of the 'Default' or 'Optional' conditions is satisfied.",
                ProviderConfigProperty.LIST_TYPE, OPTIONAL);
        property.setOptions(Arrays.asList(DEFAULT, OPTIONAL, ANY));
        configProperties.add(property);
    }

    @Override
    public ClientPolicyConditionProvider create(KeycloakSession session) {
        return new ClientScopesCondition(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "It uses the scopes requested or assigned in advance to the client to determine whether the policy is applied to this client. Condition is evaluated during OpenID Connect authorization request and/or token request.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ClientPolicyConditionRepresentation conditionRepresentation) throws ClientPolicyException {
        ClientScopesCondition.Configuration configuration = JsonSerialization.mapper.convertValue(conditionRepresentation.getConfiguration(), ClientScopesCondition.Configuration.class);

        if (configuration.getScopes() != null && !realm.getClientScopesStream().map(ClientScopeModel::getName).toList().containsAll(configuration.getScopes())) {
            throw new ClientPolicyException("Client scopes not allowed: " +  configuration.getScopes());
        }
    }
}
