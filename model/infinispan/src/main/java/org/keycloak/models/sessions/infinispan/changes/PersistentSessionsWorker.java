/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.changes;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.sessions.infinispan.PersistentUserSessionProvider;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Run one thread per session type and drain the queues once there is an entry. Will batch entries if possible.
 *
 * @author Alexander Schwartz
 */
public class PersistentSessionsWorker {
    private static final Logger LOG = Logger.getLogger(PersistentSessionsWorker.class);

    private final KeycloakSessionFactory factory;
    private final ArrayBlockingQueue<PersistentDeferredElement<String, UserSessionEntity>> asyncQueueUserSessions;
    private final ArrayBlockingQueue<PersistentDeferredElement<String, UserSessionEntity>> asyncQueueUserOfflineSessions;
    private final ArrayBlockingQueue<PersistentDeferredElement<UUID, AuthenticatedClientSessionEntity>> asyncQueueClientSessions;
    private final ArrayBlockingQueue<PersistentDeferredElement<UUID, AuthenticatedClientSessionEntity>> asyncQueueClientOfflineSessions;
    private final List<Thread> threads = new ArrayList<>();
    private volatile boolean stop;

    public PersistentSessionsWorker(KeycloakSessionFactory factory, ArrayBlockingQueue<PersistentDeferredElement<String, UserSessionEntity>> asyncQueueUserSessions, ArrayBlockingQueue<PersistentDeferredElement<String, UserSessionEntity>> asyncQueueUserOfflineSessions, ArrayBlockingQueue<PersistentDeferredElement<UUID, AuthenticatedClientSessionEntity>> asyncQueueClientSessions, ArrayBlockingQueue<PersistentDeferredElement<UUID, AuthenticatedClientSessionEntity>> asyncQueueClientOfflineSessions) {
        this.factory = factory;
        this.asyncQueueUserSessions = asyncQueueUserSessions;
        this.asyncQueueUserOfflineSessions = asyncQueueUserOfflineSessions;
        this.asyncQueueClientSessions = asyncQueueClientSessions;
        this.asyncQueueClientOfflineSessions = asyncQueueClientOfflineSessions;
    }

    public void start() {
        threads.add(new WorkerUserSession(asyncQueueUserSessions, false));
        threads.add(new WorkerUserSession(asyncQueueUserOfflineSessions, true));
        threads.add(new WorkerClientSession(asyncQueueClientSessions, false));
        threads.add(new WorkerClientSession(asyncQueueClientOfflineSessions, true));
        threads.forEach(Thread::start);
    }

    private class WorkerUserSession extends Worker<String, UserSessionEntity> {
        public WorkerUserSession(ArrayBlockingQueue<PersistentDeferredElement<String, UserSessionEntity>> queue, boolean offline) {
            super(queue, offline, PersistentUserSessionProvider::processDeferredUserSessionElements);
        }
    }

    private class WorkerClientSession extends Worker<UUID, AuthenticatedClientSessionEntity> {
        public WorkerClientSession(ArrayBlockingQueue<PersistentDeferredElement<UUID, AuthenticatedClientSessionEntity>> queue, boolean offline) {
            super(queue, offline, PersistentUserSessionProvider::processDeferredClientSessionElements);
        }
    }

    private class Worker<K, V extends SessionEntity> extends Thread {
        private final ArrayBlockingQueue<PersistentDeferredElement<K, V>> queue;
        private final boolean offline;
        private final Adapter<K, V> adapter;

        public Worker(ArrayBlockingQueue<PersistentDeferredElement<K, V>> queue, boolean offline, Adapter<K, V> adapter) {
            this.queue = queue;
            this.offline = offline;
            this.adapter = adapter;
        }

        public void run() {
            Thread.currentThread().setName(this.getClass().getName() + " for " + (offline ? "offline" : "online") + " sessions");
            while (!stop) {
                try {
                    process(queue, offline);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private void process(ArrayBlockingQueue<PersistentDeferredElement<K, V>> queue, boolean offline) throws InterruptedException {
            Collection<PersistentDeferredElement<K, V>> batch = new ArrayList<>();
            PersistentDeferredElement<K, V> polled = queue.poll(100, TimeUnit.MILLISECONDS);
            if (polled != null) {
                queue.add(polled);
                queue.drainTo(batch, 99);
                try {
                    KeycloakModelUtils.runJobInTransaction(factory,
                            session -> adapter.run(((PersistentUserSessionProvider) session.getProvider(UserSessionProvider.class)), batch, offline));
                } catch (RuntimeException ex) {
                    LOG.warnf(ex, "Unable to write %d deferred session updates", queue.size());
                }
            }
        }

        interface Adapter<K, V extends SessionEntity> {
            void run(PersistentUserSessionProvider sessionProvider, Collection<PersistentDeferredElement<K, V>> batch, boolean offline);
        }
    }

    public void stop() {
        stop = true;
        threads.forEach(Thread::interrupt);
        threads.forEach(t -> {
            try {
                t.join(TimeUnit.MINUTES.toMillis(1));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
