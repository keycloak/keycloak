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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.cache.infinispan.events.AuthenticationSessionAuthNoteUpdateEvent;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.infinispan.Cache;
import org.junit.Assume;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeThat;


/**
 *
 * @author hmlnarik
 */
@RequireProvider(InfinispanConnectionProvider.class)
public class CacheExpirationTest extends KeycloakModelTest {

    public static final int NUM_EXTRA_FACTORIES = 4;

    @Test
    public void testCacheExpiration() throws Exception {
        assumeFalse("Embedded caches not available for testing.", InfinispanUtils.isRemoteInfinispan());

        log.debugf("Number of previous instances of the class on the heap: %d", getNumberOfInstancesOfClass(AuthenticationSessionAuthNoteUpdateEvent.class));

        log.debug("Put two events to the main cache");
        inComittedTransaction(session -> {
            InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, Object> cache = provider.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);
            cache.entrySet().stream()
              .filter(me -> me.getValue() instanceof AuthenticationSessionAuthNoteUpdateEvent)
              .forEach((c, me) -> c.remove(me.getKey()));

            cache.put("1-2", AuthenticationSessionAuthNoteUpdateEvent.create("g1", "p1", Collections.emptyMap()), 30, TimeUnit.SECONDS);
            cache.put("1-2-3", AuthenticationSessionAuthNoteUpdateEvent.create("g2", "p2", Collections.emptyMap()), 30, TimeUnit.SECONDS);
        });
        Instant expiryInstant = Instant.now().plusSeconds(30);

        assumeThat("jmap output format unsupported", getNumberOfInstancesOfClass(AuthenticationSessionAuthNoteUpdateEvent.class), notNullValue());

        // Ensure that instance counting works as expected, there should be at least two instances in memory now.
        // Infinispan server is decoding the client request before processing the request at the cache level,
        // therefore there are sometimes three instances of AuthenticationSessionAuthNoteUpdateEvent class in the memory
        Integer instancesAfterInsertion = getNumberOfInstancesOfClass(AuthenticationSessionAuthNoteUpdateEvent.class);
        assertThat(instancesAfterInsertion, greaterThanOrEqualTo(2));

        // A third instance created when inserting the instances is never collected from GC for a yet unknown reason.
        // Therefore, ignore this additional instance in the upcoming tests.
        int previousInstancesOfClass = instancesAfterInsertion - 2;
        log.debug("Expecting instance count to go down to " + previousInstancesOfClass);

        log.debug("Starting other nodes and see that they join, receive the data and have their data expired");

        inIndependentFactories(NUM_EXTRA_FACTORIES, 2 * 60, () -> {
            log.debug("Joining the cluster");
            inComittedTransaction(session -> {
                InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
                Cache<String, Object> cache = provider.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

                log.debug("Waiting for caches to join the cluster");
                do {
                    sleep(1000);
                    assumeFalse("Items have expired already", expiryInstant.isBefore(Instant.now()));
                } while (! cache.getAdvancedCache().getDistributionManager().isJoinComplete());

                String site = CONFIG.scope("connectionsInfinispan", "default").get("siteName");
                log.debug("Cluster joined " + site);

                log.debug("Waiting for cache to receive the two elements within the cluster");
                do {
                    sleep(1000);
                    assumeFalse("Items have expired already", expiryInstant.isBefore(Instant.now()));
                } while (cache.entrySet().stream()
                        .filter(me -> me.getValue() instanceof AuthenticationSessionAuthNoteUpdateEvent)
                        .count() != 2);

                // access the items in the local cache in the different site (site-2) in order to fetch them from the remote cache
                assertThat(cache.get("1-2"), notNullValue());
                assertThat(cache.get("1-2-3"), notNullValue());
            });
        });

        // this is testing for a situation where an expiration lifespan configuration was missing in a replicated cache;
        // the elements were no longer seen in the cache, still they weren't garbage collected.
        // we must not look into the cache as that would trigger expiration explicitly.
        // original issue: https://issues.redhat.com/browse/KEYCLOAK-18518
        log.debug("Waiting for garbage collection to collect the entries across all caches in JVM");
        Instant gcInstant = Instant.now().plusSeconds(90);
        do {
            assertFalse("Items should have been garbage-collected", gcInstant.isBefore(Instant.now()));
            sleep(1000);
        } while (getNumberOfInstancesOfClass(AuthenticationSessionAuthNoteUpdateEvent.class) > previousInstancesOfClass);

        log.debug("Test completed");
    }

    private static final Pattern JMAP_HOTSPOT_PATTERN = Pattern.compile("\\s*\\d+:\\s+(\\d+)\\s+(\\d+)\\s+(\\S+)\\s*");

    public Integer getNumberOfInstancesOfClass(Class<?> c) {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String[] str = name.split("@");
        return getNumberOfInstancesOfClass(c, str[0]);
    }

    // This is synchronized as it doesn't make sense to run this in parallel with multiple threads
    // as each invocation will run a garbage collection anyway.
    public synchronized Integer getNumberOfInstancesOfClass(Class<?> c, String pid) {
        Process proc;
        try {
            // running jmap command will also trigger a garbage collection on the VM, but that might be VM specific
            // a test run with adding "-verbose:gc" showed the message "GC(23) Pause Full (Heap Inspection Initiated GC)" that
            // indicates a full GC run
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
