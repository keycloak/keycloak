/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy;

import java.util.Arrays;
import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientpolicy.ClientPolicyProvider;
import org.keycloak.services.clientpolicy.ClientPolicyProviderFactory;

public class DefaultClientPolicyProviderFactory implements ClientPolicyProviderFactory {

    public static final String PROVIDER_ID = "client-policy-provider";
    public static final String CONDITION_IDS = "client-policy-condition-ids";
    public static final String EXECUTOR_IDS = "client-policy-executor-ids";

    private static final ProviderConfigProperty CONDITION_IDS_PROPERTY = new ProviderConfigProperty(CONDITION_IDS, null, null, ProviderConfigProperty.LIST_TYPE, null);
    private static final ProviderConfigProperty EXECUTOR_IDS_PROPERTY = new ProviderConfigProperty(EXECUTOR_IDS, null, null, ProviderConfigProperty.LIST_TYPE, null);

    @Override
    public ClientPolicyProvider create(KeycloakSession session, ComponentModel model) {
        return new DefaultClientPolicyProvider(session, model);

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
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Arrays.asList(CONDITION_IDS_PROPERTY, EXECUTOR_IDS_PROPERTY);
    }

}
