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

import java.util.LinkedList;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientregistration.policy.AbstractClientRegistrationPolicyFactory;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MaxClientsClientRegistrationPolicyFactory extends AbstractClientRegistrationPolicyFactory {

    public static final String MAX_CLIENTS = "max-clients";
    public static final ProviderConfigProperty MAX_CLIENTS_PROPERTY = new ProviderConfigProperty();

    public static final int DEFAULT_MAX_CLIENTS = 200;

    private static List<ProviderConfigProperty> configProperties = new LinkedList<>();

    static {
        MAX_CLIENTS_PROPERTY.setName(MAX_CLIENTS);
        MAX_CLIENTS_PROPERTY.setLabel("max-clients.label");
        MAX_CLIENTS_PROPERTY.setHelpText("max-clients.tooltip");
        MAX_CLIENTS_PROPERTY.setType(ProviderConfigProperty.STRING_TYPE);
        MAX_CLIENTS_PROPERTY.setDefaultValue(String.valueOf(DEFAULT_MAX_CLIENTS));
        configProperties.add(MAX_CLIENTS_PROPERTY);
    }

    public static final String PROVIDER_ID = "max-clients";

    @Override
    public ClientRegistrationPolicy create(KeycloakSession session, ComponentModel model) {
        return new MaxClientsClientRegistrationPolicy(session, model);
    }

    @Override
    public String getHelpText() {
        return "When present, then it won't be allowed to register new client if count of existing clients in realm is same or bigger than configured limit";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        ConfigurationValidationHelper.check(config)
                .checkInt(MAX_CLIENTS_PROPERTY, true);
    }
}
