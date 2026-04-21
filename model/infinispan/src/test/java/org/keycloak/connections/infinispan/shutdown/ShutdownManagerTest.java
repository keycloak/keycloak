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
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ShutdownManagerTest {

    @Test
    public void testAddAndNotifyListener() {
        var manager = new ShutdownManager();
        var listener = new RecordingListener();
        manager.addListener(listener);

        manager.onShutdown();

        assertEquals(1, listener.invocations.size());
    }

    @Test
    public void testMultipleListenersNotified() {
        var manager = new ShutdownManager();
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
        var manager = new ShutdownManager();
        var listener = new RecordingListener();
        manager.addListener(listener);
        manager.removeListener(listener);

        manager.onShutdown();

        assertTrue(listener.invocations.isEmpty());
    }

    @Test
    public void testNoListeners() {
        var manager = new ShutdownManager();
        manager.onShutdown();
    }

    @Test
    public void testAddNullListenerThrows() {
        var manager = new ShutdownManager();
        assertThrows(NullPointerException.class, () -> manager.addListener(null));
    }

    @Test
    public void testRemoveNullListenerThrows() {
        var manager = new ShutdownManager();
        assertThrows(NullPointerException.class, () -> manager.removeListener(null));
    }

    private static class RecordingListener implements ShutdownListener {
        final List<Instant> invocations = new ArrayList<>();

        @Override
        public void onShutdown(Instant shutdownTime) {
            invocations.add(shutdownTime);
        }
    }
}
