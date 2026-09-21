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

package org.keycloak.expiration.jpa;

import java.util.function.IntConsumer;

import org.keycloak.models.KeycloakSession;

/**
 * A callback that removes expired entries from the database.
 * <p>
 * Implementations execute the actual deletion logic (e.g. a JPA query) and report how many rows were removed via the
 * {@code removeCount} consumer. The framework calls this method inside a transaction, potentially multiple times in a
 * loop to support batched deletions.
 *
 * @see ExpirationTaskBuilder
 */
@FunctionalInterface
public interface ExpirationAction {

    /**
     * Removes expired entries from the database.
     * <p>
     * This method is invoked inside a transaction. It should delete a batch of expired entries whose expiration time is
     * at or before {@code currentTime} and report the number of removed rows via {@code removeCount}.
     *
     * @param session     the current Keycloak session, valid for the duration of the enclosing transaction.
     * @param realmId     the realm to clean up, or {@code null} for non-realm-aware expiration tasks.
     * @param currentTime the current time in seconds since epoch, used as the expiration threshold. This value is
     *                    constant across all batches within a single task run.
     * @param maxRemoval  the maximum number of entries to remove in this batch.
     * @param removeCount a consumer to report the number of entries removed in this batch.
     * @return {@code true} if there are more expired entries to remove (the framework will call this method again in a
     * new transaction); {@code false} if all expired entries have been removed.
     */
    boolean removeExpired(KeycloakSession session, String realmId, int currentTime, int maxRemoval, IntConsumer removeCount);

}
