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
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.dblock.DBLockProviderFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class DBLockTest extends AbstractTestRealmKeycloakTest {

    private static final Logger log = Logger.getLogger(DBLockTest.class);

    private static final int SLEEP_TIME_MILLIS = 10;
    private static final int THREADS_COUNT = 20;
    private static final int THREADS_COUNT_MEDIUM = 12;
    private static final int ITERATIONS_PER_THREAD = 2;
    private static final int ITERATIONS_PER_THREAD_MEDIUM = 4;
    private static final int ITERATIONS_PER_THREAD_LONG = 20;

    private static final int LOCK_TIMEOUT_MILLIS = 240000; // Rather bigger to handle slow DB connections in testing env
    private static final int LOCK_RECHECK_MILLIS = 10;

    @Before
    public void before() throws Exception {

        testingClient.server().run(session -> {
            // Set timeouts for testing
            DBLockManager lockManager = new DBLockManager(session);
            DBLockProviderFactory lockFactory = lockManager.getDBLockFactory();
            lockFactory.setTimeouts(LOCK_RECHECK_MILLIS, LOCK_TIMEOUT_MILLIS);

            // Drop lock table, just to simulate racing threads for create lock table and insert lock record into it.
            lockManager.getDBLock().destroyLockInfo();
        });

    }

    @Test
    @ModelTest
    public void simpleLockTest(KeycloakSession session) throws Exception {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionLC) -> {
            DBLockProvider dbLock = new DBLockManager(sessionLC).getDBLock();
            dbLock.waitForLock(DBLockProvider.Namespace.DATABASE);
            try {
                Assert.assertEquals(DBLockProvider.Namespace.DATABASE, dbLock.getCurrentLock());
            } finally {
                dbLock.releaseLock();
            }
            Assert.assertNull(dbLock.getCurrentLock());
        });
    }

    @Test
    @ModelTest
    public void simpleNestedLockTest(KeycloakSession session) throws Exception {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionLC) -> {
            // first session lock DATABASE
            DBLockProvider dbLock1 = new DBLockManager(sessionLC).getDBLock();
            dbLock1.waitForLock(DBLockProvider.Namespace.DATABASE);
            try {
                Assert.assertEquals(DBLockProvider.Namespace.DATABASE, dbLock1.getCurrentLock());
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionLC2) -> {
                    // a second session/dblock-provider can lock another namespace OFFLINE_SESSIONS
                    DBLockProvider dbLock2 = new DBLockManager(sessionLC2).getDBLock();
                    dbLock2.waitForLock(DBLockProvider.Namespace.OFFLINE_SESSIONS);
                    try {
                        // getCurrentLock is local, each provider instance has one
                        Assert.assertEquals(DBLockProvider.Namespace.OFFLINE_SESSIONS, dbLock2.getCurrentLock());
                    } finally {
                        dbLock2.releaseLock();
                    }
                    Assert.assertNull(dbLock2.getCurrentLock());
                });
            } finally {
                dbLock1.releaseLock();
            }
            Assert.assertNull(dbLock1.getCurrentLock());
        });
    }

    @Test
    @ModelTest
    public void testLockConcurrentlyGeneral(KeycloakSession session) throws Exception {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionLC) -> {
            testLockConcurrentlyInternal(sessionLC, DBLockProvider.Namespace.DATABASE);
        });
    }

    @Test
    @ModelTest
    public void testLockConcurrentlyOffline(KeycloakSession session) throws Exception {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionLC) -> {
            testLockConcurrentlyInternal(sessionLC, DBLockProvider.Namespace.OFFLINE_SESSIONS);
        });
    }

    @Test
    @ModelTest
    public void testTwoLocksCurrently(KeycloakSession session) throws Exception {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionLC) -> {
            testTwoLocksCurrentlyInternal(sessionLC, DBLockProvider.Namespace.DATABASE, DBLockProvider.Namespace.OFFLINE_SESSIONS);
        });
    }

    @Test
    @ModelTest
    public void testTwoNestedLocksCurrently(KeycloakSession session) throws Exception {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionLC) -> {
            testTwoNestedLocksCurrentlyInternal(sessionLC, DBLockProvider.Namespace.KEYCLOAK_BOOT, DBLockProvider.Namespace.DATABASE);
        });
    }

    private void testTwoLocksCurrentlyInternal(KeycloakSession sessionLC, DBLockProvider.Namespace lock1, DBLockProvider.Namespace lock2) {
        final Semaphore semaphore = new Semaphore();
        final KeycloakSessionFactory sessionFactory = sessionLC.getKeycloakSessionFactory();
        List<Thread> threads = new LinkedList<>();
        // launch two threads and expect an error because the locks are different
        for (int i = 0; i < 2; i++) {
            final DBLockProvider.Namespace lock = (i % 2 == 0)? lock1 : lock2;
            Thread thread = new Thread(() -> {
                for (int j = 0; j < ITERATIONS_PER_THREAD_LONG; j++) {
                    try {
                        KeycloakModelUtils.runJobInTransaction(sessionFactory, session1 -> lock(session1, lock, semaphore));
                    } catch (RuntimeException e) {
                        semaphore.setException(e);
                    }
                }
            });
            threads.add(thread);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // interference is needed because different namespaces can interfere
        Assert.assertNotNull(semaphore.getException());
    }

    private void testTwoNestedLocksCurrentlyInternal(KeycloakSession sessionLC, DBLockProvider.Namespace lockTop, DBLockProvider.Namespace lockInner) {
        final Semaphore semaphore = new Semaphore();
        final KeycloakSessionFactory sessionFactory = sessionLC.getKeycloakSessionFactory();
        List<Thread> threads = new LinkedList<>();
        // launch two threads and expect an error because the locks are different
        for (int i = 0; i < THREADS_COUNT_MEDIUM; i++) {
            final boolean nested = i % 2 == 0;
            Thread thread = new Thread(() -> {
                for (int j = 0; j < ITERATIONS_PER_THREAD_MEDIUM; j++) {
                    try {
                        if (nested) {
                            // half the threads run two level lock top-inner
                            KeycloakModelUtils.runJobInTransaction(sessionFactory,
                                    session1 -> nestedTwoLevelLock(session1, lockTop, lockInner, semaphore));
                        } else {
                            // the other half only run a lock in the top namespace
                            KeycloakModelUtils.runJobInTransaction(sessionFactory,
                                    session1 -> lock(session1, lockTop, semaphore));
                        }
                    } catch (RuntimeException e) {
                        semaphore.setException(e);
                    }
                }
            });
            threads.add(thread);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Assert.assertEquals(THREADS_COUNT_MEDIUM * ITERATIONS_PER_THREAD_MEDIUM, semaphore.getTotal());
        Assert.assertNull(semaphore.getException());
    }

    private void testLockConcurrentlyInternal(KeycloakSession sessionLC, DBLockProvider.Namespace lock) {
        long startupTime = System.currentTimeMillis();

        final Semaphore semaphore = new Semaphore();
        final KeycloakSessionFactory sessionFactory = sessionLC.getKeycloakSessionFactory();

        List<Thread> threads = new LinkedList<>();

        for (int i = 0; i < THREADS_COUNT; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                    try {
                        KeycloakModelUtils.runJobInTransaction(sessionFactory, session1 ->
                                lock(session1, lock, semaphore));
                    } catch (RuntimeException e) {
                        semaphore.setException(e);
                        throw e;
                    }
                }
            });

            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long took = (System.currentTimeMillis() - startupTime);
        log.infof("DBLockTest executed in %d ms with total counter %d. THREADS_COUNT=%d, ITERATIONS_PER_THREAD=%d", took, semaphore.getTotal(), THREADS_COUNT, ITERATIONS_PER_THREAD);

        Assert.assertEquals(THREADS_COUNT * ITERATIONS_PER_THREAD, semaphore.getTotal());
        Assert.assertNull(semaphore.getException());
    }

    private void lock(KeycloakSession session, DBLockProvider.Namespace lock, Semaphore semaphore) {
        DBLockProvider dbLock = new DBLockManager(session).getDBLock();
        dbLock.waitForLock(lock);
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

    private void nestedTwoLevelLock(KeycloakSession session, DBLockProvider.Namespace lockTop,
            DBLockProvider.Namespace lockInner, Semaphore semaphore) {
        DBLockProvider dbLock = new DBLockManager(session).getDBLock();
        dbLock.waitForLock(lockTop);
        try {
            // create a new session to call the lock method with the inner namespace
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(),
                    sessionInner -> lock(sessionInner, lockInner, semaphore));
        } finally {
            dbLock.releaseLock();
        }
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
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



