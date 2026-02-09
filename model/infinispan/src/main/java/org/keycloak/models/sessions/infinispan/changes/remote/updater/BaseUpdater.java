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
package org.keycloak.models.sessions.infinispan.changes.remote.updater;

import java.util.Objects;

/**
 * Base functionality of an {@link Updater} implementation.
 * <p>
 * It stores the Infinispan cache key, value, version, and it states. However, it does not keep track of the changed
 * fields in the cache value, and it is the responsibility of the implementation to do that.
 * <p>
 * Implement the method {@link #isUnchanged()} to signal if the entity was modified or not.
 *
 * @param <K> The type of the Infinispan cache key.
 * @param <V> The type of the Infinispan cache value.
 */
public abstract class BaseUpdater<K, V> implements Updater<K, V> {

    private final K cacheKey;
    private final V cacheValue;
    private final long versionRead;
    private final UpdaterState initialState;
    private UpdaterState state;

    protected BaseUpdater(K cacheKey, V cacheValue, long versionRead, UpdaterState state) {
        this.cacheKey = Objects.requireNonNull(cacheKey);
        this.cacheValue = cacheValue;
        this.versionRead = versionRead;
        this.state = Objects.requireNonNull(state);
        this.initialState = state;
    }

    @Override
    public final K getKey() {
        return cacheKey;
    }

    @Override
    public final V getValue() {
        return cacheValue;
    }

    @Override
    public final long getVersionRead() {
        return versionRead;
    }

    @Override
    public final boolean isDeleted() {
        return state == UpdaterState.DELETED || state == UpdaterState.DELETED_TRANSIENT;
    }

    @Override
    public final boolean isCreated() {
        return state == UpdaterState.CREATED;
    }

    @Override
    public final boolean isReadOnly() {
        return state == UpdaterState.READ && isUnchanged();
    }

    @Override
    public final boolean isExpired() {
        return state == UpdaterState.EXPIRED;
    }

    @Override
    public final void markDeleted() {
        state = switch (state) {
            case READ, DELETED -> UpdaterState.DELETED;
            case CREATED, DELETED_TRANSIENT, EXPIRED -> UpdaterState.DELETED_TRANSIENT;
        };
    }

    @Override
    public void markExpired() {
        state = UpdaterState.EXPIRED;
    }

    @Override
    public boolean isTransient() {
        return state == UpdaterState.DELETED_TRANSIENT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseUpdater<?, ?> that = (BaseUpdater<?, ?>) o;
        return cacheKey.equals(that.cacheKey);
    }

    @Override
    public int hashCode() {
        return cacheKey.hashCode();
    }

    @Override
    public String toString() {
        return "BaseUpdater{" +
                "cacheKey=" + cacheKey +
                ", cacheValue=" + cacheValue +
                ", state=" + state +
                ", versionRead=" + versionRead +
                '}';
    }

    /**
     * Resets the {@link UpdaterState} to its initial value.
     */
    protected final void resetState() {
        state = initialState;
    }

    /**
     * @return {@code true} if the entity was changed after being created/read.
     */
    protected abstract boolean isUnchanged();

    protected enum UpdaterState {
        /**
         * The cache value is created.
         */
        CREATED,
        /**
         * The cache value is deleted, and it will be removed from the Infinispan cache. It cannot be recreated.
         */
        DELETED,
        /**
         * The cache value was read from the Infinispan cache.
         */
        READ,
        /**
         * The entity is transient (it won't be updated in the external infinispan cluster) and deleted.
         */
        DELETED_TRANSIENT,
        /**
         * The entity is expired (max-idle or lifespan). No changes should be applied.
         */
        EXPIRED,

    }
}
