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

package org.keycloak.models.sessions.infinispan.transaction;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import org.infinispan.commons.util.concurrent.AggregateCompletionStage;

/**
 * Represents a non-blocking transaction.
 * <p>
 * The commit and rollback operations should not block the invoker thread and register any {@link CompletionStage} into
 * the {@link AggregateCompletionStage}. The invoker is responsible to provide the {@link AggregateCompletionStage} and
 * to wait for its completion.
 */
public interface NonBlockingTransaction {

    /**
     * Asynchronously commits the transaction.
     * <p>
     * The implementation should not block the thread and add any (or none) {@link CompletionStage} into the
     * {@code stage}.
     * <p>
     * Any blocking operation should be consumed by the {@code databaseUpdates}. It will be executed at a later
     * instant.
     *
     * @param stage           The {@link AggregateCompletionStage} to collect the {@link CompletionStage}.
     * @param databaseUpdates The {@link Consumer} to use for blocking/database updates.
     */
    void asyncCommit(AggregateCompletionStage<Void> stage, Consumer<DatabaseUpdate> databaseUpdates);

    /**
     * Asynchronously rollbacks the transaction.
     * <p>
     * The implementation should not block the thread and add any (or none) {@link CompletionStage} into the
     * {@code stage}.
     *
     * @param stage The {@link AggregateCompletionStage} to collect the {@link CompletionStage}.
     */
    void asyncRollback(AggregateCompletionStage<Void> stage);
}
