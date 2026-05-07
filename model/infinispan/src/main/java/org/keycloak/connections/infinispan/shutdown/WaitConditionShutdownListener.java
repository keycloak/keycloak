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
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link ShutdownListener} that blocks the shutdown thread until a {@link ShutdownCondition} is no longer
 * {@linkplain ShutdownCondition#inProgress() in progress}, or until the provided deadline elapses.
 * <p>
 * External events should invoke {@link #check()} whenever the condition may have changed (e.g. after a topology change
 * event). If the condition has cleared, the waiting shutdown thread is unblocked and
 * {@link ShutdownCondition#complete()} is called. If the timeout expires first, {@link ShutdownCondition#onTimeout()}
 * is called instead.
 */
public class WaitConditionShutdownListener implements ShutdownListener {

    private final ReentrantLock lock;
    private final Condition stableCluster;
    private final ShutdownCondition condition;

    public WaitConditionShutdownListener(ShutdownCondition condition) {
        this.condition = Objects.requireNonNull(condition, "condition");
        lock = new ReentrantLock();
        stableCluster = lock.newCondition();
    }

    @Override
    public void onShutdown(Instant shutdownTime, Date deadline) {
        try {
            lock.lockInterruptibly();
            try {
                if (awaitUntilStable(deadline)) {
                    condition.complete();
                } else {
                    condition.onTimeout();
                }
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Checks whether the {@link ShutdownCondition} is still in progress and, if not, signals the waiting shutdown
     * thread to proceed.
     * <p>
     * This method should be called from external event handlers (e.g. topology change listeners) whenever the condition
     * may have changed.
     */
    public void check() {
        if (condition.inProgress()) {
            return;
        }
        try {
            lock.lockInterruptibly();
            try {
                stableCluster.signalAll();
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean awaitUntilStable(Date deadline) throws InterruptedException {
        while (condition.inProgress()) {
            if (!stableCluster.awaitUntil(deadline)) {
                return false;
            }
        }
        return true;
    }
}
