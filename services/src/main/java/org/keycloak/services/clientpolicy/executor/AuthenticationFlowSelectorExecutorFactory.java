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

import java.util.Arrays;
import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:ggrazian@redhat.com">Giuseppe Graziano</a>
 */
public class AuthenticationFlowSelectorExecutorFactory implements ClientPolicyExecutorProviderFactory  {

    public static final String PROVIDER_ID = "auth-flow-enforcer";

    public static final String AUTH_FLOW_ALIAS = "auth-flow-alias";
    public static final String AUTH_FLOW_LOA = "auth-flow-loa";

    private static final ProviderConfigProperty AUTH_FLOW_ALIAS_PROPERTY = new ProviderConfigProperty(
            AUTH_FLOW_ALIAS, "Auth Flow Alias", "Insert the alias of the authentication flow",
            ProviderConfigProperty.STRING_TYPE, null);

    private static final ProviderConfigProperty AUTH_FLOW_LOA_PROPERTY = new ProviderConfigProperty(
            AUTH_FLOW_LOA, "Auth Flow Loa", "Insert the loa to enforce when the selected authentication flow is executed",
            ProviderConfigProperty.INTEGER_TYPE, 1);

    @Override
    public AuthenticationFlowSelectorExecutor create(KeycloakSession session) {
        return new AuthenticationFlowSelectorExecutor();
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
        return "";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Arrays.asList(AUTH_FLOW_ALIAS_PROPERTY, AUTH_FLOW_LOA_PROPERTY);
    }

}
