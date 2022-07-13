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

package org.keycloak.config;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StorageOptions {

    public enum StorageType {

        chm("concurrenthashmap");

        private final String provider;

        StorageType(String provider) {
            this.provider = provider;
        }

        public String getProvider() {
            return provider;
        }
    }

    public static final Option<StorageType> STORAGE = new OptionBuilder<>("storage", StorageType.class)
            .category(OptionCategory.STORAGE)
            .description(String.format("Sets a storage mechanism. Possible values are: %s.",
                    String.join(",", String.join(", ", Arrays.stream(StorageType.values()).map(StorageType::name).collect(Collectors.toList())))))
            .expectedValues(StorageType.values())
            .defaultValue(Optional.empty())
            .buildTime(true)
            .build();

    public static final Option<StorageType> STORAGE_PROVIDER = new OptionBuilder<>("storage-provider", StorageType.class)
            .category(OptionCategory.STORAGE)
            .buildTime(true)
            .build();

    public static final Option<StorageType> STORAGE_EVENT_STORE = new OptionBuilder<>("storage-event-store", StorageType.class)
            .category(OptionCategory.STORAGE)
            .buildTime(true)
            .build();

    public static final Option<StorageType> STORAGE_EVENT_ADMIN_STORE = new OptionBuilder<>("storage-event-admin", StorageType.class)
            .category(OptionCategory.STORAGE)
            .buildTime(true)
            .build();

    public static final Option<StorageType> STORAGE_EVENT_AUTH_STORE = new OptionBuilder<>("storage-event-auth", StorageType.class)
            .category(OptionCategory.STORAGE)
            .buildTime(true)
            .build();

    public static final Option<StorageType> STORAGE_EXCEPTION_CONVERTER = new OptionBuilder<>("storage-exception-converter", StorageType.class)
            .category(OptionCategory.STORAGE)
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_REALM = new OptionBuilder<>("storage-realm", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_REALM_STORE = new OptionBuilder<>("storage-realm-store", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_CLIENT = new OptionBuilder<>("storage-client", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_CLIENT_STORE = new OptionBuilder<>("storage-client-store", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_CLIENT_SCOPE = new OptionBuilder<>("storage-client-scope", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_CLIENT_SCOPE_STORE = new OptionBuilder<>("storage-client-scope-store", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_GROUP = new OptionBuilder<>("storage-group", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_GROUP_STORE = new OptionBuilder<>("storage-group-store", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_ROLE = new OptionBuilder<>("storage-role", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_ROLE_STORE = new OptionBuilder<>("storage-role-store", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_USER = new OptionBuilder<>("storage-user", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_USER_STORE = new OptionBuilder<>("storage-user-store", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_DEPLOYMENT_STATE = new OptionBuilder<>("storage-deployment-state", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_AUTH_SESSION = new OptionBuilder<>("storage-auth-session", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_AUTH_SESSION_STORE = new OptionBuilder<>("storage-auth-session-store", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_USER_SESSION = new OptionBuilder<>("storage-user-session", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_USER_SESSION_STORE = new OptionBuilder<>("storage-user-session-store", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_LOGIN_FAILURE = new OptionBuilder<>("storage-login-failure", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_LOGIN_FAILURE_STORE = new OptionBuilder<>("storage-login-failure-store", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_AUTHORIZATION_PERSISTER = new OptionBuilder<>("storage-authorization-persister", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_USER_SESSION_PERSISTER = new OptionBuilder<>("storage-user-session-persister", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_ACTION_TOKEN = new OptionBuilder<>("storage-action-token", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_ACTION_TOKEN_STORE = new OptionBuilder<>("storage-action-token-store", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_DBLOCK = new OptionBuilder<>("storage-dblock", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<Boolean> STORAGE_CACHE_ENABLED = new OptionBuilder<>("cache-enabled", Boolean.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<Boolean> STORAGE_CACHE_CLUSTER_ENABLED = new OptionBuilder<>("cache-cluster-enabled", Boolean.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_CACHE_STICK_SESSION_ENABLED = new OptionBuilder<>("cache-stick-session-enabled", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_CACHE_REALM_ENABLED = new OptionBuilder<>("cache-realm-enabled", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_CACHE_USER_ENABLED = new OptionBuilder<>("cache-user-enabled", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_CACHE_CLEAR_USER = new OptionBuilder<>("cache-clear-user", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_CACHE_CLEAR_REALM = new OptionBuilder<>("cache-clear-realm", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_CACHE_CLEAR_KEYS = new OptionBuilder<>("cache-clear-keys", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_SINGLE_USE_OBJECT = new OptionBuilder<>("storage-single-use-object", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_SINGLE_USE_OBJECT_STORE = new OptionBuilder<>("storage-single-use-object-store", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_PUBLIC_KEY_STORE = new OptionBuilder<>("storage-public-key-store", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_CACHE_AUTHORIZATION_ENABLED = new OptionBuilder<>("cache-authorization-enabled", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_CACHE_COMPONENT_FACTORY = new OptionBuilder<>("cache-component-factory-cache", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_LEGACY_SESSION_SUPPORT = new OptionBuilder<>("storage-legacy-session-support", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> STORAGE_USER_STORAGE = new OptionBuilder<>("storage-user-storage", String.class)
            .category(OptionCategory.STORAGE)
            .hidden()
            .buildTime(true)
            .build();

    public static final List<Option<?>> ALL_OPTIONS = List.of(STORAGE);
}
