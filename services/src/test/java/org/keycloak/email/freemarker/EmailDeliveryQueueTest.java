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

package org.keycloak.email.freemarker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailDeliveryQueueTest {

    private final RecordingExecutor executor = new RecordingExecutor();
    private final List<String> sent = new ArrayList<>();

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void dropsEmailsWhenTheQueueIsFull() {
        // 4 places in total, 2 per realm
        EmailDeliveryQueue queue = new EmailDeliveryQueue(1, 3, executor);

        assertNotNull(queue.reserve("a"));
        assertNotNull(queue.reserve("a"));
        assertNotNull(queue.reserve("b"));
        assertNotNull(queue.reserve("b"));

        assertNull(queue.reserve("c"));
    }

    @Test
    void realmCanHaveAtMostHalfOfTheQueue() {
        EmailDeliveryQueue queue = new EmailDeliveryQueue(2, 6, executor);

        for (int i = 0; i < 4; i++) {
            assertNotNull(queue.reserve("a"));
        }
        assertNull(queue.reserve("a"));

        assertNotNull(queue.reserve("b"));
    }

    @Test
    void cancelledPlaceCanBeReservedAgain() {
        EmailDeliveryQueue queue = new EmailDeliveryQueue(1, 0, executor);
        EmailDeliveryQueue.Reservation reservation = queue.reserve("a");
        assertNull(queue.reserve("a"));

        reservation.cancel();

        assertNotNull(queue.reserve("a"));
    }

    @Test
    void realmSendsWithAtMostHalfOfTheThreads() {
        EmailDeliveryQueue queue = new EmailDeliveryQueue(4, 10, executor);
        for (int i = 1; i <= 3; i++) {
            send(queue, "a", "a" + i);
        }
        send(queue, "b", "b1");

        // realm a already sends 2 emails, so a3 waits, but b1 doesn't
        assertEquals(3, executor.tasks.size());

        executor.runNext();
        executor.runNext();
        assertEquals(List.of("a1", "a2"), sent);
        assertEquals(2, executor.tasks.size());

        executor.runAll();
        assertEquals(List.of("a1", "a2", "b1", "a3"), sent);
    }

    @Test
    void sentEmailFreesItsPlace() {
        EmailDeliveryQueue queue = new EmailDeliveryQueue(1, 0, executor);
        send(queue, "a", "first");
        assertNull(queue.reserve("a"));

        executor.runAll();

        assertNotNull(queue.reserve("a"));
    }

    @Test
    void failedEmailFreesItsPlace() {
        EmailDeliveryQueue queue = new EmailDeliveryQueue(1, 0, executor);
        queue.reserve("a").send(() -> {
            throw new IllegalStateException("Failed");
        });

        executor.runAll();

        assertNotNull(queue.reserve("a"));
    }

    @Test
    void rejectedEmailFreesItsPlace() {
        EmailDeliveryQueue queue = new EmailDeliveryQueue(1, 0, executor);
        executor.rejecting = true;

        send(queue, "a", "first");

        assertTrue(sent.isEmpty());
        assertNotNull(queue.reserve("a"));
    }

    @Test
    void closedQueueDropsEmails() {
        EmailDeliveryQueue queue = new EmailDeliveryQueue(1, 1, executor);

        queue.close(Duration.ZERO);

        assertNull(queue.reserve("a"));
        assertTrue(executor.isShutdown());
    }

    @Test
    void closeWaitsForTheEmailsBeingSent() throws InterruptedException {
        EmailDeliveryQueue queue = new EmailDeliveryQueue(1, 0, executor);
        send(queue, "a", "first");
        Thread sender = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executor.runAll();
        });
        sender.start();

        queue.close(Duration.ofSeconds(10));

        assertEquals(List.of("first"), sent);
        sender.join();
    }

    @Test
    void closeGivesUpAfterTheTimeout() {
        EmailDeliveryQueue queue = new EmailDeliveryQueue(1, 0, executor);
        send(queue, "a", "first");

        queue.close(Duration.ofMillis(50));

        assertTrue(sent.isEmpty());
        assertTrue(executor.isShutdown());
    }

    private void send(EmailDeliveryQueue queue, String realmId, String email) {
        queue.reserve(realmId).send(() -> sent.add(email));
    }

    /**
     * Runs the tasks only when the test says so.
     */
    static class RecordingExecutor extends ThreadPoolExecutor {

        final List<Runnable> tasks = new ArrayList<>();
        boolean rejecting;

        RecordingExecutor() {
            super(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        }

        @Override
        public void execute(Runnable task) {
            if (rejecting) {
                throw new RejectedExecutionException("Shutting down");
            }
            tasks.add(task);
        }

        void runNext() {
            tasks.remove(0).run();
        }

        void runAll() {
            while (!tasks.isEmpty()) {
                runNext();
            }
        }
    }
}
