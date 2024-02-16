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

package org.keycloak.testsuite.model.singleUseObject;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.DefaultActionTokenKey;
import org.keycloak.common.util.Time;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserModel;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;

@RequireProvider(SingleUseObjectProvider.class)
public class SingleUseObjectModelTest extends KeycloakModelTest {

    private String realmId;

    private String userId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "realm");
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realmId = realm.getId();
        UserModel user = s.users().addUser(realm, "user");
        userId = user.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testActionTokens() {
        DefaultActionTokenKey key = withRealm(realmId, (session, realm) -> {
            SingleUseObjectProvider singleUseObjectProvider = session.singleUseObjects();
            int time = Time.currentTime();
            DefaultActionTokenKey actionTokenKey = new DefaultActionTokenKey(userId, UUID.randomUUID().toString(), time + 60, null);
            Map<String, String> notes = new HashMap<>();
            notes.put("foo", "bar");
            singleUseObjectProvider.put(actionTokenKey.serializeKey(), actionTokenKey.getExp() - time, notes);
            return actionTokenKey;
        });

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseObjectProvider = session.singleUseObjects();
            Map<String, String> notes = singleUseObjectProvider.get(key.serializeKey());
            Assert.assertNotNull(notes);
            Assert.assertEquals("bar", notes.get("foo"));

            notes = singleUseObjectProvider.remove(key.serializeKey());
            Assert.assertNotNull(notes);
            Assert.assertEquals("bar", notes.get("foo"));
        });

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseObjectProvider = session.singleUseObjects();
            Map<String, String> notes = singleUseObjectProvider.get(key.serializeKey());
            Assert.assertNull(notes);

            notes = new HashMap<>();
            notes.put("foo", "bar");
            singleUseObjectProvider.put(key.serializeKey(), key.getExp() - Time.currentTime(), notes);
        });

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseObjectProvider = session.singleUseObjects();
            Map<String, String> notes = singleUseObjectProvider.get(key.serializeKey());
            Assert.assertNotNull(notes);
            Assert.assertEquals("bar", notes.get("foo"));
        });

        setTimeOffset(70);

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseObjectProvider = session.singleUseObjects();
            Map<String, String> notes = singleUseObjectProvider.get(key.serializeKey());
            notes = singleUseObjectProvider.get(key.serializeKey());
            Assert.assertNull(notes);
        });
    }

    @Test
    public void testSingleUseStore() {
        String key = UUID.randomUUID().toString();
        Map<String, String> notes = new HashMap<>();
        notes.put("foo", "bar");

        Map<String, String> notes2 = new HashMap<>();
        notes2.put("baf", "meow");

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseStore = session.singleUseObjects();
            Assert.assertFalse(singleUseStore.replace(key, notes2));

            singleUseStore.put(key,  60, notes);
        });

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseStore = session.singleUseObjects();
            Map<String, String> actualNotes = singleUseStore.get(key);
            Assert.assertEquals(notes, actualNotes);

            Assert.assertTrue(singleUseStore.replace(key, notes2));
        });

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseStore = session.singleUseObjects();
            Map<String, String> actualNotes = singleUseStore.get(key);
            Assert.assertEquals(notes2, actualNotes);

            Assert.assertFalse(singleUseStore.putIfAbsent(key, 60));

            Assert.assertEquals(notes2, singleUseStore.remove(key));
        });

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseStore = session.singleUseObjects();
            Assert.assertTrue(singleUseStore.putIfAbsent(key, 60));
        });

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseStore = session.singleUseObjects();
            Map<String, String> actualNotes = singleUseStore.get(key);
            assertThat(actualNotes, Matchers.anEmptyMap());
        });

        setTimeOffset(70);

        inComittedTransaction(session -> {
            SingleUseObjectProvider singleUseStore = session.singleUseObjects();
            Assert.assertNull(singleUseStore.get(key));
        });
    }

    @Test
    public void testCluster() throws InterruptedException {
        AtomicInteger index = new AtomicInteger();
        CountDownLatch afterFirstNodeLatch = new CountDownLatch(1);
        CountDownLatch afterDeleteLatch = new CountDownLatch(1);
        CountDownLatch clusterJoined = new CountDownLatch(4);
        CountDownLatch replicationDone = new CountDownLatch(4);

        String key = UUID.randomUUID().toString();
        AtomicReference<String> actionTokenKey = new AtomicReference<>();
        Map<String, String> notes = new HashMap<>();
        notes.put("foo", "bar");

        inIndependentFactories(4, 60, () -> {
            log.debug("Joining the cluster");
            clusterJoined.countDown();
            awaitLatch(clusterJoined);
            log.debug("Cluster joined");

            if (index.incrementAndGet() == 1) {
                actionTokenKey.set(withRealm(realmId, (session, realm) -> {
                    SingleUseObjectProvider singleUseStore = session.singleUseObjects();
                    singleUseStore.put(key, 60, notes);

                    int time = Time.currentTime();
                    DefaultActionTokenKey atk = new DefaultActionTokenKey(userId, UUID.randomUUID().toString(), time + 60, null);
                    singleUseStore.put(atk.serializeKey(), atk.getExp() - time, notes);

                    return atk.serializeKey();
                }));

                afterFirstNodeLatch.countDown();
            }
            awaitLatch(afterFirstNodeLatch);

            // check if single-use object/action token is available on all nodes
            inComittedTransaction(session -> {
                SingleUseObjectProvider singleUseStore = session.singleUseObjects();
                while (singleUseStore.get(key) == null || singleUseStore.get(actionTokenKey.get()) == null) {
                    sleep(1000);
                }
                replicationDone.countDown();
            });

            awaitLatch(replicationDone);

            // remove objects on one node
            if (index.incrementAndGet() == 5) {
                inComittedTransaction(session -> {
                    SingleUseObjectProvider singleUseStore = session.singleUseObjects();
                    singleUseStore.remove(key);
                    singleUseStore.remove(actionTokenKey.get());
                });

                afterDeleteLatch.countDown();
            }

            awaitLatch(afterDeleteLatch);

            // check if single-use object/action token is removed
            inComittedTransaction(session -> {
                SingleUseObjectProvider singleUseStore = session.singleUseObjects();

                while (singleUseStore.get(key) != null && singleUseStore.get(actionTokenKey.get()) != null) {
                   sleep(1000);
                }
            });
        });
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
