/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.infinispan.util;

import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.sessions.infinispan.changes.remote.RemoveEntryPredicate;

import static org.keycloak.common.Profile.Feature.MULTI_SITE;
import static org.keycloak.common.Profile.Feature.REMOTE_CACHE;

public final class InfinispanUtils {

    private static final RemoveEntryPredicate<?,?> ALWAYS_FALSE = (key, value) -> false;

    private InfinispanUtils() {
    }

    // all providers have the same order
    public static final int PROVIDER_ORDER = 1;

    // provider id for embedded cache providers
    public static final String EMBEDDED_PROVIDER_ID = "infinispan";

    // provider id for remote cache providers
    public static final String REMOTE_PROVIDER_ID = "remote";

    // true if running with external infinispan mode only
    public static boolean isRemoteInfinispan() {
        return Profile.isFeatureEnabled(Feature.MULTI_SITE) && Profile.isFeatureEnabled(REMOTE_CACHE);
    }

    // true if running with embedded caches.
    public static boolean isEmbeddedInfinispan() {
        return !Profile.isFeatureEnabled(MULTI_SITE) || !Profile.isFeatureEnabled(REMOTE_CACHE);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> RemoveEntryPredicate<K, V> alwaysFalse() {
        return (RemoveEntryPredicate<K, V>) ALWAYS_FALSE;
    }

    public static boolean isNotOfflineSessionCache(String name) {
        return !InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME.equals(name) &&
                !InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME.equals(name);
    }
}
