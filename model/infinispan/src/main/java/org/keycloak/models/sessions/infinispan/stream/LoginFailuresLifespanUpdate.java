/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.stream;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

import static org.keycloak.marshalling.Marshalling.LOGIN_FAILURES_LIFESPAN_UPDATE;

/**
 * A {@link BiConsumer} that updates the lifespan of login failure cache entries based on realm lockout policies.
 * <p>
 * This class is used to recalculate and update the time-to-live (TTL) for login failure records stored in the
 * Infinispan cache. The lifespan is determined by the realm's brute force protection settings, including whether
 * permanent lockout is enabled and the maximum number of temporary lockouts allowed.
 * <p>
 * The class is serializable via Infinispan ProtoStream to support distributed cache operations in remote caches.
 */
@ProtoTypeId(LOGIN_FAILURES_LIFESPAN_UPDATE)
public class LoginFailuresLifespanUpdate implements BiConsumer<Cache<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>>, Map.Entry<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>>> {

    @ProtoField(1)
    final long maxDeltaTimeMillis;
    @ProtoField(2)
    final int maxTemporaryLockouts;
    @ProtoField(3)
    final boolean permanentLockout;

    /**
     * Creates a new login failures lifespan update operation with the specified lockout policy parameters.
     * <p>
     * This constructor is annotated with {@link ProtoFactory} to enable Infinispan ProtoStream serialization for remote
     * cache operations.
     *
     * @param maxDeltaTimeMillis   The maximum time window in milliseconds for tracking failures
     * @param maxTemporaryLockouts The maximum number of temporary lockouts allowed
     * @param permanentLockout     Whether permanent lockout is enabled
     */
    @ProtoFactory
    public LoginFailuresLifespanUpdate(long maxDeltaTimeMillis, int maxTemporaryLockouts, boolean permanentLockout) {
        this.maxDeltaTimeMillis = maxDeltaTimeMillis;
        this.maxTemporaryLockouts = maxTemporaryLockouts;
        this.permanentLockout = permanentLockout;
    }

    /**
     * Updates the lifespan of a login failure cache entry based on the configured lockout policy.
     * <p>
     * The new lifespan is calculated using {@link SessionTimeouts#getLoginFailuresLifespanMs} which considers the
     * current failure count, permanent lockout settings, and maximum delta time. The cache entry is updated using flags
     * that optimize performance by avoiding locks and ignoring return values.
     *
     * @param cache The Infinispan cache containing login failure entries
     * @param entry The cache entry to update with its key and wrapped login failure entity
     */
    @Override
    public void accept(Cache<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> cache, Map.Entry<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> entry) {
        var entity = entry.getValue().getEntity();
        long lifespan = SessionTimeouts.getLoginFailuresLifespanMs(permanentLockout, maxTemporaryLockouts, maxDeltaTimeMillis, entity);
        cache.getAdvancedCache()
                .withFlags(Flag.ZERO_LOCK_ACQUISITION_TIMEOUT, Flag.FAIL_SILENTLY, Flag.IGNORE_RETURN_VALUES)
                .computeIfPresent(entry.getKey(), ValueIdentityBiFunction.getInstance(), lifespan, TimeUnit.MILLISECONDS);
    }
}
