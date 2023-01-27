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

package org.keycloak.testsuite.model.globalLock;

import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.keycloak.models.dblock.DBLockGlobalLockProviderFactory;
import org.keycloak.models.locking.GlobalLockProvider;
import org.keycloak.models.locking.LockAcquiringTimeoutException;
import org.keycloak.models.locking.NoneGlobalLockProviderFactory;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@RequireProvider(value = GlobalLockProvider.class,
        exclude = { NoneGlobalLockProviderFactory.PROVIDER_ID, DBLockGlobalLockProviderFactory.PROVIDER_ID }
)
public class GlobalLocksTest extends KeycloakModelTest {

    private static final Logger LOG = Logger.getLogger(GlobalLocksTest.class);
    @Override
    protected boolean isUseSameKeycloakSessionFactoryForAllThreads() {
        return true;
    }

    @Test
    public void concurrentLockingTest() {
        final String LOCK_NAME = "simpleLockTestLockName";

        AtomicInteger counter = new AtomicInteger();
        int numIterations = 50;
        Random rand = new Random();
        List<Integer> resultingList = new LinkedList<>();

        IntStream.range(0, numIterations).parallel().forEach(index -> inComittedTransaction(s -> {
            GlobalLockProvider lockProvider = s.getProvider(GlobalLockProvider.class);
            LOG.infof("Iteration %d entered session", index);

            try {
                lockProvider.withLock(LOCK_NAME, Duration.ofSeconds(60), innerSession -> {
                    LOG.infof("Iteration %d entered locked block", index);

                    // Locked block
                    int c = counter.getAndIncrement();

                    try {
                        Thread.sleep(rand.nextInt(100));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }

                    resultingList.add(c);
                    return null;
                });
            } catch (LockAcquiringTimeoutException e) {
                throw new RuntimeException(e);
            }
        }));

        assertThat(resultingList, hasSize(numIterations));
        assertThat(resultingList, equalTo(IntStream.range(0, 50).boxed().collect(Collectors.toList())));
    }

    @Test
    public void lockTimeoutExceptionTest() {
        final String LOCK_NAME = "lockTimeoutExceptionTestLock";
        AtomicInteger counter = new AtomicInteger();
        CountDownLatch waitForTheOtherThreadToFail = new CountDownLatch(1);

        IntStream.range(0, 2).parallel().forEach(index -> inComittedTransaction(s -> {
            GlobalLockProvider lockProvider = s.getProvider(GlobalLockProvider.class);

            try {
                lockProvider.withLock(LOCK_NAME, Duration.ofSeconds(2), innerSession -> {
                    int c = counter.incrementAndGet();
                    if (c == 1) {
                        try {
                            waitForTheOtherThreadToFail.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        LOG.infof("Lock acquired by thread %s with counter: %d", Thread.currentThread().getName(), c);
                        throw new RuntimeException("Lock acquired by more than one thread.");
                    }
                    return  null;
                });
            } catch (LockAcquiringTimeoutException e) {
                int c = counter.incrementAndGet();
                LOG.infof("Exception when acquiring lock by thread %s with counter: %d", Thread.currentThread().getName(), c);
                if (c != 2) {
                    throw new RuntimeException("Acquiring lock failed by different thread than second.");
                }

                assertThat(e.getMessage(), containsString("Lock [" + LOCK_NAME + "] already acquired by keycloak instance"));
                waitForTheOtherThreadToFail.countDown();
            }
        }));
    }

    @Test
    public void testReleaseAllLocksMethod() throws InterruptedException {
        final int NUMBER_OF_THREADS = 4;
        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

        CountDownLatch locksAcquired = new CountDownLatch(NUMBER_OF_THREADS);
        CountDownLatch testFinished = new CountDownLatch(1);

        try {
            // Acquire locks and let the threads wait until the end of this test method
            executor.submit(() -> {
                IntStream.range(0, NUMBER_OF_THREADS).parallel()
                        .forEach(i ->
                                inComittedTransaction(s -> {
                                    GlobalLockProvider lockProvider = s.getProvider(GlobalLockProvider.class);
                                    try {
                                        lockProvider.withLock("LOCK_" + i, session -> {
                                            locksAcquired.countDown();
                                            try {
                                                testFinished.await();
                                            } catch (InterruptedException e) {
                                                Thread.currentThread().interrupt();
                                                throw new RuntimeException(e);
                                            }
                                            return null;
                                        });
                                    } catch (LockAcquiringTimeoutException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                        );
            });

            locksAcquired.await();

            // Test no lock can be acquired because all are still hold by the executor above
            AtomicInteger counter = new AtomicInteger();
            IntStream.range(0, NUMBER_OF_THREADS).parallel()
                    .forEach(i ->
                            inComittedTransaction(s -> {
                                GlobalLockProvider lockProvider = s.getProvider(GlobalLockProvider.class);
                                try {
                                    lockProvider.withLock("LOCK_" + i, Duration.ofSeconds(1), is -> {
                                        throw new RuntimeException("Acquiring lock should not succeed as it was acquired in the first transaction");
                                    });
                                } catch (LockAcquiringTimeoutException e) {
                                    counter.incrementAndGet();
                                }
                            })
                    );
            assertThat(counter.get(), Matchers.equalTo(NUMBER_OF_THREADS));

            // Unlock all locks forcefully
            inComittedTransaction(s -> {
                GlobalLockProvider lockProvider = s.getProvider(GlobalLockProvider.class);
                lockProvider.forceReleaseAllLocks();
            });

            // Test all locks can be acquired again
            counter.set(0);
            IntStream.range(0, NUMBER_OF_THREADS).parallel()
                    .forEach(i ->
                            inComittedTransaction(s -> {
                                GlobalLockProvider lockProvider = s.getProvider(GlobalLockProvider.class);
                                try {
                                    lockProvider.withLock("LOCK_" + i, Duration.ofSeconds(1), is -> counter.incrementAndGet());
                                } catch (LockAcquiringTimeoutException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                    );

            assertThat(counter.get(), Matchers.equalTo(NUMBER_OF_THREADS));
        } finally {
            testFinished.countDown();
            executor.shutdown();
        }
    }
}
