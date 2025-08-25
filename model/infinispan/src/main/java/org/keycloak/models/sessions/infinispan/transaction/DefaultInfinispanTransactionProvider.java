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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.infinispan.commons.util.concurrent.AggregateCompletionStage;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakTransaction;

/**
 * A {@link KeycloakTransaction} that collects {@link NonBlockingTransaction} to commit/rollback in a non-blocking
 * fashion.
 * <p>
 * This class is not thread-safe.
 */
public class DefaultInfinispanTransactionProvider extends AbstractKeycloakTransaction implements InfinispanTransactionProvider {

    private final List<NonBlockingTransaction> transactionList = new ArrayList<>(4);

    @Override
    public void registerTransaction(NonBlockingTransaction transaction) {
        transactionList.add(Objects.requireNonNull(transaction));
    }

    @Override
    public void close() {
        transactionList.clear();
    }

    @Override
    protected void commitImpl() {
        final AggregateCompletionStage<Void> stage = CompletionStages.aggregateCompletionStage();
        transactionList.forEach(transaction -> transaction.asyncCommit(stage));
        CompletionStages.join(stage.freeze());
    }

    @Override
    protected void rollbackImpl() {
        final AggregateCompletionStage<Void> stage = CompletionStages.aggregateCompletionStage();
        transactionList.forEach(transaction -> transaction.asyncRollback(stage));
        CompletionStages.join(stage.freeze());
    }
}
