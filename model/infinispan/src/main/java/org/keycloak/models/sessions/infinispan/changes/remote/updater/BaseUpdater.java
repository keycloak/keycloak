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

/**
 * Base functionality of an {@link Updater} implementation.
 * <p>
 * It stores the Infinispan cache key, value, version, and it states. However, it does not keep track of the changed
 * fields in the cache value, and it is the responsibility of the implementation to do that.
 * <p>
 * The method {@link #onFieldChanged()} must be invoked to track changes in the cache value.
 *
 * @param <K> The type of the Infinispan cache key.
 * @param <V> The type of the Infinispan cache value.
 */
public abstract class BaseUpdater<K, V> implements Updater<K, V> {

    private final K cacheKey;
    private final V cacheValue;
    private final long versionRead;
    private UpdaterState state;

    protected BaseUpdater(K cacheKey, V cacheValue, long versionRead, UpdaterState state) {
        this.cacheKey = cacheKey;
        this.cacheValue = cacheValue;
        this.versionRead = versionRead;
        this.state = state;
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
        return state == UpdaterState.DELETED;
    }

    @Override
    public final boolean isCreated() {
        return state == UpdaterState.CREATED;
    }

    @Override
    public final boolean isReadOnly() {
        return state == UpdaterState.READ_ONLY;
    }

    @Override
    public final void markDeleted() {
        state = UpdaterState.DELETED;
    }

    /**
     * Must be invoked when a field change to mark this updated and modified.
     */
    protected final void onFieldChanged() {
        state = state.stateAfterChange();
    }

    protected enum UpdaterState {
        /**
         * The cache value is created. It implies {@link #MODIFIED}.
         */
        CREATED,
        /**
         * The cache value is deleted, and it will be removed from the Infinispan cache. It cannot be recreated.
         */
        DELETED,
        /**
         * The cache value was read the Infinispan cache and was not modified.
         */
        READ_ONLY {
            @Override
            UpdaterState stateAfterChange() {
                return MODIFIED;
            }
        },
        /**
         * The cache value was read from the Infinispan cache and was modified. Changes will be merged into the current
         * Infinispan cache value.
         */
        MODIFIED;

        UpdaterState stateAfterChange() {
            return this;
        }

    }
}
