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

package org.keycloak.testsuite.authz;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.locks.LockSupport.parkNanos;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import javax.script.ScriptContext;
import org.junit.Test;
import org.keycloak.authorization.policy.provider.js.ScriptCache;
import org.keycloak.models.ScriptModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.scripting.EvaluatableScriptAdapter;
import org.keycloak.scripting.ScriptBindingsConfigurer;
import org.keycloak.scripting.ScriptExecutionException;

public class ScriptCacheTest {

    /**
     * This test is non-deterministic but should fail most of the time if there is a concurrency issue
     * when caching entries
     */
    @Test
    public void testConcurrency() throws Exception {
        ScriptCache scriptCache = new ScriptCache(10, -1);
        ExecutorService executor = Executors.newWorkStealingPool();
        CompletableFuture allFutures = CompletableFuture.completedFuture(null);

        for (int i = 0; i < 500; i++) {
            String id = KeycloakModelUtils.generateId();

            CompletableFuture<Void> future = CompletableFuture.runAsync(new Runnable() {
                @Override
                public void run() {
                    scriptCache.computeIfAbsent(id, ScriptCacheTest::createScriptAndPark);
                }
            }, executor);

            // should throw an exception here during concurrent modification
            scriptCache.computeIfAbsent(id, ScriptCacheTest::createScript);

            allFutures = CompletableFuture.allOf(allFutures, future);
        }

        allFutures.get(10, TimeUnit.SECONDS);
    }

    private static EvaluatableScriptAdapter createScript(String s) {
        return new EvaluatableScriptAdapter() {
            @Override
            public ScriptModel getScriptModel() {
                return null;
            }

            @Override
            public Object eval(ScriptBindingsConfigurer bindingsConfigurer)
                    throws ScriptExecutionException {
                return null;
            }

            @Override
            public Object eval(ScriptContext context) throws ScriptExecutionException {
                return null;
            }
        };
    }

    private static EvaluatableScriptAdapter createScriptAndPark(String s) {
        parkNanos(MILLISECONDS.toNanos(10));
        return createScript(s);
    }
}
