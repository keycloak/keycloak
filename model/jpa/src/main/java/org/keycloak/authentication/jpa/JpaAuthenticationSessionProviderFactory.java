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

package org.keycloak.authentication.jpa;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.util.DurationConverter;
import org.keycloak.config.MetricsOptions;
import org.keycloak.config.OptionsUtil;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.sessions.AuthenticationSessionProviderFactory;
import org.keycloak.timer.TimerProvider;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.jboss.logging.Logger;

import static org.keycloak.executors.ExecutorsProvider.EXPIRATION_TASKS;

public class JpaAuthenticationSessionProviderFactory implements AuthenticationSessionProviderFactory<JpaAuthenticationSessionProvider>, EnvironmentDependentProviderFactory, ServerInfoAwareProviderFactory, ExpirationTask.Monitoring {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    public static final String PROVIDER_ID = "jpa";
    private static final String TASK_NAME = "expiration-auth-session";

    private int authSessionsLimit;
    private int expirationTaskIntervalSeconds;
    private int expirationTaskTimeoutSeconds;
    private Timer expirationTaskTimer;
    private Meter.MeterProvider<Timer> realmExpirationTimer;
    private Meter.MeterProvider<DistributionSummary> realmExpiredCounter;

    // Config
    private static final String AUTH_SESSION_LIMIT_KEY = "authSessionsLimit";
    private static final int DEFAULT_AUTH_SESSION_LIMIT = 300;
    private static final String EXPIRATION_TASK_INTERVAL_KEY = "expirationTaskIntervalSeconds";
    private static final int DEFAULT_EXPIRATION_TASK_INTERVAL = 600;
    private static final String EXPIRATION_TASK_TIMEOUT_KEY = "expirationTaskTimeoutSeconds";
    private static final int DEFAULT_EXPIRATION_TASK_TIMEOUT = 300;

    @Override
    public JpaAuthenticationSessionProvider create(KeycloakSession session) {
        return new JpaAuthenticationSessionProvider(session, authSessionsLimit);
    }

    @Override
    public void init(Config.Scope config) {
        authSessionsLimit = config.getInt(AUTH_SESSION_LIMIT_KEY, DEFAULT_AUTH_SESSION_LIMIT);
        expirationTaskIntervalSeconds = parseDuration(config, EXPIRATION_TASK_INTERVAL_KEY, DEFAULT_EXPIRATION_TASK_INTERVAL, "expiration task interval");
        expirationTaskTimeoutSeconds = parseDuration(config, EXPIRATION_TASK_TIMEOUT_KEY, DEFAULT_EXPIRATION_TASK_TIMEOUT, "expiration task timeout");
        if (config.root().getBoolean(MetricsOptions.METRICS_ENABLED.getKey(), Boolean.FALSE)) {
            expirationTaskTimer = Timer.builder("keycloak.expiration")
                    .description("Keycloak expiration tasks duration")
                    .tag("type", "authentication-sessions")
                    .publishPercentileHistogram()
                    .register(Metrics.globalRegistry);
            realmExpirationTimer = Timer.builder("keycloak.expiration.realm")
                    .description("Duration of an expiration task for a specific realm")
                    .tag("type", "authentication-sessions")
                    .publishPercentileHistogram()
                    .withRegistry(Metrics.globalRegistry);
            realmExpiredCounter = DistributionSummary.builder("keycloak.expiration.removed")
                    .description("Number of removed entities during a realm expiration task")
                    .tag("type", "authentication-sessions")
                    .withRegistry(Metrics.globalRegistry);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        try (var session = factory.create()) {
            var executor = session.getProvider(ExecutorsProvider.class).getExecutor(EXPIRATION_TASKS);
            var timer = session.getProvider(TimerProvider.class);
            timer.schedule(new ExpirationTask(factory, executor, this, expirationTaskIntervalSeconds, expirationTaskTimeoutSeconds),
                    expirationTaskIntervalSeconds,
                    TASK_NAME);
        }
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(JpaConnectionProvider.class, ExecutorsProvider.class, TimerProvider.class);
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.CACHELESS);
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();
        builder.property()
                .name(AUTH_SESSION_LIMIT_KEY)
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .helpText("The maximum number of concurrent authentication sessions per RootAuthenticationSession.")
                .defaultValue(DEFAULT_AUTH_SESSION_LIMIT)
                .add();
        builder.property()
                .name(EXPIRATION_TASK_INTERVAL_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("The interval in seconds between expired authentication session cleanup runs. " + OptionsUtil.DURATION_DESCRIPTION)
                .defaultValue(DEFAULT_EXPIRATION_TASK_INTERVAL)
                .add();
        builder.property()
                .name(EXPIRATION_TASK_TIMEOUT_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("The transaction timeout in seconds for each realm's expired authentication session cleanup. " + OptionsUtil.DURATION_DESCRIPTION)
                .defaultValue(DEFAULT_EXPIRATION_TASK_TIMEOUT)
                .add();
        return builder.build();
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return Map.of(
                AUTH_SESSION_LIMIT_KEY, Integer.toString(authSessionsLimit),
                EXPIRATION_TASK_INTERVAL_KEY, expirationTaskIntervalSeconds + " seconds",
                EXPIRATION_TASK_TIMEOUT_KEY, expirationTaskTimeoutSeconds + " seconds"
        );
    }

    @Override
    public void onTaskCompleted(Duration duration) {
        var timer = expirationTaskTimer;
        if (timer != null) {
            timer.record(duration);
        }
    }

    @Override
    public void onTaskCompletedForRealm(String realmName, boolean success, int removed, Duration duration) {
        var tags = List.of(Tag.of("realm", realmName), Tag.of("success", Boolean.toString(success)));
        var timer = realmExpirationTimer;
        if (timer != null) {
            timer.withTags(tags).record(duration);
        }
        var counter = realmExpiredCounter;
        if (counter != null) {
            counter.withTags(tags).record(removed);
        }
    }

    private static int parseDuration(Config.Scope config, String key, int defaultValueSeconds, String what) {
        var duration = DurationConverter.parseDuration(config.get(key));
        if (duration == null) {
            return defaultValueSeconds;
        }
        var seconds = Math.toIntExact(duration.getSeconds());
        if (seconds <= 0) {
            logger.warnf("Invalid %s specified: %s. Using default value of %s seconds.", what, duration, defaultValueSeconds);
            return defaultValueSeconds;
        }
        return seconds;
    }
}
