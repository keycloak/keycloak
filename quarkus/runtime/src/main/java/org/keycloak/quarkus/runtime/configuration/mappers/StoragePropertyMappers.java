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
                fromOption(StorageOptions.STORAGE_LEGACY_ENABLED)
                        .to("kc.spi-connections-jpa-legacy-enabled")
                        .mapFrom("storage")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .transformer(StoragePropertyMappers::isDefaultPersistenceUnitEnabled)
                        .build(),
                fromOption(StorageOptions.STORAGE)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_PROVIDER)
                        .mapFrom("storage")
                        .to("kc.spi-map-storage-provider")
                        .transformer(StoragePropertyMappers::resolveStorageProvider)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_REALM)
                        .to("kc.spi-realm-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_CLIENT)
                        .to("kc.spi-client-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_CLIENT_SCOPE)
                        .to("kc.spi-client-scope-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_GROUP)
                        .to("kc.spi-group-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_ROLE)
                        .to("kc.spi-role-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_USER)
                        .to("kc.spi-user-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getAreaStorage)
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
                fromOption(StorageOptions.STORAGE_USER_SESSION)
                        .to("kc.spi-user-sessions-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getCacheStorage)
                        .paramLabel("type")
                        .build(),
                fromOption(StorageOptions.STORAGE_LOGIN_FAILURE)
                        .to("kc.spi-login-failure-provider")
                        .mapFrom("storage")
                        .transformer(StoragePropertyMappers::getCacheStorage)
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
        };
    }

    private static Optional<String> getAreaStorage(Optional<String> storage, ConfigSourceInterceptorContext context) {
        return of("legacy".equals(storage.orElse(null)) ? "jpa" : "map");
    }

    private static Optional<String> getCacheStorage(Optional<String> storage, ConfigSourceInterceptorContext context) {
        return of("legacy".equals(storage.orElse(null)) ? "infinispan" : "map");
    }

    private static Optional<String> getDbLockProvider(Optional<String> storage, ConfigSourceInterceptorContext context) {
        return of("legacy".equals(storage.orElse(null)) ? "jpa" : "none");
    }

    private static Optional<String> getUserSessionPersisterStorage(Optional<String> storage, ConfigSourceInterceptorContext context) {
        return of("legacy".equals(storage.orElse(null)) ? "jpa" : "disabled");
    }

    private static Optional<String> isDefaultPersistenceUnitEnabled(Optional<String> value, ConfigSourceInterceptorContext context) {
        if (value.get().equals(StorageOptions.StorageType.legacy.name())) {
            return of(Boolean.TRUE.toString());
        }

        return of(Boolean.valueOf(value.get()).toString());
    }

    private static Optional<String> resolveStorageProvider(Optional<String> value, ConfigSourceInterceptorContext context) {
        return Optional.ofNullable("legacy".equals(value.orElse(null)) ? null : "concurrenthashmap");
    }

    private static Optional<String> isCacheAreaEnabledForStorage(Optional<String> storage, ConfigSourceInterceptorContext context) {
        return of("legacy".equals(storage.orElse(null)) ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
    }
}
