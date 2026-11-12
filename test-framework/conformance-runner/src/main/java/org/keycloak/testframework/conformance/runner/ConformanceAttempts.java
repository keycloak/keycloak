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

package org.keycloak.testframework.conformance.runner;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Runs a conformance module up to a number of attempts, as modules occasionally fail for reasons outside of the system
 * under test, e.g. network errors or the timing of the suite's waits.
 * <ul>
 * <li>An attempt counts as failed when the module does not finish with its expected result, or when its run fails with
 * an exception.</li>
 * <li>A {@link ModuleCleanupFailedException} is not retried: the module and its browser may still be running, so another
 * attempt could interfere with them.</li>
 * <li>Every run of a module, i.e. every attempt of every module, uses its own suite alias ({@link #newAlias}), so a
 * late callback of an earlier run's browser, e.g. an authorization response, goes to that run's callback URL and never
 * reaches the module of another run, neither a later attempt of the same module nor another module.</li>
 * </ul>
 */
public final class ConformanceAttempts {

    private ConformanceAttempts() {
    }

    private static final AtomicLong RUNS = new AtomicLong();

    /**
     * A suite alias no other run of a module in this JVM uses: {@code <base>-<n>}. The suite routes the callbacks to the
     * running module by the alias in the callback URL, so its callback URL has to be accepted by the system under test,
     * e.g. registered as a redirect URI of a client, before the run.
     */
    public static String newAlias(String base) {
        return base + "-" + RUNS.incrementAndGet();
    }

    /**
     * Runs the module until an attempt finishes as expected or all {@code attempts} are used.
     *
     * @param run runs the given attempt, numbered from 1
     * @param failure why a result is not the expected one, or {@code null} if it is
     * @param notices receives a message about every failed attempt that is run again, and about a module that finished
     * as expected only when run again
     * @return the result of the attempt that finished as expected, or of the last attempt, with its failure
     */
    public static Outcome run(String module, int attempts, Function<Integer, ConformanceModuleResult> run,
            Function<ConformanceModuleResult, String> failure, Consumer<String> notices) {
        for (int attempt = 1; ; attempt++) {
            ConformanceModuleResult result;
            try {
                result = run.apply(attempt);
            } catch (ModuleCleanupFailedException e) {
                throw e;
            } catch (RuntimeException e) {
                // e.g. the suite's API not answering or the module not reaching a state in time
                if (attempt < attempts) {
                    notices.accept("Attempt " + attempt + " of " + attempts + " of conformance module " + module
                            + " failed, running it again: " + e);
                    continue;
                }
                throw e;
            }
            String reason = failure.apply(result);
            if (reason == null) {
                if (attempt > 1) {
                    notices.accept("Conformance module " + module + " " + result.moduleVariant()
                            + " finished as expected in attempt " + attempt + " of " + attempts);
                }
                return new Outcome(result, null, attempt, attempts);
            }
            if (attempt >= attempts) {
                return new Outcome(result, reason, attempt, attempts);
            }
            notices.accept("Attempt " + attempt + " of " + attempts + " of conformance module " + module
                    + " did not finish as expected, running it again:\n" + reason);
        }
    }

    /**
     * The result of the last attempt, and why it did not finish as expected, {@code null} if it did.
     */
    public record Outcome(ConformanceModuleResult result, String failure, int attempt, int attempts) {

        public boolean succeeded() {
            return failure == null;
        }
    }
}
