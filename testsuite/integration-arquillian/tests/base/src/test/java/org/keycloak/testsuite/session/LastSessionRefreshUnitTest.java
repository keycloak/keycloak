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

package org.keycloak.testsuite.session;

import org.infinispan.Cache;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.sessions.CrossDCLastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.changes.sessions.CrossDCLastSessionRefreshStoreFactory;
import org.keycloak.models.sessions.infinispan.changes.sessions.SessionData;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.timer.TimerProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class LastSessionRefreshUnitTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

    }


    @After
    public void cleanupPeriodicTask() {
        // Cleanup unneeded periodic task, which was added during this test
        testingClient.server().run((session -> {

            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTask(CrossDCLastSessionRefreshStoreFactory.LSR_PERIODIC_TASK_NAME);

        }));
    }


    @Test
    public void testLastSessionRefreshCounters() {
        testingClient.server().run(new  LastSessionRefreshServerCounterTest());
    }

    public static class LastSessionRefreshServerCounterTest extends LastSessionRefreshServerTest {


        @Override
        public void run(KeycloakSession session) {
            CrossDCLastSessionRefreshStore customStore = createStoreInstance(session, 1000000, 1000);
            System.out.println("sss");

            int lastSessionRefresh = Time.currentTime();

            // Add 8 items. No message
            for (int i=0 ; i<8 ; i++){
                customStore.putLastSessionRefresh(session, "session-" + i, "master", lastSessionRefresh);
            }
            Assert.assertEquals(0, counter.get());

            // Add 2 other items. Message sent now due the maxCount is 10
            for (int i=8 ; i<10 ; i++){
                customStore.putLastSessionRefresh(session, "session-" + i, "master", lastSessionRefresh);
            }
            Assert.assertEquals(1, counter.get());

            // Add 5 items. No additional message
            for (int i=10 ; i<15 ; i++){
                customStore.putLastSessionRefresh(session, "session-" + i, "master", lastSessionRefresh);
            }
            Assert.assertEquals(1, counter.get());

            // Add 20 items. 2 additional messages
            for (int i=15 ; i<35 ; i++){
                customStore.putLastSessionRefresh(session, "session-" + i, "master", lastSessionRefresh);
            }
            Assert.assertEquals(3, counter.get());

        }

    }


    @Test
    public void testLastSessionRefreshIntervals() {
        testingClient.server().run(new  LastSessionRefreshServerIntervalsTest());
    }

    public static class LastSessionRefreshServerIntervalsTest extends LastSessionRefreshServerTest {

        @Override
        public void run(KeycloakSession session) {
            // Long timer interval. No message due the timer wasn't executed
            CrossDCLastSessionRefreshStore customStore1 = createStoreInstance(session, 100000, 10);
            Time.setOffset(100);

            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                throw new RuntimeException();
            }
            Assert.assertEquals(0, counter.get());

            // Short timer interval 10 ms. 1 message due the interval is executed and lastRun was in the past due to Time.setOffset
            CrossDCLastSessionRefreshStore customStore2 = createStoreInstance(session, 10, 10);
            Time.setOffset(200);

            Retry.execute(() -> {
                Assert.assertEquals(1, counter.get());
            }, 100, 10);

            Assert.assertEquals(1, counter.get());

            // Another sleep won't send message. lastRun was updated
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                throw new RuntimeException();
            }
            Assert.assertEquals(1, counter.get());


            Time.setOffset(0);
        }

    }


    public static abstract class LastSessionRefreshServerTest implements RunOnServer {

        AtomicInteger counter = new AtomicInteger();

        CrossDCLastSessionRefreshStore createStoreInstance(KeycloakSession session, long timerIntervalMs, int maxIntervalBetweenMessagesSeconds) {
            CrossDCLastSessionRefreshStoreFactory factory = new CrossDCLastSessionRefreshStoreFactory() {

                @Override
                protected CrossDCLastSessionRefreshStore createStoreInstance(int maxIntervalBetweenMessagesSeconds, int maxCount, String eventKey) {
                    return new CrossDCLastSessionRefreshStore(maxIntervalBetweenMessagesSeconds, maxCount, eventKey) {

                        @Override
                        protected void sendMessage(KeycloakSession kcSession, Map<String, SessionData> refreshesToSend) {
                            counter.incrementAndGet();
                        }

                    };
                }

            };

            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            return factory.createAndInit(session, cache, timerIntervalMs, maxIntervalBetweenMessagesSeconds, 10, false);
        }

    }


}
