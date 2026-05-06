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

package org.keycloak.connections.infinispan.shutdown;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class WaitConditionShutdownListenerTest {

    private static Date deadlineFromNow(long millis) {
        return Date.from(Instant.now().plus(millis, ChronoUnit.MILLIS));
    }

    @Test
    public void testNotInProgressReturnsImmediately() {
        var condition = new TestCondition(false);
        var listener = new WaitConditionShutdownListener(condition);
        listener.onShutdown(Instant.now(), deadlineFromNow(5000));

        assertEquals(1, condition.completeCount.get());
        assertEquals(0, condition.timeoutCount.get());
    }

    @Test
    public void testBlocksUntilCheckSignals() throws Exception {
        var condition = new TestCondition(true);
        var listener = new WaitConditionShutdownListener(condition);
        var completed = new CountDownLatch(1);

        var thread = new Thread(() -> {
            listener.onShutdown(Instant.now(), deadlineFromNow(5000));
            completed.countDown();
        });
        thread.start();

        Thread.sleep(50);
        condition.inProgress.set(false);
        listener.check();

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        assertEquals(1, condition.completeCount.get());
        assertEquals(0, condition.timeoutCount.get());
    }

    @Test
    public void testTimesOutWhenStillInProgress() throws Exception {
        var condition = new TestCondition(true);
        var listener = new WaitConditionShutdownListener(condition);
        var completed = new CountDownLatch(1);

        var thread = new Thread(() -> {
            listener.onShutdown(Instant.now(), deadlineFromNow(100));
            completed.countDown();
        });
        thread.start();

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        assertEquals(0, condition.completeCount.get());
        assertEquals(1, condition.timeoutCount.get());
    }

    @Test
    public void testCheckWhenNotInProgressSignals() throws Exception {
        var condition = new TestCondition(true);
        var listener = new WaitConditionShutdownListener(condition);
        var completed = new CountDownLatch(1);

        var thread = new Thread(() -> {
            listener.onShutdown(Instant.now(), deadlineFromNow(5000));
            completed.countDown();
        });
        thread.start();

        Thread.sleep(50);
        condition.inProgress.set(false);
        listener.check();

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        assertEquals(1, condition.completeCount.get());
    }

    @Test
    public void testCheckWhileStillInProgressDoesNotUnblock() throws Exception {
        var condition = new TestCondition(true);
        var listener = new WaitConditionShutdownListener(condition);
        var completed = new CountDownLatch(1);

        var thread = new Thread(() -> {
            listener.onShutdown(Instant.now(), deadlineFromNow(500));
            completed.countDown();
        });
        thread.start();

        Thread.sleep(50);
        listener.check();

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        assertEquals(1, condition.timeoutCount.get());
    }

    @Test
    public void testInterruptUnblocksShutdown() throws Exception {
        var condition = new TestCondition(true);
        var listener = new WaitConditionShutdownListener(condition);
        var completed = new CountDownLatch(1);

        var thread = new Thread(() -> {
            listener.onShutdown(Instant.now(), deadlineFromNow(30000));
            completed.countDown();
        });
        thread.start();

        Thread.sleep(50);
        thread.interrupt();

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        assertEquals(0, condition.completeCount.get());
        assertEquals(0, condition.timeoutCount.get());
    }

    @Test
    public void testMultipleChecksBeforeConditionClears() throws Exception {
        var condition = new TestCondition(true);
        var listener = new WaitConditionShutdownListener(condition);
        var completed = new CountDownLatch(1);

        var thread = new Thread(() -> {
            listener.onShutdown(Instant.now(), deadlineFromNow(5000));
            completed.countDown();
        });
        thread.start();

        Thread.sleep(50);
        listener.check();
        listener.check();
        condition.inProgress.set(false);
        listener.check();

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        assertEquals(1, condition.completeCount.get());
        assertEquals(0, condition.timeoutCount.get());
    }

    @Test
    public void testNullConditionThrows() {
        assertThrows(NullPointerException.class, () -> new WaitConditionShutdownListener(null));
    }

    @Test
    public void testCheckWhenNotInProgressNoOp() {
        var condition = new TestCondition(false);
        var listener = new WaitConditionShutdownListener(condition);
        listener.check();
    }

    @Test
    public void testDeadlineInThePastTimesOutImmediately() {
        var condition = new TestCondition(true);
        var listener = new WaitConditionShutdownListener(condition);
        var pastDeadline = Date.from(Instant.now().minus(1, ChronoUnit.SECONDS));

        listener.onShutdown(Instant.now(), pastDeadline);

        assertEquals(0, condition.completeCount.get());
        assertEquals(1, condition.timeoutCount.get());
    }

    private static class TestCondition implements ShutdownCondition {
        final AtomicBoolean inProgress;
        final AtomicInteger completeCount = new AtomicInteger();
        final AtomicInteger timeoutCount = new AtomicInteger();

        TestCondition(boolean initialValue) {
            this.inProgress = new AtomicBoolean(initialValue);
        }

        @Override
        public boolean inProgress() {
            return inProgress.get();
        }

        @Override
        public void onTimeout() {
            timeoutCount.incrementAndGet();
        }

        @Override
        public void complete() {
            completeCount.incrementAndGet();
        }
    }
}
