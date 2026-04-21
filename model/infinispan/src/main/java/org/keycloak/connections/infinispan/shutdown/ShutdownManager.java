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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.keycloak.common.util.Time;

import org.jboss.logging.Logger;

/**
 * Manages an ordered list of {@link ShutdownListener} instances that are notified sequentially when the server shuts
 * down.
 * <p>
 * Listeners are invoked in registration order. Each listener may block the calling thread to delay shutdown until its
 * condition is met (e.g. waiting for a stable cache topology).
 * <p>
 * This class is thread-safe: listeners can be added or removed concurrently.
 */
public class ShutdownManager {

    private static final Logger logger = Logger.getLogger(ShutdownManager.class);

    private final List<ShutdownListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(ShutdownListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void removeListener(ShutdownListener listener) {
        listeners.remove(Objects.requireNonNull(listener));
    }

    public void onShutdown() {
        var instant = Instant.ofEpochMilli(Time.currentTimeMillis());
        for (var listener : listeners) {
            try {
                listener.onShutdown(instant);
            } catch (Exception e) {
                logger.warnf(e, "Shutdown listener %s failed", listener);
            }
        }
    }

}
