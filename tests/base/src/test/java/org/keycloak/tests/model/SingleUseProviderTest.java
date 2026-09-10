/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.tests.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class SingleUseProviderTest {

    private static final Logger logger = Logger.getLogger(SingleUseProviderTest.class);

    private static final int ITEMS_COUNT = 100;
    private static final int THREADS_COUNT = 20;

    @InjectRealm(attachTo = "master")
    ManagedRealm realm;

    @TestOnServer
    public void testConcurrentRemoveFromSingleUseCacheShouldFail(KeycloakSession session) throws Exception {
        Map<Integer, Tracker> tracker = new ConcurrentHashMap<>();

        // Add some items to singleUse cache
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (session1) -> {
            for (int i = 0; i < ITEMS_COUNT; i++) {
                Map<String, String> mapp = Collections.singletonMap("my-key-" + i, "my-value-" + i);
                SingleUseObjectProvider singleUseProvider = session1.getProvider(SingleUseObjectProvider.class);
                singleUseProvider.put("my-key-" + i, 1000, mapp);
                tracker.put(i, new Tracker());
            }
        });

        // Try to remove all items
        Runnable runnable = () -> {

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (session1) -> {
                // Each thread trying to remove all items
                for (int i = 0; i < ITEMS_COUNT ; i++) {
                    SingleUseObjectProvider singleUseProvider1 = session1.getProvider(SingleUseObjectProvider.class);
                    Map<String, String> data = singleUseProvider1.remove("my-key-" + i);
                    if (data != null) {
                        tracker.get(i).countSuccess.incrementAndGet();
                    } else {
                        tracker.get(i).countFailures.incrementAndGet();
                    }
                }
            });
        };

        // Try to remove all items concurrently
        List<Thread> workers = new ArrayList<>();
        for (int j=0 ; j< THREADS_COUNT ; j++) {
            Thread t = new Thread(runnable);
            workers.add(t);
            t.start();
        }

        for (Thread t : workers) {
            t.join();
        }

        // Check countSuccess and countFailures. For each key, only single successful "remove" is allowed. Other threads should fail to remove the item and nothing should be found
        for (Map.Entry<Integer, Tracker> entry : tracker.entrySet()) {
            logger.debug(entry.getKey() + ": " + entry.getValue());
        }

        for (Map.Entry<Integer, Tracker> entry : tracker.entrySet()) {
            Assertions.assertEquals(1, entry.getValue().countSuccess.get());
            Assertions.assertEquals(THREADS_COUNT - 1, entry.getValue().countFailures.get());
        }
    }

    private class Tracker {
        AtomicInteger countSuccess = new AtomicInteger(0);
        AtomicInteger countFailures = new AtomicInteger(0);

        @Override
        public String toString() {
            return "success: " + countSuccess.get() + ", failures: " + countFailures.get();
        }

    }
}
