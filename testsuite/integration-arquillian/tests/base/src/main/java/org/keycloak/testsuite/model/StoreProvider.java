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

import java.util.Arrays;
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
    JPA("jpa") {
        @Override
        public void addStoreOptions(List<String> commands) {
            commands.add("--storage=" + getAlias());
            getDbVendor().ifPresent(vendor -> commands.add("--db=" + vendor));
            commands.add("--db-url='" + System.getProperty("keycloak.map.storage.connectionsJpa.url") + "'");
            commands.add("--db-username=" + System.getProperty("keycloak.map.storage.connectionsJpa.user"));
            commands.add("--db-password=" + System.getProperty("keycloak.map.storage.connectionsJpa.password"));
        }
    },
    HOTROD("hotrod") {
        @Override
        public void addStoreOptions(List<String> commands) {
            commands.add("--storage=" + getAlias());
            commands.add("--storage-hotrod-host='" + System.getProperty("keycloak.connectionsHotRod.host") + "'");
            commands.add("--storage-hotrod-username" + System.getProperty("keycloak.connectionsHotRod.username"));
            commands.add("--storage-hotrod-password" + System.getProperty("keycloak.connectionsHotRod.password"));
        }
    },
    LEGACY("legacy") {
        @Override
        public void addStoreOptions(List<String> commands) {
            getDbVendor().ifPresent(vendor -> commands.add("--db=" + vendor));
            commands.add("--db-url='" + System.getProperty("keycloak.connectionsJpa.url") + "'");
            commands.add("--db-username=" + System.getProperty("keycloak.connectionsJpa.user"));
            commands.add("--db-password=" + System.getProperty("keycloak.connectionsJpa.password"));
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
