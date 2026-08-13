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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.keycloak.Config;
import org.keycloak.common.util.DurationConverter;
import org.keycloak.config.OptionsUtil;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.configuration.ServerConfigStorageProvider;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

import static org.keycloak.executors.ExecutorsProvider.EXPIRATION_TASKS;

/**
 * Shared utilities for configuring and bootstrapping {@link ExpirationTask} instances in provider factories.
 * <p>
 * Provides standard configuration keys ({@code expirationTaskIntervalSeconds}, {@code expirationTaskTimeoutSeconds}),
 * duration parsing, and executor lookup.
 */
public final class ExpirationHelper {

    private static final String EXPIRATION_TASK_INTERVAL_KEY = "expirationTaskIntervalSeconds";
    private static final int DEFAULT_EXPIRATION_TASK_INTERVAL = 600;
    private static final String EXPIRATION_TASK_TIMEOUT_KEY = "expirationTaskTimeoutSeconds";
    private static final int DEFAULT_EXPIRATION_TASK_TIMEOUT = 300;
    private static final String EXPIRATION_TASK_MAX_REMOVAL_KEY = "expirationTaskMaxRemoval";

    private ExpirationHelper() {
    }

    public static int getExpirationTaskInterval(Config.Scope config, Logger logger) {
        return parseDuration(config, logger, EXPIRATION_TASK_INTERVAL_KEY, DEFAULT_EXPIRATION_TASK_INTERVAL, "expiration task interval");
    }

    public static int getExpirationTaskTimeout(Config.Scope config, Logger logger) {
        return parseDuration(config, logger, EXPIRATION_TASK_TIMEOUT_KEY, DEFAULT_EXPIRATION_TASK_TIMEOUT, "expiration task timeout");
    }

    /**
     * Reads and validates the maximum number of entries to remove per expiration batch from the provider configuration.
     * Falls back to {@link ExpirationTaskBuilder#DEFAULT_MAX_REMOVAL} if not set or invalid.
     */
    public static int getExpirationTaskMaxRemoval(Config.Scope config, Logger logger) {
        var value = config.getInt(EXPIRATION_TASK_MAX_REMOVAL_KEY, ExpirationTaskBuilder.DEFAULT_MAX_REMOVAL);
        if (value <= 0) {
            logger.warnf("Invalid expiration task max removal specified: %d. Using default value of %d.", value, ExpirationTaskBuilder.DEFAULT_MAX_REMOVAL);
            return ExpirationTaskBuilder.DEFAULT_MAX_REMOVAL;
        }
        return value;
    }

    public static Set<Class<? extends Provider>> dependsOn() {
        return Set.of(ExecutorsProvider.class, TimerProvider.class, ServerConfigStorageProvider.class);
    }

    public static Executor expirationExecutor(KeycloakSessionFactory factory) {
        try (var session = factory.create()) {
            return session.getProvider(ExecutorsProvider.class).getExecutor(EXPIRATION_TASKS);
        }
    }

    public static void addConfiguration(ProviderConfigurationBuilder builder, String what) {
        builder.property()
                .name(EXPIRATION_TASK_INTERVAL_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("The interval in seconds between expired " + what + " cleanup runs. " + OptionsUtil.DURATION_DESCRIPTION)
                .defaultValue(DEFAULT_EXPIRATION_TASK_INTERVAL)
                .add();
        builder.property()
                .name(EXPIRATION_TASK_TIMEOUT_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("The transaction timeout in seconds for each expired " + what + " cleanup run. " + OptionsUtil.DURATION_DESCRIPTION)
                .defaultValue(DEFAULT_EXPIRATION_TASK_TIMEOUT)
                .add();
        builder.property()
                .name(EXPIRATION_TASK_MAX_REMOVAL_KEY)
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .helpText("The maximum number of expired " + what + " entries to remove per batch.")
                .defaultValue(ExpirationTaskBuilder.DEFAULT_MAX_REMOVAL)
                .add();
    }

    public static void addToOperationalInfo(int interval, int timeout, int maxRemoval, Map<String, String> info) {
        info.put(EXPIRATION_TASK_INTERVAL_KEY, interval + " seconds");
        info.put(EXPIRATION_TASK_TIMEOUT_KEY, timeout + " seconds");
        info.put(EXPIRATION_TASK_MAX_REMOVAL_KEY, Integer.toString(maxRemoval));
    }

    private static int parseDuration(Config.Scope config, Logger logger, String key, int defaultValueSeconds, String what) {
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
