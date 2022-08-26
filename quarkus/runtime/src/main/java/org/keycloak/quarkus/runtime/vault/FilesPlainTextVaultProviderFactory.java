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
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.quarkus.runtime.configuration.Configuration;

public class FilesPlainTextVaultProviderFactory extends org.keycloak.vault.FilesPlainTextVaultProviderFactory
    implements EnvironmentDependentProviderFactory {

    public static final String ID = "file";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return getId().equals(Configuration.getRawValue("kc.vault"));
    }
}
