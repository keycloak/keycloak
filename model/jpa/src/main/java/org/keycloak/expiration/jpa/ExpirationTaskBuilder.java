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

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.keycloak.expiration.jpa.impl.DefaultExpirationTask;
import org.keycloak.expiration.jpa.impl.RealmAwareExpirationTask;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelException;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

/**
 * A builder for creating {@link ExpirationTask} instances.
 * <p>
 * Required properties: {@link #withFactory(KeycloakSessionFactory)}, {@link #withAction(ExpirationAction)},
 * {@link #withEntityId(String)}, {@link #withExecutor(Executor)}, and {@link #withInterval(int, TimeUnit)}.
 * <p>
 * Optional properties: {@link #withListener(ExpirationListener)}, {@link #withMetrics(boolean)},
 * {@link #withRealmExpiration(boolean)}, {@link #withMaxRemoval(int)} (defaults to 128),
 * and {@link #withTimeout(int, TimeUnit)} (defaults to the interval).
 * <p>
 * Example usage:
 * <pre>{@code
 * ExpirationTask.builder()
 *     .withFactory(factory)
 *     .withEntityId("authentication-sessions")
 *     .withInterval(600, TimeUnit.SECONDS)
 *     .withTimeout(300, TimeUnit.SECONDS)
 *     .withAction(myAction)
 *     .withExecutor(executor)
 *     .withMetrics(true)
 *     .withRealmExpiration(true)
 *     .withMaxRemoval(128)
 *     .build()
 *     .schedule();
 * }</pre>
 *
 * @see ExpirationTask#builder()
 */
public final class ExpirationTaskBuilder {

    private static final String EXPIRATION_METRIC_NAME = "keycloak.expiration";
    private static final String EXPIRATION_DESCRIPTION = "Keycloak expiration tasks duration";

    private static final String EXPIRATION_REMOVALS_METRIC_NAME = "keycloak.expiration.removals";
    private static final String EXPIRATION_REMOVALS_DESCRIPTION = "Keycloak number of removed entities during an expiration task execution";

    private static final String TYPE_TAG = "type";
    private static final String OUTCOME_TAG = "outcome";

    static final int DEFAULT_MAX_REMOVAL = 128;

    private ExpirationAction action;
    private ExpirationListener listener;
    private Executor executor;
    private String entityId;
    private int interval;
    private int timeout;
    private int maxRemoval = DEFAULT_MAX_REMOVAL;
    private boolean metrics;
    private boolean realmExpiration;
    private KeycloakSessionFactory factory;

    // state
    private boolean intervalSet;
    private boolean timeoutSet;

    ExpirationTaskBuilder() {
    }

    /**
     * Sets the {@link ExpirationAction} that performs the actual deletion of expired entries.
     */
    public ExpirationTaskBuilder withAction(ExpirationAction action) {
        this.action = action;
        return this;
    }

    /**
     * Sets an optional {@link ExpirationListener} to be notified after each task run. When metrics are also enabled,
     * both the metrics listener and this listener are notified.
     */
    public ExpirationTaskBuilder withListener(ExpirationListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Sets the {@link Executor} used to run the cleanup work off the timer thread.
     */
    public ExpirationTaskBuilder withExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    /**
     * Sets a unique identifier for the entity type being expired (e.g. {@code "authentication-sessions"}). Used in
     * metric tags, log messages, and as the key prefix for distributed coordination via
     * {@link org.keycloak.storage.configuration.ServerConfigStorageProvider}.
     */
    public ExpirationTaskBuilder withEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    /**
     * Enables or disables Micrometer metrics for this expiration task. When enabled, a {@code keycloak.expiration}
     * timer and a {@code keycloak.expiration.removals} distribution summary are registered with tags for the entity
     * type and outcome.
     */
    public ExpirationTaskBuilder withMetrics(boolean metrics) {
        this.metrics = metrics;
        return this;
    }

    /**
     * Enables per-realm expiration. When {@code true}, the task iterates over all realms and invokes the
     * {@link ExpirationAction} once per realm. When {@code false}, the action is invoked once with a {@code null}
     * realm ID.
     */
    public ExpirationTaskBuilder withRealmExpiration(boolean realmExpiration) {
        this.realmExpiration = realmExpiration;
        return this;
    }

    /**
     * Sets the maximum number of entries to remove per batch. Defaults to {@value #DEFAULT_MAX_REMOVAL}.
     *
     * @throws ModelException if the value is not positive.
     */
    public ExpirationTaskBuilder withMaxRemoval(int maxRemoval) {
        if (maxRemoval <= 0) {
            throw new ModelException("Max removal must be greater than 0");
        }
        this.maxRemoval = maxRemoval;
        return this;
    }

    /**
     * Sets the interval between expiration task runs.
     *
     * @throws ArithmeticException if the converted value overflows an {@code int}.
     */
    public ExpirationTaskBuilder withInterval(int interval, TimeUnit timeUnit) {
        this.intervalSet = true;
        this.interval = Math.toIntExact(timeUnit.toSeconds(interval));
        return this;
    }

    /**
     * Sets the transaction timeout for each expiration task run. Defaults to the interval if not set.
     *
     * @throws ArithmeticException if the converted value overflows an {@code int}.
     */
    public ExpirationTaskBuilder withTimeout(int timeout, TimeUnit timeUnit) {
        this.timeoutSet = true;
        this.timeout = Math.toIntExact(timeUnit.toSeconds(timeout));
        return this;
    }

    /**
     * Sets the {@link KeycloakSessionFactory} used to create sessions for each transaction.
     */
    public ExpirationTaskBuilder withFactory(KeycloakSessionFactory factory) {
        this.factory = factory;
        return this;
    }

    /**
     * Builds the {@link ExpirationTask}.
     *
     * @throws NullPointerException if any required property is not set.
     * @throws ModelException       if the interval is not set or is not positive.
     */
    public ExpirationTask build() {
        Objects.requireNonNull(factory);
        Objects.requireNonNull(action);
        Objects.requireNonNull(entityId);
        Objects.requireNonNull(executor);
        if (!intervalSet) {
            throw new ModelException("Expiration interval must be set");
        }
        if (interval <= 0) {
            throw new ModelException("Interval must be greater than 0");
        }
        if (!timeoutSet) {
            timeout = interval;
        }
        return realmExpiration ?
                new RealmAwareExpirationTask(factory, executor, action, getListener(), entityId, timeout, interval, maxRemoval) :
                new DefaultExpirationTask(factory, executor, action, getListener(), entityId, timeout, interval, maxRemoval);
    }

    private ExpirationListener getListener() {
        var optionalListener = Optional.ofNullable(listener);
        if (!metrics) {
            return optionalListener.orElse(NoListener.INSTANCE);
        }
        var metricsListener = createMetrics(entityId);
        return optionalListener
                .map(userListener -> (ExpirationListener) new CompositeListener(metricsListener, userListener))
                .orElse(metricsListener);
    }

    private static Listener createMetrics(String entityId) {
        var timer = Timer.builder(EXPIRATION_METRIC_NAME)
                .description(EXPIRATION_DESCRIPTION)
                .tag(TYPE_TAG, entityId)
                .publishPercentileHistogram()
                .withRegistry(Metrics.globalRegistry);
        var counter = DistributionSummary.builder(EXPIRATION_REMOVALS_METRIC_NAME)
                .description(EXPIRATION_REMOVALS_DESCRIPTION)
                .tag(TYPE_TAG, entityId)
                .withRegistry(Metrics.globalRegistry);
        return new Listener(timer, counter);
    }

    private record Listener(Meter.MeterProvider<Timer> timer,
                            Meter.MeterProvider<DistributionSummary> counter) implements ExpirationListener {

        private Listener {
            Objects.requireNonNull(timer);
            Objects.requireNonNull(counter);
        }

        @Override
        public void onTaskRun(String realmId, Outcome outcome, int removed, Duration duration) {
            var tags = Tags.of(OUTCOME_TAG, outcome.name());
            timer.withTags(tags).record(duration);
            counter.withTags(tags).record(removed);
        }
    }

    private record CompositeListener(Listener metrics, ExpirationListener userListener) implements ExpirationListener {

        private CompositeListener {
            Objects.requireNonNull(userListener);
        }

        @Override
        public void onTaskRun(String realmId, Outcome outcome, int removed, Duration duration) {
            metrics.onTaskRun(realmId, outcome, removed, duration);
            userListener.onTaskRun(realmId, outcome, removed, duration);
        }
    }

    private enum NoListener implements ExpirationListener {
        INSTANCE;

        @Override
        public void onTaskRun(String realmId, Outcome outcome, int removed, Duration duration) {
            //no-op
        }
    }

}
