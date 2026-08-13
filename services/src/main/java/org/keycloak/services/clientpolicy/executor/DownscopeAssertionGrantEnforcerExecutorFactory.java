/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.util.Collections;
import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * <p>Factory that enforces the grant to only use scopes that are already present in
 * the initial assertion. For the moment it can only be used in the Token Exchange
 * context.</p>
 *
 * @author rmartinc
 */
public class DownscopeAssertionGrantEnforcerExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "downscope-assertion-grant-enforcer";

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new DownscopeAssertionGrantEnforcerExecutor(session);
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
        return """
               It ensures that the scopes in the final access token are limited to the ones already present in the JWT assertion passed.
               For the moment, the executor applies to certain grants where some initial token/assertion is passed (for example
               subject_token in case of Standard Token Exchange grant). The limitation is done over the scopes that are
               present in the initial token/assertion, returning an error if any extra scope is requested.
               """;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }
}
