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

package org.keycloak.models.map.common;

/**
 * This interface provides a way for marking entities that can expire. For example, user sessions are valid only
 * for certain amount of time. After that time the entities can be removed from storage/omitted from query results.
 *
 * Presence of expired entities in the storage should be transparent to layers above the physical one. This can be
 * achieved in more ways. Ideal solution is when expired entities never reach Keycloak codebase, however, this may
 * not be possible for all storage implementations, therefore, we need to double-check entities validity before they
 * reach logical layer, for example, before we turn entity into model.
 *
 * Implementation of actual removal of the entities from the storage is responsibility of each storage individually.
 *
 */
public interface ExpirableEntity extends AbstractEntity {

    /**
     * Returns a point in the time (timestamp in milliseconds since The Epoch) when this entity expires.
     *
     * @return a timestamp in milliseconds since The Epoch or {@code null} if this entity never expires
     *         or expiration is not known.
     */
    Long getExpiration();

    /**
     * Sets a point in the time (timestamp in milliseconds since The Epoch) when this entity expires.
     *
     * @param expiration a timestamp in milliseconds since The Epoch or {@code null} if this entity never expires.
     */
    void setExpiration(Long expiration);
}
