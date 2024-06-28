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
import java.util.stream.Stream;

import org.keycloak.models.AuthenticatorConfigModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultDeployedConfigurationsProvider implements DeployedConfigurationsProvider {

    private final Map<String, AuthenticatorConfigModel> deployedAuthenticatorConfigs;
    public DefaultDeployedConfigurationsProvider(Map<String, AuthenticatorConfigModel> deployedAuthenticatorConfigs) {
        this.deployedAuthenticatorConfigs = deployedAuthenticatorConfigs;
    }

    @Override
    public void registerDeployedAuthenticatorConfig(AuthenticatorConfigModel model) {
        deployedAuthenticatorConfigs.put(model.getId(), model);
    }

    @Override
    public Stream<AuthenticatorConfigModel> getDeployedAuthenticatorConfigs() {
        return deployedAuthenticatorConfigs.values().stream();
    }

    @Override
    public void close() {

    }
}
