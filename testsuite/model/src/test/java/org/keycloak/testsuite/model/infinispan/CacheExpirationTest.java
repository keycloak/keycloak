/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model.infinispan;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.cache.infinispan.events.AuthenticationSessionAuthNoteUpdateEvent;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hamcrest.Matchers;
import org.infinispan.Cache;
import org.junit.Assume;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assume.assumeThat;


/**
 *
 * @author hmlnarik
 */
@RequireProvider(InfinispanConnectionProvider.class)
public class CacheExpirationTest extends KeycloakModelTest {

    @Test
    public void testCacheExpiration() throws Exception {
        AtomicLong putTime = new AtomicLong();
        inComittedTransaction(session -> {
            InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, Object> cache = provider.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);
            cache.entrySet().stream()
              .filter(me -> me.getValue() instanceof AuthenticationSessionAuthNoteUpdateEvent)
              .forEach((c, me) -> c.remove(me.getKey()));

            putTime.set(System.currentTimeMillis());
            cache.put("1-2", AuthenticationSessionAuthNoteUpdateEvent.create("g1", "p1", "r1", Collections.emptyMap()), 20000, TimeUnit.MILLISECONDS);
            cache.put("1-2-3", AuthenticationSessionAuthNoteUpdateEvent.create("g2", "p2", "r2", Collections.emptyMap()), 20000, TimeUnit.MILLISECONDS);
        });

        assumeThat("jmap output format unsupported", getNumberOfInstancesOfClass(AuthenticationSessionAuthNoteUpdateEvent.class), notNullValue());

        assertThat(getNumberOfInstancesOfClass(AuthenticationSessionAuthNoteUpdateEvent.class), is(2));

        AtomicInteger maxCountOfInstances = new AtomicInteger();
        AtomicInteger minCountOfInstances = new AtomicInteger(100);
        inIndependentFactories(4, 5 * 60, () -> {
            log.debug("Joining the cluster");
            inComittedTransaction(session -> {
                InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
                Cache<String, Object> cache = provider.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);
                do {
                    try { Thread.sleep(1000); } catch (InterruptedException ex) {}
                } while (! cache.getAdvancedCache().getDistributionManager().isJoinComplete());
                cache.keySet().forEach(s -> {});
            });
            log.debug("Cluster joined");
            int c = getNumberOfInstancesOfClass(AuthenticationSessionAuthNoteUpdateEvent.class);
            maxCountOfInstances.getAndAccumulate(c, Integer::max);
            assumeThat("Seems we're running on a way too slow a computer", System.currentTimeMillis() - putTime.get(), Matchers.lessThan(20000L));

            // Wait for at most 3 minutes which is much more than 15 seconds expiration set in DefaultInfinispanConnectionProviderFactory
            for (int i = 0; i < 3 * 60; i++) {
                try { Thread.sleep(1000); } catch (InterruptedException ex) {}
                if (getNumberOfInstancesOfClass(AuthenticationSessionAuthNoteUpdateEvent.class) == 0) {
                    break;
                }
            }

            c = getNumberOfInstancesOfClass(AuthenticationSessionAuthNoteUpdateEvent.class);
            minCountOfInstances.getAndAccumulate(c, Integer::min);
        });

        assertThat(maxCountOfInstances.get(), is(10));
        assertThat(minCountOfInstances.get(), is(0));
    }

    private static final Pattern JMAP_HOTSPOT_PATTERN = Pattern.compile("\\s*\\d+:\\s+(\\d+)\\s+(\\d+)\\s+(\\S+)\\s*");

    public Integer getNumberOfInstancesOfClass(Class<?> c) {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String[] str = name.split("@");
        return getNumberOfInstancesOfClass(c, str[0]);
    }

    public Integer getNumberOfInstancesOfClass(Class<?> c, String pid) {
        Process proc;
        try {
            proc = Runtime.getRuntime().exec("jmap -histo:live " + pid);

            try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                AtomicInteger matchingLines = new AtomicInteger();
                String className = c.getName();
                return stdInput.lines()
                  .peek(log::trace)
                  .map(JMAP_HOTSPOT_PATTERN::matcher)
                  .filter(Matcher::matches)
                  .peek(m -> matchingLines.incrementAndGet())
                  .filter(m -> Objects.equals(m.group(3), className))
                  .peek(m -> log.debugf("jmap: %s", m.group()))
                  .findAny()

                  .map(m -> Integer.valueOf(m.group(1)))
                    .orElseGet(() -> matchingLines.get() == 0 ? null : 0);
            }
        } catch (IOException ex) {
            log.debug(ex);
            Assume.assumeTrue("jmap not found or unsupported", false);
            return null;
        }
    }
}
