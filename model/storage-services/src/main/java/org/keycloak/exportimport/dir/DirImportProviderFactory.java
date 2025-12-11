/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.exportimport.dir;

import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ImportProvider;
import org.keycloak.exportimport.ImportProviderFactory;
import org.keycloak.exportimport.Strategy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import static org.keycloak.exportimport.ExportImportConfig.DEFAULT_STRATEGY;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DirImportProviderFactory implements ImportProviderFactory {

    public static final String REALM_NAME = "realmName";
    public static final String DIR = "dir";
    private static final String STRATEGY = "strategy";

    public static final String PROVIDER_ID = DirExportProviderFactory.PROVIDER_ID;

    private Config.Scope config;

    @Override
    public ImportProvider create(KeycloakSession session, Map<String, String> overrides) {
        Strategy strategy = Enum.valueOf(Strategy.class, System.getProperty(ExportImportConfig.STRATEGY, config.get(STRATEGY, DEFAULT_STRATEGY.toString())));
        String realmName = System.getProperty(ExportImportConfig.REALM_NAME, config.get(REALM_NAME));
        String dir = overrides.getOrDefault(ExportImportConfig.DIR, System.getProperty(ExportImportConfig.DIR, config.get(DIR)));
        return new DirImportProvider(session.getKeycloakSessionFactory(), strategy)
                .withDir(dir)
                .withRealmName(realmName);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
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
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(REALM_NAME)
                .type("string")
                .helpText("Realm to export")
                .add()

                .property()
                .name(DIR)
                .type("string")
                .helpText("Directory to import from")
                .add()

                .property()
                .name(STRATEGY)
                .type("string")
                .helpText("Strategy for import: " + Strategy.IGNORE_EXISTING.name() + ", " + Strategy.OVERWRITE_EXISTING)
                .add()

                .build();
    }

}
