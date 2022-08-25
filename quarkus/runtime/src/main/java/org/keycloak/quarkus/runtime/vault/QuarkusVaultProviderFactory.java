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

package org.keycloak.quarkus.runtime.vault;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.vault.AbstractVaultProviderFactory;
import org.keycloak.vault.VaultProvider;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.runtime.VaultConfigHolder;

public class QuarkusVaultProviderFactory extends AbstractVaultProviderFactory implements EnvironmentDependentProviderFactory {

    private String[] kvPaths;
    private VaultKVSecretEngine secretEngine;

    @Override
    public VaultProvider create(KeycloakSession session) {
        return new QuarkusVaultProvider(secretEngine, kvPaths, getRealmName(session), super.keyResolvers);
    }

    @Override
    public void init(Config.Scope config) {
        super.init(config);
        kvPaths = config.getArray("paths");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        InstanceHandle<VaultKVSecretEngine> engineInstance = Arc.container().instance(VaultKVSecretEngine.class);

        if (engineInstance.isAvailable()) {
            secretEngine = engineInstance.get();
        }

        InstanceHandle<VaultConfigHolder> configInstance = Arc.container().instance(VaultConfigHolder.class);

        if (!configInstance.isAvailable() || configInstance.get().getVaultBootstrapConfig() == null) {
            throw new RuntimeException("No configuration defined for hashicorp provider.");
        }
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "hashicorp";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return getId().equals(Configuration.getRawValue("kc.vault"));
    }

    @Override
    public boolean isSupported() {
        // in quarkus we do not use this method when installing providers
        return false;
    }
}
