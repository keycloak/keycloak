/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.configuration;

import io.smallrye.config.ConfigValue;
import org.keycloak.common.crypto.FipsMode;
import org.keycloak.config.ClassLoaderOptions;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.HealthOptions;
import org.keycloak.config.MetricsOptions;
import org.keycloak.config.SecurityOptions;
import org.keycloak.config.StorageOptions;
import org.keycloak.config.database.Database;

import java.util.Optional;
import java.util.Properties;

import static org.keycloak.config.StorageOptions.STORAGE;
import static org.keycloak.config.StorageOptions.STORAGE_JPA_DB;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getRawPersistedProperty;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

/**
 * Most frequently used Keycloak configuration properties
 */
public class KeycloakConfiguration {
    private static final String KC_OPTIMIZED = NS_KEYCLOAK_PREFIX + "optimized";

    public static Optional<String> getKcOptionalValue(String propertyName) {
        return Configuration.getOptionalValue(NS_KEYCLOAK_PREFIX.concat(propertyName));
    }

    public static boolean isOptimized() {
        return getRawPersistedProperty(KC_OPTIMIZED).isPresent();
    }

    public static void markAsOptimized(Properties properties) {
        properties.put(KC_OPTIMIZED, Boolean.TRUE.toString());
    }

    public static boolean isMetricsEnabled() {
        return getKcBooleanValue(MetricsOptions.METRICS_ENABLED.getKey());
    }

    public static boolean isHealthEnabled() {
        return getKcBooleanValue(HealthOptions.HEALTH_ENABLED.getKey());
    }

    public static Optional<FipsMode> getFipsMode() {
        return getKcOptionalValue(SecurityOptions.FIPS_MODE.getKey()).map(FipsMode::valueOfOption);
    }

    public static Optional<String> getIgnoredArtifacts() {
        return getKcCurrentBuiltTimeProperty(ClassLoaderOptions.IGNORE_ARTIFACTS.getKey());
    }

    /* DB */
    public static boolean isLegacyJpa() {
        return getKcBooleanValue(STORAGE.getKey());
    }

    public static boolean isMapJpa() {
        Optional<String> storage = getStorage();
        return storage.isPresent() && storage.get().equals(StorageOptions.StorageType.jpa.name());
    }

    public static boolean isJpa() {
        // JPA legacy or JPA map
        return isLegacyJpa() || isMapJpa();
    }

    public static Optional<Database.Vendor> getJpaDbVendor() {
        return getKcOptionalValue(DatabaseOptions.DB.getKey()).flatMap(StorageOptions::getDatabaseVendor);
    }

    public static Optional<Database.Vendor> getMapJpaDbVendor() {
        return getKcOptionalValue(STORAGE_JPA_DB.getKey()).flatMap(StorageOptions::getDatabaseVendor);
    }

    public static Optional<String> getStorage() {
        return getKcOptionalValue(STORAGE.getKey());
    }

    public static Optional<String> getDbDialect() {
        return getKcOptionalValue(DatabaseOptions.DB_DIALECT.getKey());
    }

    public static Optional<String> getDbSchema() {
        return getKcOptionalValue(DatabaseOptions.DB_SCHEMA.getKey());
    }

    /* Internal */
    private static boolean getKcBooleanValue(String propertyName) {
        return Configuration.getOptionalBooleanValue(NS_KEYCLOAK_PREFIX.concat(propertyName)).orElse(false);
    }

    private static Optional<String> getKcCurrentBuiltTimeProperty(String propertyName) {
        return Optional.ofNullable(Configuration.getCurrentBuiltTimeProperty(NS_KEYCLOAK_PREFIX.concat(propertyName))).map(ConfigValue::getValue);
    }
}
