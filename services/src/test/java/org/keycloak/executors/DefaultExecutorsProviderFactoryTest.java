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

package org.keycloak.executors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import org.keycloak.Config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultExecutorsProviderFactoryTest {

    private final List<ThreadPoolExecutor> pools = new ArrayList<>();

    @AfterEach
    void tearDown() {
        pools.forEach(ThreadPoolExecutor::shutdownNow);
    }

    @Test
    void emailQueueIsBoundedByDefault() {
        ThreadPoolExecutor pool = pool(Map.of(), ExecutorsProvider.EMAIL);

        assertEquals(4, pool.getCorePoolSize());
        assertEquals(64, pool.getMaximumPoolSize());
        assertEquals(128, pool.getQueue().remainingCapacity());
    }

    @Test
    void otherQueuesAreUnboundedByDefault() {
        ThreadPoolExecutor pool = pool(Map.of(), ExecutorsProvider.EXPIRATION_TASKS);

        assertEquals(Integer.MAX_VALUE, pool.getQueue().remainingCapacity());
    }

    @Test
    void poolIsConfigurable() {
        ThreadPoolExecutor pool = pool(Map.of("email.min", "2", "email.max", "8", "email.queue", "5"), ExecutorsProvider.EMAIL);

        assertEquals(2, pool.getCorePoolSize());
        assertEquals(8, pool.getMaximumPoolSize());
        assertEquals(5, pool.getQueue().remainingCapacity());
    }

    @Test
    void fixedPoolKeepsTheQueueSize() {
        ThreadPoolExecutor pool = pool(Map.of("email.min", "2", "email.max", "2"), ExecutorsProvider.EMAIL);

        assertEquals(2, pool.getMaximumPoolSize());
        assertEquals(128, pool.getQueue().remainingCapacity());
    }

    @Test
    void zeroMeansUnbounded() {
        ThreadPoolExecutor pool = pool(Map.of("email.queue", "0"), ExecutorsProvider.EMAIL);

        assertEquals(Integer.MAX_VALUE, pool.getQueue().remainingCapacity());
    }

    private ThreadPoolExecutor pool(Map<String, String> config, String taskType) {
        DefaultExecutorsProviderFactory factory = new DefaultExecutorsProviderFactory();
        factory.init(scope("", config));
        ThreadPoolExecutor pool = (ThreadPoolExecutor) factory.createPoolEmbedded(taskType);
        pools.add(pool);
        return pool;
    }

    private static Config.Scope scope(String prefix, Map<String, String> values) {
        return new Config.AbstractScope() {
            @Override
            public String get(String key) {
                return values.get(prefix + key);
            }

            @Override
            public Config.Scope scope(String... scope) {
                return DefaultExecutorsProviderFactoryTest.scope(prefix + String.join(".", scope) + ".", values);
            }

            @Override
            public Set<String> getPropertyNames() {
                return values.keySet();
            }

            @Override
            public Config.Scope root() {
                return DefaultExecutorsProviderFactoryTest.scope("", values);
            }
        };
    }
}
