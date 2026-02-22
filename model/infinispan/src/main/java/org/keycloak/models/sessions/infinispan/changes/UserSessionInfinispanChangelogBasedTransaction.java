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

package org.keycloak.models.sessions.infinispan.changes;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;

/**
 * User session transaction implementation that optimizes Infinispan cache expiration settings.
 * <p>
 * This class overrides the parent's timeout computation to disable max-idle tracking, which is expensive for Infinispan
 * to maintain. Instead, the lifespan is adjusted to be the minimum of the original lifespan and max-idle values,
 * ensuring sessions still expire at the correct time without the overhead of idle time tracking.
 */
public class UserSessionInfinispanChangelogBasedTransaction<K, V extends SessionEntity> extends InfinispanChangelogBasedTransaction<K, V> {
    public UserSessionInfinispanChangelogBasedTransaction(KeycloakSession kcSession, CacheHolder<K, V> cacheHolder) {
        super(kcSession, cacheHolder);
    }

    @Override
    protected long computeLifespan(long maxIdle, long lifespan) {
        return SessionTimeouts.calculateEffectiveSessionLifespan(maxIdle, lifespan);
    }

    @Override
    protected long computeMaxIdle(long maxIdle, long lifespan) {
        return SessionTimeouts.IMMORTAL_FLAG;
    }
}
