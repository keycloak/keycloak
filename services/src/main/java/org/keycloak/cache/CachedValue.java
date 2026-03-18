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

package org.keycloak.cache;

import java.util.Objects;

/**
 * Internal representation of cached lookup values.
 * <p>
 * This is an implementation detail used by {@link DefaultAlternativeLookupProvider} to cache alternative lookups and
 * reduce database load. The interface provides type-safe wrappers for different kinds of cached values used in the
 * lookup cache.
 */
interface CachedValue {

    /**
     * Creates a cached value wrapping a simple identifier string.
     *
     * @param value the non-null identifier value to cache
     * @return a cached string value
     */
    static CachedString ofId(String value) {
        return new CachedString(value);
    }

    /**
     * Creates a cached value for a client role lookup.
     *
     * @param clientId the non-null client identifier
     * @param roleName the non-null role name
     * @return a cached role qualifier for a client role
     */
    static CachedRoleQualifier ofClientRole(String clientId, String roleName) {
        return new CachedRoleQualifier(Objects.requireNonNull(clientId), roleName);
    }

    /**
     * Creates a cached value for a realm role lookup.
     *
     * @param roleName the non-null role name
     * @return a cached role qualifier for a realm role
     */
    static CachedRoleQualifier ofRealmRole(String roleName) {
        return new CachedRoleQualifier(null, roleName);
    }

    /**
     * A cached value wrapping a simple string identifier.
     *
     * @param value the non-null identifier value
     */
    record CachedString(String value) implements CachedValue {
        public CachedString {
            Objects.requireNonNull(value);
        }
    }

    /**
     * A cached value wrapping role qualifier information.
     * <p>
     * For client roles, both {@code clientId} and {@code roleName} are present. For realm roles, {@code clientId} is
     * {@code null}.
     *
     * @param clientId the client identifier, or {@code null} for realm roles
     * @param roleName the non-null role name
     */
    record CachedRoleQualifier(String clientId, String roleName) implements CachedValue {
        public CachedRoleQualifier {
            Objects.requireNonNull(roleName);
        }

        /**
         * Checks if this qualifier represents a realm role.
         *
         * @return {@code true} if this is a realm role (clientId is null), {@code false} otherwise
         */
        public boolean isRealmRole() {
            return clientId == null;
        }
    }
}
