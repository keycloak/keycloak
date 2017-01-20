/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.dblock.DBLockProviderFactory;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DBLockTest extends AbstractModelTest {

    private static final Logger log = Logger.getLogger(DBLockTest.class);

    private static final int SLEEP_TIME_MILLIS = 10;
    private static final int THREADS_COUNT = 20;
    private static final int ITERATIONS_PER_THREAD = 2;

    private static final int LOCK_TIMEOUT_MILLIS = 240000; // Rather bigger to handle slow DB connections in testing env
    private static final int LOCK_RECHECK_MILLIS = 10;

    @Before
    @Override
    public void before() throws Exception {
        super.before();

        // Set timeouts for testing
        DBLockManager lockManager = new DBLockManager(session);
        DBLockProviderFactory lockFactory = lockManager.getDBLockFactory();
        lockFactory.setTimeouts(LOCK_RECHECK_MILLIS, LOCK_TIMEOUT_MILLIS);

        // Drop lock table, just to simulate racing threads for create lock table and insert lock record into it.
        lockManager.getDBLock().destroyLockInfo();

        commit();
    }

    @Test
    public void testLockConcurrently() throws Exception {
        long startupTime = System.currentTimeMillis();

        final Semaphore semaphore = new Semaphore();
        final KeycloakSessionFactory sessionFactory = realmManager.getSession().getKeycloakSessionFactory();

        List<Thread> threads = new LinkedList<>();
        for (int i=0 ; i<THREADS_COUNT ; i++) {
            Thread thread = new Thread() {

                @Override
                public void run() {
                    for (int i=0 ; i<ITERATIONS_PER_THREAD ; i++) {
                        try {
                            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                                @Override
                                public void run(KeycloakSession session) {
                                    lock(session, semaphore);
                                }

                            });
                        } catch (RuntimeException e) {
                            semaphore.setException(e);
                            throw e;
                        }
                    }
                }

            };
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        long took = (System.currentTimeMillis() - startupTime);
        log.infof("DBLockTest executed in %d ms with total counter %d. THREADS_COUNT=%d, ITERATIONS_PER_THREAD=%d", took, semaphore.getTotal(), THREADS_COUNT, ITERATIONS_PER_THREAD);
        Assert.assertEquals(semaphore.getTotal(), THREADS_COUNT * ITERATIONS_PER_THREAD);
        Assert.assertNull(semaphore.getException());
    }

    private void lock(KeycloakSession session, Semaphore semaphore) {
        DBLockProvider dbLock = new DBLockManager(session).getDBLock();
        dbLock.waitForLock();
        try {
            semaphore.increase();
            Thread.sleep(SLEEP_TIME_MILLIS);
            semaphore.decrease();
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        } finally {
            dbLock.releaseLock();
        }
    }


    // Ensure just one thread is allowed to run at the same time
    private class Semaphore {

        private AtomicInteger counter = new AtomicInteger(0);
        private AtomicInteger totalIncreases = new AtomicInteger(0);

        private volatile Exception exception = null;

        private void increase() {
            int current = counter.incrementAndGet();
            if (current != 1) {
                IllegalStateException ex = new IllegalStateException("Counter has illegal value: " + current);
                setException(ex);
                throw ex;
            }
            totalIncreases.incrementAndGet();
        }

        private void decrease() {
            int current = counter.decrementAndGet();
            if (current != 0) {
                IllegalStateException ex = new IllegalStateException("Counter has illegal value: " + current);
                setException(ex);
                throw ex;
            }
        }

        private synchronized void setException(Exception exception) {
            if (this.exception == null) {
                this.exception = exception;
            }
        }

        private synchronized Exception getException() {
            return exception;
        }

        private int getTotal() {
            return totalIncreases.get();
        }
    }




}
