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

package org.keycloak.services.clientregistration.policy.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientregistration.policy.AbstractClientRegistrationPolicyFactory;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientScopesClientRegistrationPolicyFactory extends AbstractClientRegistrationPolicyFactory {

    private List<ProviderConfigProperty> configProperties;

    // Keeping the name for backwards compatibility
    public static final String PROVIDER_ID = "allowed-client-templates";

    public static final String ALLOWED_CLIENT_SCOPES = "allowed-client-scopes";

    public static final String ALLOW_DEFAULT_SCOPES = "allow-default-scopes";

    @Override
    public ClientRegistrationPolicy create(KeycloakSession session, ComponentModel model) {
        return new ClientScopesClientRegistrationPolicy(session, model);
    }

    @Override
    public String getHelpText() {
        return "When present, it allows to specify whitelist of client scopes, which will be allowed in representation of registered (or updated) client";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        List<ProviderConfigProperty> configProps = new LinkedList<>();

        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ALLOWED_CLIENT_SCOPES);
        property.setLabel("allowed-client-scopes.label");
        property.setHelpText("allowed-client-scopes.tooltip");
        property.setType(ProviderConfigProperty.MULTIVALUED_LIST_TYPE);

        if (session != null) {
            property.setOptions(getClientScopes(session));
        }
        configProps.add(property);

        property = new ProviderConfigProperty();
        property.setName(ALLOW_DEFAULT_SCOPES);
        property.setLabel("allow-default-scopes.label");
        property.setHelpText("allow-default-scopes.tooltip");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(true);
        configProps.add(property);

        configProperties = configProps;
        return configProperties;
    }

    private List<String> getClientScopes(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            return Collections.emptyList();
        } else {
            return realm.getClientScopesStream().map(ClientScopeModel::getName).collect(Collectors.toList());
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return getConfigProperties(null);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
