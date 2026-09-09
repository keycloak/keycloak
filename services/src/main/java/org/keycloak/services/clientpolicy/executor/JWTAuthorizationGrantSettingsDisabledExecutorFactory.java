/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.services.clientpolicy.executor;

import java.util.Collections;
import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Factory for the executor that keeps the JWT authorization grant settings of a client out of
 * client requests, so only a realm administrator with the policy disabled can change them.
 *
 * @author <a href="mailto:mahdi.a.alhakim@gmail.com">Mahdi Alhakim</a>
 */
public class JWTAuthorizationGrantSettingsDisabledExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "jwt-authorization-grant-settings-disabled";

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new JWTAuthorizationGrantSettingsDisabledExecutor();
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
               It rejects a request that sets or changes the JWT authorization grant settings of a client, which are
               the identity provider allow list and the token audience allow list. Enabling the grant on a client
               that already exists is rejected as well, while turning it off stays allowed. A request that repeats
               the value the client already has is permitted, so existing configuration is preserved. Combine it
               with the client-updater-context condition to restrict it to requests that the client itself makes.
               """;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }
}
