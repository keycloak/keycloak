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

package org.keycloak.expiration.jpa.impl;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.keycloak.expiration.jpa.ExpirationAction;
import org.keycloak.expiration.jpa.ExpirationListener;
import org.keycloak.expiration.jpa.ExpirationTask;
import org.keycloak.expiration.jpa.Outcome;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

/**
 * Base implementation of {@link ExpirationTask} that handles scheduling, executor delegation, and concurrency guards.
 * <p>
 * Subclasses implement {@link #doWork()} to perform the actual cleanup. The base class ensures that:
 * <ul>
 *     <li>Each {@link #run()} invocation submits work to an {@link Executor} so the timer thread is not blocked.</li>
 *     <li>Concurrent runs are prevented: if a previous run is still in progress, the new invocation is skipped.</li>
 *     <li>A transaction timeout is configured on the session factory for the duration of the cleanup.</li>
 * </ul>
 */
abstract class BaseExpirationTask implements ExpirationTask {

    protected final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final int transactionTimeoutSeconds;
    private final Executor executor;
    private final AtomicBoolean inProgress;
    protected final KeycloakSessionFactory factory;
    protected final String entityId;
    protected final int intervalSeconds;
    protected final int maxRemoval;
    protected final ExpirationAction action;
    protected final ExpirationListener listener;

    BaseExpirationTask(KeycloakSessionFactory factory, Executor executor, ExpirationAction action, ExpirationListener listener, String entityId, int transactionTimeoutSeconds, int intervalSeconds, int maxRemoval) {
        this.factory = Objects.requireNonNull(factory);
        this.executor = Objects.requireNonNull(executor);
        this.action = Objects.requireNonNull(action);
        this.listener = Objects.requireNonNull(listener);
        this.entityId = Objects.requireNonNull(entityId);
        this.transactionTimeoutSeconds = transactionTimeoutSeconds;
        this.intervalSeconds = intervalSeconds;
        this.maxRemoval = maxRemoval;
        this.inProgress = new AtomicBoolean();
    }

    @Override
    public final void run() {
        executor.execute(this::removeExpired);
    }

    @Override
    public void schedule() {
        try (var session = factory.create()) {
            var intervalMillis = TimeUnit.SECONDS.toMillis(intervalSeconds);
            session.getProvider(TimerProvider.class)
                    .schedule(this, intervalMillis, intervalMillis, "expiration-" + entityId);
        }
    }

    abstract void doWork();

    protected static Outcome computeOutcome(boolean success, boolean failed) {
        return failed ? (success ? Outcome.PARTIAL : Outcome.FAILED) : Outcome.OK;
    }

    private void removeExpired() {
        if (!inProgress.compareAndSet(false, true)) {
            logger.debugf("Skipping expiration task for '%s'. Already in progress", entityId);
            return;
        }
        KeycloakModelUtils.setTransactionLimit(factory, transactionTimeoutSeconds);
        try {
            doWork();
        } catch (Exception e) {
            logger.warnf(e, "Exception during cleanup of expired '%s'", entityId);
        } finally {
            KeycloakModelUtils.setTransactionLimit(factory, 0);
            inProgress.set(false);
        }
    }
}
