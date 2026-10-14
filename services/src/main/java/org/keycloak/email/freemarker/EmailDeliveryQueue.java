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
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;

/**
 * Sends emails in the background, so requests don't wait for the mail server. When too many emails are waiting, new
 * ones are dropped. A realm can use at most half of the places and half of the threads, so one slow mail server can't
 * hold up the other realms.
 */
public final class EmailDeliveryQueue {

    private static final Logger logger = Logger.getLogger(EmailDeliveryQueue.class);
    private static final long FULL_WARNING_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);

    private final ExecutorService executor;
    private final int maxEmails;
    private final int maxEmailsPerRealm;
    private final int maxSendingPerRealm;
    private final Map<String, RealmEmails> realms = new HashMap<>();
    private int emails;
    private boolean closed;
    private long lastFullWarning;

    EmailDeliveryQueue(int threads, int queueSize) {
        this(threads, queueSize, newExecutor(threads));
    }

    EmailDeliveryQueue(int threads, int queueSize, ExecutorService executor) {
        this.executor = executor;
        this.maxEmails = threads + queueSize;
        this.maxEmailsPerRealm = Math.max(1, maxEmails / 2);
        this.maxSendingPerRealm = Math.max(1, threads / 2);
    }

    private static ExecutorService newExecutor(int threads) {
        AtomicInteger threadNumber = new AtomicInteger();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threads, threads, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), task -> {
            Thread thread = new Thread(task, "kc-email-delivery-" + threadNumber.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    /**
     * Returns {@code null} when there are too many emails. Otherwise, the email must then be sent or cancelled.
     */
    synchronized Reservation reserve(String realmId) {
        if (closed) {
            return null;
        }
        if (emails >= maxEmails) {
            warnFull();
            return null;
        }
        RealmEmails realm = realms.computeIfAbsent(realmId, RealmEmails::new);
        if (realm.emails >= maxEmailsPerRealm) {
            warnFull();
            return null;
        }
        emails++;
        realm.emails++;
        return new Reservation(realm);
    }

    synchronized void close(Duration timeout) {
        closed = true;
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        try {
            while (emails > 0 && System.currentTimeMillis() < deadline) {
                wait(Math.max(1, deadline - System.currentTimeMillis()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (emails > 0) {
            logger.warnf("%d emails were not sent before shutting down", emails);
        }
        executor.shutdownNow();
    }

    private synchronized void send(RealmEmails realm, Runnable email) {
        if (realm.sending >= maxSendingPerRealm) {
            realm.waiting.add(email);
            return;
        }
        realm.sending++;
        try {
            executor.execute(() -> {
                try {
                    email.run();
                } catch (RuntimeException e) {
                    logger.error("Failed to send email", e);
                } finally {
                    sent(realm);
                }
            });
        } catch (RejectedExecutionException e) {
            // the server is shutting down, and close() already logged the emails that are not sent
            sent(realm);
        }
    }

    private synchronized void sent(RealmEmails realm) {
        realm.sending--;
        free(realm);
        Runnable next = realm.waiting.poll();
        if (next != null) {
            send(realm, next);
        }
    }

    private synchronized void free(RealmEmails realm) {
        emails--;
        realm.emails--;
        if (realm.emails == 0) {
            realms.remove(realm.id);
        }
        notifyAll();
    }

    private void warnFull() {
        long now = System.currentTimeMillis();
        if (now - lastFullWarning >= FULL_WARNING_INTERVAL_MILLIS) {
            lastFullWarning = now;
            logger.warn("Too many emails are waiting to be sent, so new emails are dropped");
        }
    }

    final class Reservation {

        private final RealmEmails realm;

        private Reservation(RealmEmails realm) {
            this.realm = realm;
        }

        void send(Runnable email) {
            EmailDeliveryQueue.this.send(realm, email);
        }

        void cancel() {
            free(realm);
        }
    }

    private static final class RealmEmails {
        private final String id;
        private int emails;
        private int sending;
        private final Queue<Runnable> waiting = new ArrayDeque<>();

        private RealmEmails(String id) {
            this.id = id;
        }
    }
}
