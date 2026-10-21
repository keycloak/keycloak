/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model.util;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;

/**
 * This controller adds possibility to manually control more transaction within
 * one test case.
 * <p />
 * It uses ExecutorService to run each transaction in a separate thread. This
 * is necessary, for example, for pessimistic locking in HotRod as the locks
 * needs to be reentrant by the same thread. If this is running in one thread
 * the pessimistic locking does not work as all transactions are able to
 * acquire the same lock repeatedly.
 */
public class TransactionController implements AutoCloseable {
    private final AtomicReference<KeycloakSession> session = new AtomicReference<>();
    private final ExecutorService executor;
    private final AtomicReference<String> threadName = new AtomicReference<>();

    public TransactionController(KeycloakSessionFactory sessionFactory) {
        executor = Executors.newSingleThreadExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        executor.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });
        try {
            if (!latch.await(1, TimeUnit.MINUTES)) {
                throw new RuntimeException("Initialization of TransactionController timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        executeAndWaitUntilFinished(() -> threadName.set(Thread.currentThread().getName()));
        executeAndWaitUntilFinished(() -> session.set(sessionFactory.create()));
    }

    public void begin() {
        executeAndWaitUntilFinished(() -> getTransactionManager().begin());
    }

    public void commit() {
        executeAndWaitUntilFinished(() -> getTransactionManager().commit());
    }

    public void rollback() {
        executeAndWaitUntilFinished(() -> getTransactionManager().rollback());
    }

    public <R> R runStep(Function<KeycloakSession, R> task) {
        AtomicReference<R> result = new AtomicReference<>();
        executeAndWaitUntilFinished(() -> result.set(task.apply(session.get())));
        return result.get();
    }

    private KeycloakTransactionManager getTransactionManager() {
        return session.get().getTransactionManager();
    }

    private void executeAndWaitUntilFinished(Runnable runnable) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<RuntimeException> exception = new AtomicReference<>();
        executor.execute(() -> {
            if (!Objects.equals(threadName.get(), Thread.currentThread().getName())) {
                throw new RuntimeException("Execution running in different thread");
            }
            try {
                runnable.run();
            } catch (RuntimeException ex) {
                exception.set(ex);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(1, TimeUnit.MINUTES)) {
                throw new RuntimeException("Waiting for the operation to finish timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        if (exception.get() != null) {
            throw exception.get();
        }
    }

    @Override
    public void close() throws Exception {
        // Shutdown executor
        executor.shutdown();
        try {
            // Wait until it is terminated
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                // Shutdown forcefully
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
