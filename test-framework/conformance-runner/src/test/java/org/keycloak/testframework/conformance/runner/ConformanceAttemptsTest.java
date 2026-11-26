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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConformanceAttemptsTest {

    private final List<String> notices = new ArrayList<>();

    @Test
    void everyRunHasItsOwnAlias() {
        Set<String> aliases = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String alias = ConformanceAttempts.newAlias("keycloak");
            assertTrue(alias.matches("keycloak-[0-9]+"), alias);
            assertTrue(aliases.add(alias), "alias used twice: " + alias);
        }
    }

    @Test
    void runsAgainUntilTheModuleFinishesAsExpected() {
        List<String> results = List.of("FAILED", "FAILED", "PASSED");

        ConformanceAttempts.Outcome outcome = ConformanceAttempts.run("module", 4,
                attempt -> result(results.get(attempt - 1)), ConformanceAttemptsTest::failure, notices::add);

        assertTrue(outcome.succeeded());
        assertEquals(3, outcome.attempt());
        assertEquals(3, notices.size(), notices::toString);
        assertTrue(notices.get(2).contains("finished as expected in attempt 3 of 4"), notices::toString);
    }

    @Test
    void returnsTheLastFailureWhenAllAttemptsFail() {
        ConformanceAttempts.Outcome outcome = ConformanceAttempts.run("module", 2,
                attempt -> result("FAILED"), ConformanceAttemptsTest::failure, notices::add);

        assertFalse(outcome.succeeded());
        assertEquals(2, outcome.attempt());
        assertEquals("finished with FAILED", outcome.failure());
    }

    @Test
    void runsAgainAfterAnException() {
        ConformanceAttempts.Outcome outcome = ConformanceAttempts.run("module", 4, attempt -> {
            if (attempt == 1) {
                throw new IllegalStateException("suite not answering");
            }
            return result("PASSED");
        }, ConformanceAttemptsTest::failure, notices::add);

        assertTrue(outcome.succeeded());
        assertTrue(notices.get(0).contains("failed, running it again: java.lang.IllegalStateException: suite not answering"),
                notices::toString);
    }

    @Test
    void rethrowsTheExceptionOfTheLastAttempt() {
        IllegalStateException last = new IllegalStateException("attempt 2");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ConformanceAttempts.run("module", 2, attempt -> {
            throw attempt == 2 ? last : new IllegalStateException("attempt 1");
        }, ConformanceAttemptsTest::failure, notices::add));

        assertSame(last, thrown);
    }

    @Test
    void doesNotRunAgainWhenTheModuleCouldNotBeStopped() {
        AtomicInteger runs = new AtomicInteger();
        ModuleCleanupFailedException cleanupFailed = new ModuleCleanupFailedException("not stopped",
                new IllegalStateException("timed out"), new IllegalStateException("HTTP 500"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ConformanceAttempts.run("module", 4, attempt -> {
            runs.incrementAndGet();
            throw cleanupFailed;
        }, ConformanceAttemptsTest::failure, notices::add));

        assertSame(cleanupFailed, thrown);
        assertEquals(1, runs.get());
        assertTrue(notices.isEmpty(), notices::toString);
    }

    @Test
    void lateCallbackOfAFailedRunReachesNeitherItsRetryNorAnotherModule() {
        // The suite routes a callback to the module run under the alias of its callback URL
        Map<String, List<String>> callbacksByAlias = new HashMap<>();
        List<String> aliasesOfModuleA = new ArrayList<>();

        ConformanceAttempts.Outcome moduleA = ConformanceAttempts.run("module-a", 4, attempt -> {
            String alias = ConformanceAttempts.newAlias("keycloak");
            aliasesOfModuleA.add(alias);
            callbacksByAlias.put(alias, new ArrayList<>());
            if (attempt == 1) {
                throw new IllegalStateException("timed out waiting for the browser");
            }
            return callbacksByAlias.get(alias).isEmpty() ? result("PASSED") : result("FAILED");
        }, ConformanceAttemptsTest::failure, notices::add);

        String aliasOfModuleB = ConformanceAttempts.newAlias("keycloak");
        callbacksByAlias.put(aliasOfModuleB, new ArrayList<>());
        // the browser of module A's first, failed run sends its authorization response only now
        callbacksByAlias.get(aliasesOfModuleA.get(0)).add("late authorization response");

        assertTrue(moduleA.succeeded(), moduleA::failure);
        assertEquals(2, aliasesOfModuleA.size());
        assertEquals(List.of("late authorization response"), callbacksByAlias.get(aliasesOfModuleA.get(0)));
        assertTrue(callbacksByAlias.get(aliasesOfModuleA.get(1)).isEmpty());
        assertTrue(callbacksByAlias.get(aliasOfModuleB).isEmpty());
    }

    private static ConformanceModuleResult result(String result) {
        return new ConformanceModuleResult("plan", Map.of(), "module", Map.of(), "plan-1", "module-1", "FINISHED", result,
                JsonNodeFactory.instance.arrayNode());
    }

    private static String failure(ConformanceModuleResult result) {
        return result.finishedWith(ConformanceResult.PASSED) ? null : "finished with " + result.result();
    }
}
