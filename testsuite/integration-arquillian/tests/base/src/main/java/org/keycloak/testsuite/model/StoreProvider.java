/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model;

import org.keycloak.utils.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public enum StoreProvider {
    CHM("chm") {
        @Override
        public void addStoreOptions(List<String> commands) {
            commands.add("--storage=" + getAlias());
        }
    },
    FILE("file") {
        @Override
        public void addStoreOptions(List<String> commands) {
            commands.add("--storage=" + getAlias());
        }
    },
    JPA("jpa") {
        @Override
        public void addStoreOptions(List<String> commands) {
            commands.add("--storage=" + getAlias());
            getDbVendor().ifPresent(vendor -> commands.add("--storage-jpa-db=" + vendor));
            commands.add("--db-url=" + System.getProperty("keycloak.map.storage.connectionsJpa.url"));
            commands.add("--db-username=" + System.getProperty("keycloak.map.storage.connectionsJpa.user"));
            commands.add("--db-password=" + System.getProperty("keycloak.map.storage.connectionsJpa.password"));
        }
    },
    HOTROD("hotrod") {
        @Override
        public void addStoreOptions(List<String> commands) {
            commands.add("--storage=" + getAlias());
            commands.add("--storage-hotrod-host=" + System.getProperty("keycloak.connectionsHotRod.host"));
            commands.add("--storage-hotrod-username=" + System.getProperty("keycloak.connectionsHotRod.username", "admin"));
            commands.add("--storage-hotrod-password=" + System.getProperty("keycloak.connectionsHotRod.password", "admin"));
        }
    },
    LEGACY("legacy") {
        @Override
        public void addStoreOptions(List<String> commands) {
            getDbVendor().ifPresent(vendor -> commands.add("--db=" + vendor));
            commands.add("--db-username=" + System.getProperty("keycloak.connectionsJpa.user"));
            commands.add("--db-password=" + System.getProperty("keycloak.connectionsJpa.password"));
            if ("mssql".equals(getDbVendor().orElse(null))){
                commands.add("--transaction-xa-enabled=false");
            }
            commands.add("--db-url=" + System.getProperty("keycloak.connectionsJpa.url"));
        }

        @Override
        public List<String> getStoreOptionsToKeycloakConfImport() {
            List<String> options = new ArrayList<>();
            getDbVendor().ifPresent(vendor -> options.add("db=" + vendor));
            options.add("db-url=" + System.getProperty("keycloak.connectionsJpa.url"));
            options.add("db-username=" + System.getProperty("keycloak.connectionsJpa.user"));
            options.add("db-password=" + System.getProperty("keycloak.connectionsJpa.password"));
            if ("mssql".equals(getDbVendor().orElse(null))){
                options.add("transaction-xa-enabled=false");
            }
            return options;
        }
    },
    DEFAULT("default") {
        @Override
        public void addStoreOptions(List<String> commands) {
            //nop
        }
    };

    public static final String AUTH_SERVER_QUARKUS_MAP_STORAGE_PROFILE = "auth.server.quarkus.mapStorage.profile.config";
    public static final String DB_VENDOR_PROPERTY = "keycloak.storage.connections.vendor";

    private final String alias;

    public abstract void addStoreOptions(List<String> commands);

    StoreProvider(String alias) {
        this.alias = alias;
    }

    /**
     * Add store options for the import command in migration tests. The options
     * will be added as lines in the <em>keycloak.conf</em> file.
     * @return The option lines to add
     */
    public List<String> getStoreOptionsToKeycloakConfImport() {
        return Collections.emptyList();
    }

    public String getAlias() {
        return alias;
    }

    public boolean isLegacyStore() {
        return this.equals(LEGACY);
    }

    public boolean isMapStore() {
        return !isLegacyStore() && !this.equals(DEFAULT);
    }

    public static Optional<String> getDbVendor() {
        return Optional.ofNullable(System.getProperty(DB_VENDOR_PROPERTY)).filter(StringUtil::isNotBlank);
    }

    public static StoreProvider getCurrentProvider() {
        return getProviderByAlias(System.getProperty(AUTH_SERVER_QUARKUS_MAP_STORAGE_PROFILE, ""));
    }

    /**
     * Get Store Provider by alias
     *
     * @param alias alias
     * @return store provider, LEGACY when vendor is specified, otherwise DEFAULT
     */
    public static StoreProvider getProviderByAlias(String alias) {
        return Arrays.stream(StoreProvider.values())
                .filter(f -> f.getAlias().equals(alias))
                .findFirst()
                .orElseGet(() -> getDbVendor().isEmpty() ? DEFAULT : LEGACY);
    }
}
