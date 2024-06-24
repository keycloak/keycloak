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

package org.keycloak.models.sessions.infinispan;

/**
 * An updated interface for Infinispan cache.
 * <p>
 * When the entity is changed, the new entity must be written (or removed) into the Infinispan cache.
 * The methods {@link #onEntityUpdated()} and {@link #onEntityRemoved()} signals the entity has changed.
 *
 * @param <T> The entity type.
 */
public interface SessionEntityUpdater<T> {

    /**
     * @return The entity tracked by this {@link SessionEntityUpdater}.
     * It does not fetch the value from the Infinispan cache and uses a local copy.
     */
    T getEntity();

    /**
     * Signals that the entity was updated, and the Infinispan cache needs to be updated.
     */
    void onEntityUpdated();

    /**
     * Signals that the entity was removed, and the Infinispan cache needs to be updated.
     */
    void onEntityRemoved();

}
