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

import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportProvider;
import org.keycloak.exportimport.ExportProviderFactory;
import org.keycloak.exportimport.UsersExportStrategy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import static org.keycloak.exportimport.ExportImportConfig.DEFAULT_USERS_EXPORT_STRATEGY;
import static org.keycloak.exportimport.ExportImportConfig.DEFAULT_USERS_PER_FILE;

/**
 * Construct a {@link DirExportProviderFactory} to be used to export one or more realms.
 * For the sake of testing in the legacy testing setup, configurations can be overwritten via system properties.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DirExportProviderFactory implements ExportProviderFactory {

    public static final String PROVIDER_ID = "dir";
    public static final String DIR = "dir";
    public static final String REALM_NAME = "realmName";
    public static final String USERS_EXPORT_STRATEGY = "usersExportStrategy";
    public static final String USERS_PER_FILE = "usersPerFile";
    private Config.Scope config;

    @Override
    public ExportProvider create(KeycloakSession session) {
        String dir = System.getProperty(ExportImportConfig.DIR, config.get(DIR));
        String realmName = System.getProperty(ExportImportConfig.REALM_NAME, config.get(REALM_NAME));
        String usersExportStrategy = System.getProperty(ExportImportConfig.USERS_EXPORT_STRATEGY, config.get(USERS_EXPORT_STRATEGY, DEFAULT_USERS_EXPORT_STRATEGY.toString()));
        String usersPerFile = System.getProperty(ExportImportConfig.USERS_PER_FILE, config.get(USERS_PER_FILE, String.valueOf(DEFAULT_USERS_PER_FILE)));
        return new DirExportProvider(session.getKeycloakSessionFactory())
                .withDir(dir)
                .withRealmName(realmName)
                .withUsersExportStrategy(Enum.valueOf(UsersExportStrategy.class, usersExportStrategy.toUpperCase()))
                .withUsersPerFile(Integer.parseInt(usersPerFile.trim()));
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
                .helpText("Directory to export to")
                .add()

                .property()
                .name(USERS_EXPORT_STRATEGY)
                .type("string")
                .helpText("Users export strategy")
                .defaultValue(DEFAULT_USERS_EXPORT_STRATEGY)
                .add()

                .property()
                .name(USERS_PER_FILE)
                .type("int")
                .helpText("Users per exported file")
                .defaultValue(DEFAULT_USERS_PER_FILE)
                .add()

                .build();
    }

}
