/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.jpa.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.models.utils.SessionTimeoutHelper;

/**
 * Remembers, per realm and session type, the time at which the realm was found without any user session.
 * <p>
 * A session created after that time cannot expire before the observation time plus the shortest idle timeout, so the
 * expiration can be skipped until then. Only the raw observation time is stored; the deadline is derived from the
 * current realm settings on every check, so setting changes need no invalidation.
 * <p>
 * The state is local to the node, which is fine as the expiration of a realm is performed by a single node at a time.
 * A node without state simply performs the regular check.
 */
final class EmptyRealmWatermarks {

    private final Map<String, Integer> observedAt = new ConcurrentHashMap<>();

    /**
     * @return {@code true} if the realm was found empty and no session can be expired yet.
     */
    boolean canSkip(String realmId, boolean offline, int currentTime, int minIdle) {
        Integer observed = observedAt.get(key(realmId, offline));
        // an observation in the future happens if the clock moved backwards, for example, a time offset in tests
        return observed != null && observed <= currentTime && currentTime < deadline(observed, minIdle);
    }

    void markEmpty(String realmId, boolean offline, int currentTime) {
        observedAt.put(key(realmId, offline), currentTime);
    }

    void clear(String realmId, boolean offline) {
        observedAt.remove(key(realmId, offline));
    }

    private static long deadline(int observed, int minIdle) {
        return (long) observed + minIdle + SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;
    }

    private static String key(String realmId, boolean offline) {
        return realmId + (offline ? "/1" : "/0");
    }
}
