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

package org.keycloak.quarkus.runtime.configuration.mappers;

import static java.util.Optional.of;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.util.Optional;
import org.keycloak.config.StorageOptions;

import io.smallrye.config.ConfigSourceInterceptorContext;

final class StoragePropertyMappers {

    private StoragePropertyMappers(){}

    public static PropertyMapper[] getMappers() {
        return new PropertyMapper[] {
                fromOption(StorageOptions.STORAGE)
                        .to("kc.spi-map-storage-provider")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_EVENT_STORE)
                        .mapFrom("storage")
                        .to("kc.spi-events-store-provider")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_EVENT_ADMIN_STORE)
                        .mapFrom("storage")
                        .to("kc.spi-events-store-map-storage-admin-events-provider")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_EVENT_AUTH_STORE)
                        .mapFrom("storage")
                        .to("kc.spi-events-store-map-storage-auth-events-provider")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_REALM)
                        .to("kc.spi-realm-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_REALM_STORE)
                        .to("kc.spi-realm-map-storage-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_CLIENT)
                        .to("kc.spi-client-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_CLIENT_STORE)
                        .to("kc.spi-client-map-storage-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_CLIENT_SCOPE)
                        .to("kc.spi-client-scope-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_CLIENT_SCOPE_STORE)
                        .to("kc.spi-client-scope-map-storage-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_GROUP)
                        .to("kc.spi-group-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_GROUP_STORE)
                        .to("kc.spi-group-map-storage-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_ROLE)
                        .to("kc.spi-role-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_ROLE_STORE)
                        .to("kc.spi-role-map-storage-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_USER)
                        .to("kc.spi-user-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_USER_STORE)
                        .to("kc.spi-user-map-storage-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_DEPLOYMENT_STATE)
                        .to("kc.spi-deployment-state-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_AUTH_SESSION)
                        .to("kc.spi-authentication-sessions-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getCacheStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_AUTH_SESSION_STORE)
                        .to("kc.spi-authentication-sessions-map-storage-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_USER_SESSION)
                        .to("kc.spi-user-sessions-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getCacheStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_USER_SESSION_STORE)
                        .to("kc.spi-user-sessions-map-storage-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_LOGIN_FAILURE)
                        .to("kc.spi-login-failure-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getCacheStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_LOGIN_FAILURE_STORE)
                        .to("kc.spi-login-failure-map-storage-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_USER_SESSION_PERSISTER)
                        .to("kc.spi-user-session-persister-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getUserSessionPersisterStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_AUTHORIZATION_PERSISTER)
                        .to("kc.spi-authorization-persister-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_ACTION_TOKEN)
                        .to("kc.spi-action-token-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getCacheStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_ACTION_TOKEN_STORE)
                        .to("kc.spi-action-token-map-storage-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_DBLOCK)
                        .to("kc.spi-dblock-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getDbLockProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_CACHE_REALM_ENABLED)
                        .to("kc.spi-realm-cache-default-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isCacheAreaEnabledForStorage)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(StorageOptions.STORAGE_CACHE_AUTHORIZATION_ENABLED)
                        .to("kc.spi-authorization-cache-default-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isCacheAreaEnabledForStorage)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(StorageOptions.STORAGE_CACHE_USER_ENABLED)
                        .to("kc.spi-user-cache-default-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isCacheAreaEnabledForStorage)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(StorageOptions.STORAGE_SINGLE_USE_OBJECT)
                        .to("kc.spi-single-use-object-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getCacheStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_SINGLE_USE_OBJECT_STORE)
                        .to("kc.spi-single-use-object-map-storage-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_CACHE_COMPONENT_FACTORY)
                        .to("kc.spi-component-factory-default-caching-forced")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isForceComponentFactoryCache)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_PUBLIC_KEY_STORE)
                        .to("kc.spi-public-key-storage-map-storage-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_EXCEPTION_CONVERTER)
                        .to("kc.spi-exception-converter-jpa-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isLegacyStoreEnabled)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(StorageOptions.STORAGE_CACHE_ENABLED)
                        .to("kc.spi-connections-infinispan-default-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isLegacyStoreEnabled)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(StorageOptions.STORAGE_CACHE_CLUSTER_ENABLED)
                        .to("kc.spi-cluster-infinispan-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isLegacyStoreEnabled)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(StorageOptions.STORAGE_CACHE_STICK_SESSION_ENABLED)
                        .to("kc.spi-sticky-session-encoder-infinispan-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isLegacyStoreEnabled)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(StorageOptions.STORAGE_CACHE_CLEAR_REALM)
                        .to("kc.spi-admin-realm-restapi-extension-clear-realm-cache-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isLegacyStoreEnabled)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(StorageOptions.STORAGE_CACHE_CLEAR_USER)
                        .to("kc.spi-admin-realm-restapi-extension-clear-user-cache-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isLegacyStoreEnabled)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(StorageOptions.STORAGE_CACHE_CLEAR_KEYS)
                        .to("kc.spi-admin-realm-restapi-extension-clear-keys-cache-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isLegacyStoreEnabled)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(StorageOptions.STORAGE_LEGACY_SESSION_SUPPORT)
                        .to("kc.spi-legacy-session-support-default-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isLegacyStoreEnabled)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_USER_STORAGE)
                        .to("kc.spi-admin-realm-restapi-extension-user-storage-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isLegacyStoreEnabled)
                        .paramLabel("type")
                        .build()
        };
    }

    private static Optional<String> isForceComponentFactoryCache(Optional<String> storage, ConfigSourceInterceptorContext context) {
        if (storage.isPresent()) {
            return Optional.of(Boolean.TRUE.toString());
        }

        return storage;
    }

    private static Optional<String> getAreaStorage(Optional<String> storage, ConfigSourceInterceptorContext context) {
        return of(storage.isEmpty() ? "jpa" : "map");
    }

    private static Optional<String> getCacheStorage(Optional<String> storage, ConfigSourceInterceptorContext context) {
        return of(storage.isEmpty() ? "infinispan" : "map");
    }

    private static Optional<String> getDbLockProvider(Optional<String> storage, ConfigSourceInterceptorContext context) {
        return of(storage.isEmpty() ? "jpa" : "none");
    }

    private static Optional<String> getUserSessionPersisterStorage(Optional<String> storage, ConfigSourceInterceptorContext context) {
        return of(storage.isEmpty() ? "jpa" : "disabled");
    }

    private static Optional<String> isLegacyStoreEnabled(Optional<String> value, ConfigSourceInterceptorContext context) {
        if (value.isEmpty()) {
            return of(Boolean.TRUE.toString());
        }

        return of(Boolean.FALSE.toString());
    }

    private static Optional<String> resolveMapStorageProvider(Optional<String> value, ConfigSourceInterceptorContext context) {
        try {
            if (value.isPresent()) {
                return of(value.map(StorageOptions.StorageType::valueOf).map(StorageOptions.StorageType::getProvider)
                        .orElse(StorageOptions.StorageType.chm.getProvider()));
            }
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Invalid storage provider: " + value.orElse(null), iae);
        }

        return value;
    }

    private static Optional<String> isCacheAreaEnabledForStorage(Optional<String> storage, ConfigSourceInterceptorContext context) {
        return of(storage.isEmpty() ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
    }
}
