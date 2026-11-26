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

package org.keycloak.common.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class RetryTest {

    @Test
    void shouldDoExactNumberOfRetries() {
        AtomicInteger i = new AtomicInteger(0);
        int retryCount = 2;
        Assertions.assertThrowsExactly(RuntimeException.class, () -> Retry.executeWithBackoff(iteration -> {
            i.incrementAndGet();
            throw new RuntimeException();
        }, retryCount, 1));

        Assertions.assertEquals(retryCount, i.get());
    }

    @Disabled("Disabled because it is slow. Run manually when needed.")
    @Test
    void shouldWaitExactlyTenSeconds() {
        long started = System.currentTimeMillis();
        AtomicLong last = new AtomicLong(System.currentTimeMillis());
        Assertions.assertThrowsExactly(RuntimeException.class, () -> Retry.executeWithBackoff(iteration -> {
            long current = System.currentTimeMillis();
            long interval = current - last.get();
            System.out.println("Interval " + iteration + ": " + interval);
            last.set(current);
            throw new RuntimeException();
        }, Duration.of(10, ChronoUnit.SECONDS), 1));

        long duration = System.currentTimeMillis() - started;
        System.out.print("Duration: " + duration);
        Assertions.assertTrue(9500 < duration  && duration < 10500);

    }

}
