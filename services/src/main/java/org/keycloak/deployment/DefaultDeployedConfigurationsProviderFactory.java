/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.deployment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.Config;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultDeployedConfigurationsProviderFactory implements DeployedConfigurationsProviderFactory {

    public static final String PROVIDER_ID = "default";
    private final Map<String, AuthenticatorConfigModel> deployedAuthenticatorConfigs = new ConcurrentHashMap<>();
    @Override
    public DeployedConfigurationsProvider create(KeycloakSession session) {
        return new DefaultDeployedConfigurationsProvider(deployedAuthenticatorConfigs);
    }

    @Override
    public void init(Config.Scope config) {

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
}
