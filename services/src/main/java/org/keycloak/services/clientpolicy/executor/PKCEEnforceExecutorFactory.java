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

package org.keycloak.services.clientpolicy.executor;

import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientpolicy.executor.AbstractAugumentingClientRegistrationPolicyExecutorFactory;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

public class PKCEEnforceExecutorFactory extends AbstractAugumentingClientRegistrationPolicyExecutorFactory {

    public static final String PROVIDER_ID = "pkce-enforce-executor";

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session, ComponentModel model) {
        return new PKCEEnforceExecutor(session, model);
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
        return "It makes the client enforce Proof Key for Code Exchange operation.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return super.getConfigProperties();
    }

}
