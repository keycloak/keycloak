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

import org.jboss.logging.Logger;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.config.StorageOptions;
import org.keycloak.config.StorageOptions.StorageType;

import io.smallrye.config.ConfigSourceInterceptorContext;

final class StoragePropertyMappers {

    private StoragePropertyMappers(){}

    public static PropertyMapper[] getMappers() {
        return new PropertyMapper[] {
                fromOption(StorageOptions.STORAGE)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_PROVIDER)
                        .to("kc.spi-map-storage-provider")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_EVENT_STORE_PROVIDER)
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
                fromOption(StorageOptions.STORAGE_REALM_PROVIDER)
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
                fromOption(StorageOptions.STORAGE_CLIENT_PROVIDER)
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
                fromOption(StorageOptions.STORAGE_CLIENT_SCOPE_PROVIDER)
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
                fromOption(StorageOptions.STORAGE_GROUP_PROVIDER)
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
                fromOption(StorageOptions.STORAGE_ROLE_PROVIDER)
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
                fromOption(StorageOptions.STORAGE_USER_PROVIDER)
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
                fromOption(StorageOptions.STORAGE_DEPLOYMENT_STATE_PROVIDER)
                        .to("kc.spi-deployment-state-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_DEPLOYMENT_STATE_RESOURCES_VERSION_SEED)
                        .to("kc.spi-deployment-state-map-resources-version-seed")
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_AUTH_SESSION_PROVIDER)
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
                fromOption(StorageOptions.STORAGE_USER_SESSION_PROVIDER)
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
                fromOption(StorageOptions.STORAGE_LOGIN_FAILURE_PROVIDER)
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
                fromOption(StorageOptions.STORAGE_AUTHORIZATION_PROVIDER)
                        .to("kc.spi-authorization-persister-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_AUTHORIZATION_STORE)
                        .to("kc.spi-authorization-persister-map-storage-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::resolveMapStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_ACTION_TOKEN_PROVIDER)
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
                fromOption(StorageOptions.STORAGE_SINGLE_USE_OBJECT_PROVIDER)
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
                fromOption(StorageOptions.STORAGE_PUBLIC_KEY_STORAGE_STORE)
                        .to("kc.spi-public-key-storage-map-storage-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::resolveMapStorageProviderPublicKeyStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_EXCEPTION_CONVERTER)
                        .to("kc.spi-exception-converter-jpa-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isLegacyStoreEnabled)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(StorageOptions.STORAGE_ADMIN_CACHE_CLEAR_REALM)
                        .to("kc.spi-admin-realm-restapi-extension-clear-realm-cache-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isLegacyStoreEnabled)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(StorageOptions.STORAGE_ADMIN_CACHE_CLEAR_USER)
                        .to("kc.spi-admin-realm-restapi-extension-clear-user-cache-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isLegacyStoreEnabled)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(StorageOptions.STORAGE_ADMIN_CACHE_CLEAR_KEYS)
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
                fromOption(StorageOptions.STORAGE_ADMIN_USER_STORAGE)
                        .to("kc.spi-admin-realm-restapi-extension-user-storage-enabled")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::isLegacyStoreEnabled)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_HOTROD_HOST)
                        .to("kc.spi-connections-hot-rod-default-host")
                        .paramLabel("host")
                        .build(),
                fromOption(StorageOptions.STORAGE_HOTROD_PORT)
                        .to("kc.spi-connections-hot-rod-default-port")
                        .paramLabel("port")
                        .build(),
                fromOption(StorageOptions.STORAGE_HOTROD_USERNAME)
                        .to("kc.spi-connections-hot-rod-default-username")
                        .paramLabel("username")
                        .build(),
                fromOption(StorageOptions.STORAGE_HOTROD_PASSWORD)
                        .to("kc.spi-connections-hot-rod-default-password")
                        .paramLabel("password")
                        .build(),
                fromOption(StorageOptions.STORAGE_HOTROD_CACHE_CONFIGURE)
                        .to("kc.spi-connections-hot-rod-default-configure-remote-caches")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(StorageOptions.STORAGE_HOTROD_CACHE_REINDEX)
                        .to("kc.spi-connections-hot-rod-default-reindex-caches")
                        .paramLabel("[cache1,cache2,...]|all")
                        .build()
        };
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
                return of(value.map(StorageType::valueOf).map(StorageType::getProvider)
                        .orElse(StorageType.chm.getProvider()));
            }
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Invalid storage provider: " + value.orElse(null), iae);
        }

        return value;
    }

    private static Optional<String> resolveMapStorageProviderPublicKeyStorage(Optional<String> value, ConfigSourceInterceptorContext context) {
        try {
            if (value.isPresent()) {
                // there is only one public key storage provider available
                return of(StorageType.chm.getProvider());
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
