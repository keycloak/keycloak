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
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public abstract class AbstractClientPolicyConditionProviderFactory implements ClientPolicyConditionProviderFactory {

    public static final String IS_NEGATIVE_LOGIC = "is-negative-logic";

    private static final ProviderConfigProperty IS_NEGATIVE_LOGIC_PROPERTY = new ProviderConfigProperty(IS_NEGATIVE_LOGIC, "clientpolicycondition-is-negative-logic.label", "clientpolicycondition-is-negative-logic.tooltip", ProviderConfigProperty.BOOLEAN_TYPE, false);

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
    public List<ProviderConfigProperty> getConfigProperties() {
        return new ArrayList<>(Arrays.asList(IS_NEGATIVE_LOGIC_PROPERTY));
    }

}
