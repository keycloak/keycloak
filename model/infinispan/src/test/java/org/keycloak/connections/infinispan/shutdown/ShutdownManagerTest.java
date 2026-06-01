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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.keycloak.common.util.Time;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ShutdownManagerTest {

    @Test
    public void testAddAndNotifyListener() {
        var manager = new ShutdownManager(0, 5000);
        var listener = new RecordingListener();
        manager.addListener(listener);

        manager.onShutdown();

        assertEquals(1, listener.invocations.size());
    }

    @Test
    public void testMultipleListenersNotified() {
        var manager = new ShutdownManager(0, 5000);
        var listener1 = new RecordingListener();
        var listener2 = new RecordingListener();
        manager.addListener(listener1);
        manager.addListener(listener2);

        manager.onShutdown();

        assertEquals(1, listener1.invocations.size());
        assertEquals(1, listener2.invocations.size());
    }

    @Test
    public void testRemoveListener() {
        var manager = new ShutdownManager(0, 5000);
        var listener = new RecordingListener();
        manager.addListener(listener);
        manager.removeListener(listener);

        manager.onShutdown();

        assertTrue(listener.invocations.isEmpty());
    }

    @Test
    public void testNoListeners() {
        var manager = new ShutdownManager(0, 5000);
        manager.onShutdown();
    }

    @Test
    public void testAddNullListenerThrows() {
        var manager = new ShutdownManager(0, 5000);
        assertThrows(NullPointerException.class, () -> manager.addListener(null));
    }

    @Test
    public void testRemoveNullListenerThrows() {
        var manager = new ShutdownManager(0, 5000);
        assertThrows(NullPointerException.class, () -> manager.removeListener(null));
    }

    @Test
    public void testDeadlineIsProvided() {
        var manager = new ShutdownManager(0, 5000);
        var listener = new RecordingListener();
        manager.addListener(listener);

        manager.onShutdown();

        assertNotNull(listener.deadlines.get(0));
    }

    @Test
    public void testDeadlineReflectsShutdownStartTime() {
        var manager = new ShutdownManager(1000, 2000);
        var startTime = Instant.parse("2026-01-01T00:00:00Z");
        manager.onShutdownStarted(startTime);

        var listener = new RecordingListener();
        manager.addListener(listener);
        manager.onShutdown();

        var expectedDeadline = Date.from(Instant.parse("2026-01-01T00:00:03Z"));
        assertEquals(expectedDeadline, listener.deadlines.get(0));
        assertEquals(startTime, listener.invocations.get(0));
    }

    @Test
    public void testDeadlineWithoutShutdownStartTimeFallsBackToCurrentTime() {
        var manager = new ShutdownManager(0, 1000);
        var listener = new RecordingListener();
        manager.addListener(listener);

        var before = Instant.ofEpochMilli(Time.currentTimeMillis());
        manager.onShutdown();

        var deadline = listener.deadlines.get(0);
        assertTrue(deadline.after(Date.from(before)));
    }

    @Test
    public void testAllListenersShareSameDeadline() {
        var manager = new ShutdownManager(500, 1500);
        var startTime = Instant.parse("2026-01-01T00:00:00Z");
        manager.onShutdownStarted(startTime);

        var listener1 = new RecordingListener();
        var listener2 = new RecordingListener();
        manager.addListener(listener1);
        manager.addListener(listener2);

        manager.onShutdown();

        assertEquals(listener1.deadlines.get(0), listener2.deadlines.get(0));
    }

    @Test
    public void testFailingListenerDoesNotBlockOthers() {
        var manager = new ShutdownManager(0, 5000);
        var listener1 = new RecordingListener();
        var listener2 = new RecordingListener();

        manager.addListener(listener1);
        manager.addListener((shutdownTime, deadline) -> {
            throw new RuntimeException("boom");
        });
        manager.addListener(listener2);

        manager.onShutdown();

        assertEquals(1, listener1.invocations.size());
        assertEquals(1, listener2.invocations.size());
    }

    private static class RecordingListener implements ShutdownListener {
        final List<Instant> invocations = new ArrayList<>();
        final List<Date> deadlines = new ArrayList<>();

        @Override
        public void onShutdown(Instant shutdownTime, Date deadline) {
            invocations.add(shutdownTime);
            deadlines.add(deadline);
        }
    }
}
