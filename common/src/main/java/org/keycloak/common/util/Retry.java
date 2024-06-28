/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.common.util;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Retry {


    /**
     * Runs the given {@code runnable} at most {@code attemptsCount} times until it passes,
     * leaving {@code intervalMillis} milliseconds between the invocations.
     * The runnable is reexecuted if it throws a {@link RuntimeException} or {@link AssertionError}.
     * @param runnable
     * @param attemptsCount Total number of attempts to execute the {@code runnable}
     * @param intervalMillis
     * @return Index of the first successful invocation, starting from 0.
     */
    public static int execute(Runnable runnable, int attemptsCount, long intervalMillis) {
        int iteration = 0;
        while (true) {
            try {
                runnable.run();
                return iteration;
            } catch (RuntimeException | AssertionError e) {
                attemptsCount--;
                iteration++;
                if (attemptsCount > 0) {
                    try {
                        if (intervalMillis > 0) {
                            Thread.sleep(intervalMillis);
                        }
                    } catch (InterruptedException ie) {
                        ie.addSuppressed(e);
                        throw new RuntimeException(ie);
                    }
                } else {
                    throw e;
                }
            }
        }
    }


    /**
     * Runs the given {@code runnable} at most {@code attemptsCount} times until it passes,
     * leaving some increasing random delay milliseconds between the invocations. It uses Exponential backoff + jitter algorithm
     * to compute the delay. More details https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
     *
     * The base for delay is specified by {@code intervalBaseMillis} number.
     *
     * The runnable is reexecuted if it throws a {@link RuntimeException} or {@link AssertionError}.
     *
     * @param runnable
     * @param attemptsCount Total number of attempts to execute the {@code runnable}
     * @param intervalBaseMillis base for the exponential backoff + jitter
     *
     * @return Index of the first successful invocation, starting from 0.
     */
    public static int executeWithBackoff(AdvancedRunnable runnable, int attemptsCount, int intervalBaseMillis) {
        return executeWithBackoff(runnable, null, attemptsCount, intervalBaseMillis);
    }


    public static int executeWithBackoff(AdvancedRunnable runnable, ThrowableCallback throwableCallback, int attemptsCount, int intervalBaseMillis) {
        long duration = 0;
        for (int i = 0; i < attemptsCount; i++) {
            duration += computeIterationBase(intervalBaseMillis, i);
        }
        return executeWithBackoff(runnable, throwableCallback, Duration.ofMillis(duration), intervalBaseMillis);
    }

    public static int executeWithBackoff(AdvancedRunnable runnable, Duration timeout, int intervalBaseMillis) {
        return executeWithBackoff(runnable, null, timeout, intervalBaseMillis);
    }

    public static int executeWithBackoff(AdvancedRunnable runnable, ThrowableCallback throwableCallback, Duration timeout, int intervalBaseMillis) {
        long maximumTime = Time.currentTimeMillis() + timeout.toMillis();

        int iteration = 0;
        while (true) {
            try {
                runnable.run(iteration);
                return iteration;
            } catch (RuntimeException | AssertionError e) {

                if (throwableCallback != null) {
                    throwableCallback.handleThrowable(iteration, e);
                }

                iteration++;
                if (Time.currentTimeMillis() < maximumTime) {
                    try {
                        if (intervalBaseMillis > 0) {
                            int delay = computeBackoffInterval(intervalBaseMillis, iteration);
                            Thread.sleep(delay);
                        }
                    } catch (InterruptedException ie) {
                        ie.addSuppressed(e);
                        throw new RuntimeException(ie);
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    public static int computeBackoffInterval(int base, int iteration) {
        return ThreadLocalRandom.current().nextInt(computeIterationBase(base, iteration));
    }

    private static int computeIterationBase(int base, int iteration) {
        return base * (1 << iteration);
    }

    /**
     * Runs the given {@code runnable} at most {@code attemptsCount} times until it passes,
     * leaving {@code intervalMillis} milliseconds between the invocations.
     * The runnable is reexecuted if it throws a {@link RuntimeException} or {@link AssertionError}.
     * @param supplier
     * @param attemptsCount Total number of attempts to execute the {@code runnable}
     * @param intervalMillis
     * @return Value generated by the {@code supplier}.
     */
    public static <T> T call(Supplier<T> supplier, int attemptsCount, long intervalMillis) {
        int iteration = 0;
        while (true) {
            try {
                return supplier.get(iteration);
            } catch (Exception | AssertionError e) {
                attemptsCount--;
                iteration++;
                if (attemptsCount > 0) {
                    try {
                        if (intervalMillis > 0) {
                            Thread.sleep(intervalMillis);
                        }
                    } catch (InterruptedException ie) {
                        ie.addSuppressed(e);
                        throw new RuntimeException(ie);
                    }
                } else {
                    throw e;
                }
            }
        }
    }


    /**
     * Runnable, which provides some additional info (iteration for now)
     */
    public interface AdvancedRunnable {

        void run(int iteration);

    }

    /**
     * Needed here because:
     * - java.util.function.BiConsumer defined from Java 8
     * - Adds some additional info (current iteration and called throwable
     */
    public interface ThrowableCallback {

        void handleThrowable(int iteration, Throwable t);

    }

    /**
     * Needed here because:
     * - java.util.function.Supplier defined from Java 8
     * - Adds some additional info (current iteration)
     */
    public interface Supplier<T> {

        /**
         * Gets a result.
         *
         * @return a result
         */
        T get(int iteration);
    }


}
