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

package org.keycloak.tls;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.keycloak.common.util.DurationConverter;

import org.jboss.logging.Logger;

public final class CertificateReloadScheduler {

    private static final Logger log = Logger.getLogger(CertificateReloadScheduler.class);

    private static ScheduledExecutorService executor;

    private CertificateReloadScheduler() {
    }

    public static Optional<Duration> parsePeriod(String value) {
        if (value == null || value.isBlank() || "-1".equals(value.trim())) {
            return Optional.empty();
        }
        Duration period = DurationConverter.parseDuration(value.trim());
        if (period == null || period.isZero() || period.isNegative()) {
            return Optional.empty();
        }
        return Optional.of(period);
    }

    public static ScheduledFuture<?> schedule(Duration period, Runnable task) {
        long millis = period.toMillis();
        log.debugf("Scheduling reload of certificates every %s", period);
        return getExecutor().scheduleWithFixedDelay(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.warnf(e, "Failed to reload certificates, the previously loaded certificates will be used");
            }
        }, millis, millis, TimeUnit.MILLISECONDS);
    }

    private static synchronized ScheduledExecutorService getExecutor() {
        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "keycloak-certificate-reloader");
                thread.setDaemon(true);
                return thread;
            });
        }
        return executor;
    }
}
