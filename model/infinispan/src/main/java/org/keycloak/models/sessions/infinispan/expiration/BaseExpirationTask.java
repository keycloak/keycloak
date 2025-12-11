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

package org.keycloak.models.sessions.infinispan.expiration;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.KeycloakModelUtils;

import org.jboss.logging.Logger;

/**
 * ${@link BaseExpirationTask} contains the main logic to remove expired sessions from the database.
 * <p>
 * The implementation only need to provide a {@link Predicate}, by implementing {@link #realmFilter()}. This
 * {@link Predicate} decides if the session belonging to the {@link RealmModel} must be checked in this round.
 */
abstract class BaseExpirationTask implements ExpirationTask {

    protected static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final AtomicReference<PurgeExpiredTask> currentTask = new AtomicReference<>();
    private final KeycloakSessionFactory factory;
    private final int delaySeconds;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Consumer<Duration> onTaskExecuted;
    private final ExecutorService executorService;

    BaseExpirationTask(KeycloakSessionFactory factory, ScheduledExecutorService scheduledExecutorService, int delaySeconds, Consumer<Duration> onTaskExecuted) {
        this.factory = Objects.requireNonNull(factory);
        this.delaySeconds = delaySeconds;
        this.scheduledExecutorService = Objects.requireNonNull(scheduledExecutorService);
        this.onTaskExecuted = Objects.requireNonNullElse(onTaskExecuted, value -> {
        });
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            //create daemon threads
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("user-session-purge-expired");
            return t;
        });
    }

    @Override
    public void start() {
        scheduleNextTask();
    }

    @Override
    public void stop() {
        var existing = currentTask.getAndSet(null);
        if (existing == null) {
            return;
        }
        existing.scheduledFuture().cancel(true);
        executorService.shutdown();
    }

    void purgeExpired() {
        log.debug("PurgeExpired database sessions started");
        long start = System.nanoTime();
        try {
            KeycloakModelUtils.runJobInTransaction(factory, session -> {
                var provider = session.getProvider(UserSessionPersisterProvider.class);
                if (provider == null) {
                    return;
                }

                session.realms().getRealmsStream()
                        .filter(realmFilter())
                        .forEach(provider::removeExpired);
            });
        } catch (Throwable t) {
            logUnexpectedErrorDuringDeletion(t);
        } finally {
            long duration = System.nanoTime() - start;
            onTaskExecuted.accept(Duration.of(duration, ChronoUnit.NANOS));
            log.debugf("PurgeExpired tasks completed in %s seconds", TimeUnit.NANOSECONDS.toSeconds(duration));
        }
    }

    abstract Predicate<RealmModel> realmFilter();

    private void scheduleNextTask() {
        var existingTask = currentTask.get();
        var newTask = createAndSchedule();
        if (currentTask.compareAndSet(existingTask, newTask)) {
            newTask.taskFuture().thenRun(this::scheduleNextTask);
            return;
        }
        newTask.scheduledFuture().cancel(true);
    }

    private PurgeExpiredTask createAndSchedule() {
        var taskFuture = new CompletableFuture<Void>();
        var scheduleFuture = scheduledExecutorService.schedule(() -> runAndComplete(taskFuture),
                delaySeconds, TimeUnit.SECONDS);
        return new PurgeExpiredTask(scheduleFuture, taskFuture);
    }

    private void runAndComplete(CompletableFuture<Void> toComplete) {
        CompletableFuture.runAsync(this::purgeExpired, executorService)
                .exceptionally(throwable -> {
                    logUnexpectedErrorDuringDeletion(throwable);
                    return null;
                })
                .thenApply(toComplete::complete);
    }

    private static void logUnexpectedErrorDuringDeletion(Throwable throwable) {
        log.error("Unexpected error while removing expired entries from database", throwable);
    }

    private record PurgeExpiredTask(ScheduledFuture<?> scheduledFuture, CompletionStage<Void> taskFuture) {

    }
}
