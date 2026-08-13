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

package org.keycloak.connections.jpa;

/**
 * Marker interface for JPA entities that can tolerate asynchronous commit.
 * <p>
 * When a transaction only modifies entities that implement this interface (and whose
 * {@link #isAsyncCommitAllowed(EntityOperationType)} returns {@code true} for the
 * respective operation). See {@link AsyncCommitIntegrator} for details.
 * This is currently only supported for PostgreSQL databases.
 * <p>
 * Entities that do NOT implement this interface are considered "important" — any modification
 * to them forces synchronous commit for the entire transaction.
 *
 * @author Alexander Schwartz
 */
public interface AsynchronousCommitAllowed {

    enum EntityOperationType {
        INSERT, UPDATE, DELETE
    }

    /**
     * Whether this entity allows asynchronous commit for the given operation type.
     * <p>
     * Returning {@code false} for any operation that occurs during a transaction
     * will force synchronous commit for the entire transaction.
     *
     * @param operationType the type of database operation being performed
     * @return {@code true} if the operation can tolerate asynchronous commit
     */
    default boolean isAsyncCommitAllowed(EntityOperationType operationType) {
        return true;
    }

}
