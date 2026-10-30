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

package org.keycloak.cache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DefaultLocalCacheProviderFactoryTest {

    private final DefaultLocalCacheProviderFactory provider = new DefaultLocalCacheProviderFactory();

    @Test
    public void closeCallsTheRemovalListenerForEveryValue() {
        List<String> removed = new ArrayList<>();
        LocalCache<String, String> cache = provider.create(LocalCacheConfiguration.<String, String>builder()
                .name("closeCallsTheRemovalListener")
                .removalListener(removed::add)
                .build());
        cache.put("a", "1");
        cache.put("b", "2");

        cache.close();

        assertEquals(List.of("1", "2"), removed.stream().sorted().toList());
        assertNull(cache.get("a"));
    }

    @Test
    public void computeIfAbsentComputesTheValueOnce() {
        LocalCache<String, Object> cache = provider.create(LocalCacheConfiguration.<String, Object>builder()
                .name("computeIfAbsentComputesTheValueOnce")
                .expirationAfterAccess(Duration.ofMinutes(1))
                .build());

        Object first = cache.computeIfAbsent("a", key -> new Object());
        Object second = cache.computeIfAbsent("a", key -> new Object());

        assertSame(first, second);
        assertSame(first, cache.get("a"));
    }

    @Test
    public void expiredValueIsRemovedWithoutAnotherAccess() throws InterruptedException {
        CountDownLatch removed = new CountDownLatch(1);
        LocalCache<String, String> cache = provider.create(LocalCacheConfiguration.<String, String>builder()
                .name("expiredValueIsRemovedWithoutAnotherAccess")
                .expirationAfterAccess(Duration.ofMillis(100))
                .removalListener(value -> removed.countDown())
                .build());
        cache.put("a", "1");

        assertTrue(removed.await(10, TimeUnit.SECONDS));
    }
}
