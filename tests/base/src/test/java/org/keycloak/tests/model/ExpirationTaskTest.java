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

package org.keycloak.tests.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.common.util.Time;
import org.keycloak.expiration.jpa.ExpirationAction;
import org.keycloak.expiration.jpa.ExpirationListener;
import org.keycloak.expiration.jpa.ExpirationTask;
import org.keycloak.expiration.jpa.Outcome;
import org.keycloak.expiration.jpa.impl.DefaultExpirationTask;
import org.keycloak.expiration.jpa.impl.RealmAwareExpirationTask;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelException;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.annotations.TestOnServer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class ExpirationTaskTest {

    private static final Executor SYNCHRONOUS = Runnable::run;
    private static final int INTERVAL_SECONDS = 5;
    private static final int REMOVALS_PER_BATCH = 10;
    private static final String REALM_NAME_1 = "expiration-test-1";
    private static final String REALM_NAME_2 = "expiration-test-2";

    @InjectRealm(config = ExpirationTestRealm1.class, ref = "expiration-test-1")
    ManagedRealm realm1;

    @InjectRealm(config = ExpirationTestRealm2.class, ref = "expiration-test-2")
    ManagedRealm realm2;

    // -- Builder validation tests --

    @TestOnServer
    public void testBuilderRequiresFactory(KeycloakSession session) {
        assertThrows(NullPointerException.class, () -> ExpirationTask.builder()
                .withAction(noopAction())
                .withEntityId("test")
                .withExecutor(SYNCHRONOUS)
                .withInterval(1, TimeUnit.SECONDS)
                .build());
    }

    @TestOnServer
    public void testBuilderRequiresAction(KeycloakSession session) {
        assertThrows(NullPointerException.class, () -> ExpirationTask.builder()
                .withFactory(session.getKeycloakSessionFactory())
                .withEntityId("test")
                .withExecutor(SYNCHRONOUS)
                .withInterval(1, TimeUnit.SECONDS)
                .build());
    }

    @TestOnServer
    public void testBuilderRequiresEntityId(KeycloakSession session) {
        assertThrows(NullPointerException.class, () -> ExpirationTask.builder()
                .withFactory(session.getKeycloakSessionFactory())
                .withAction(noopAction())
                .withExecutor(SYNCHRONOUS)
                .withInterval(1, TimeUnit.SECONDS)
                .build());
    }

    @TestOnServer
    public void testBuilderRequiresExecutor(KeycloakSession session) {
        assertThrows(NullPointerException.class, () -> ExpirationTask.builder()
                .withFactory(session.getKeycloakSessionFactory())
                .withAction(noopAction())
                .withEntityId("test")
                .withInterval(1, TimeUnit.SECONDS)
                .build());
    }

    @TestOnServer
    public void testBuilderRequiresInterval(KeycloakSession session) {
        assertThrows(ModelException.class, () -> ExpirationTask.builder()
                .withFactory(session.getKeycloakSessionFactory())
                .withAction(noopAction())
                .withEntityId("test")
                .withExecutor(SYNCHRONOUS)
                .build());
    }

    @TestOnServer
    public void testBuilderRejectsZeroInterval(KeycloakSession session) {
        assertThrows(ModelException.class, () -> ExpirationTask.builder()
                .withFactory(session.getKeycloakSessionFactory())
                .withAction(noopAction())
                .withEntityId("test")
                .withExecutor(SYNCHRONOUS)
                .withInterval(0, TimeUnit.SECONDS)
                .build());
    }

    @TestOnServer
    public void testBuilderRejectsZeroMaxRemoval(KeycloakSession session) {
        assertThrows(ModelException.class, () -> ExpirationTask.builder()
                .withFactory(session.getKeycloakSessionFactory())
                .withAction(noopAction())
                .withEntityId("test")
                .withExecutor(SYNCHRONOUS)
                .withInterval(1, TimeUnit.SECONDS)
                .withMaxRemoval(0)
                .build());
    }

    @TestOnServer
    public void testBuilderCreatesDefaultTask(KeycloakSession session) {
        var task = buildTask("builder-default", noopAction(), new RecordingListener(), false, session.getKeycloakSessionFactory());
        assertThat(task, instanceOf(DefaultExpirationTask.class));
    }

    @TestOnServer
    public void testBuilderCreatesRealmAwareTask(KeycloakSession session) {
        var task = buildTask("builder-realm", noopAction(), new RecordingListener(), true, session.getKeycloakSessionFactory());
        assertThat(task, instanceOf(RealmAwareExpirationTask.class));
    }

    // -- DefaultExpirationTask tests --

    @TestOnServer
    public void testDefaultTaskSingleBatch(KeycloakSession session) {
        var listener = new RecordingListener();
        var callCount = new AtomicInteger();
        ExpirationAction action = (s, realmId, currentTime, maxRemoval, removeCount) -> {
            callCount.incrementAndGet();
            removeCount.accept(REMOVALS_PER_BATCH);
            return false;
        };

        var task = buildTask("default-single", action, listener, false, session.getKeycloakSessionFactory());
        task.run();

        assertThat(callCount.get(), is(1));
        assertThat(listener.events, hasSize(1));
        var event = listener.events.get(0);
        assertThat(event.realmId, nullValue());
        assertThat(event.outcome, is(Outcome.OK));
        assertThat(event.removed, is(REMOVALS_PER_BATCH));
        assertThat(event.duration.toNanos(), greaterThan(0L));
    }

    @TestOnServer
    public void testDefaultTaskReceivesMaxRemoval(KeycloakSession session) {
        var receivedMaxRemoval = new AtomicInteger();
        ExpirationAction action = (s, realmId, currentTime, maxRemoval, removeCount) -> {
            receivedMaxRemoval.set(maxRemoval);
            return false;
        };

        var customMaxRemoval = 42;
        var task = ExpirationTask.builder()
                .withFactory(session.getKeycloakSessionFactory())
                .withAction(action)
                .withEntityId("max-removal-test")
                .withExecutor(SYNCHRONOUS)
                .withInterval(INTERVAL_SECONDS, TimeUnit.SECONDS)
                .withTimeout(INTERVAL_SECONDS, TimeUnit.SECONDS)
                .withMaxRemoval(customMaxRemoval)
                .build();
        task.run();

        assertThat(receivedMaxRemoval.get(), is(customMaxRemoval));
    }

    @TestOnServer
    public void testDefaultTaskUsesDefaultMaxRemoval(KeycloakSession session) {
        var receivedMaxRemoval = new AtomicInteger();
        ExpirationAction action = (s, realmId, currentTime, maxRemoval, removeCount) -> {
            receivedMaxRemoval.set(maxRemoval);
            return false;
        };

        var task = buildTask("max-removal-default", action, new RecordingListener(), false, session.getKeycloakSessionFactory());
        task.run();

        assertThat(receivedMaxRemoval.get(), is(128));
    }

    @TestOnServer
    public void testDefaultTaskMultiBatch(KeycloakSession session) {
        var listener = new RecordingListener();
        var callCount = new AtomicInteger();
        ExpirationAction action = (s, realmId, currentTime, maxRemoval, removeCount) -> {
            removeCount.accept(REMOVALS_PER_BATCH);
            return callCount.incrementAndGet() < 3;
        };

        var task = buildTask("default-multi", action, listener, false, session.getKeycloakSessionFactory());
        task.run();

        assertThat(callCount.get(), is(3));
        assertThat(listener.events, hasSize(1));
        assertThat(listener.events.get(0).outcome, is(Outcome.OK));
        assertThat(listener.events.get(0).removed, is(REMOVALS_PER_BATCH * 3));
    }

    @TestOnServer
    public void testDefaultTaskCoordinationSkipsDuplicateRuns(KeycloakSession session) {
        var listener = new RecordingListener();
        var callCount = new AtomicInteger();
        ExpirationAction action = (s, realmId, currentTime, maxRemoval, removeCount) -> {
            callCount.incrementAndGet();
            return false;
        };

        var task = buildTask("default-coord", action, listener, false, session.getKeycloakSessionFactory());

        try {
            // first run executes
            task.run();
            assertThat(callCount.get(), is(1));

            // second run within interval is skipped
            task.run();
            assertThat(callCount.get(), is(1));

            // advance time past the interval
            Time.setOffset(INTERVAL_SECONDS + 1);

            // third run executes
            task.run();
            assertThat(callCount.get(), is(2));
            assertThat(listener.events, hasSize(2));
        } finally {
            Time.setOffset(0);
        }
    }

    @TestOnServer
    public void testDefaultTaskFailedOutcome(KeycloakSession session) {
        var listener = new RecordingListener();
        ExpirationAction action = (s, realmId, currentTime, maxRemoval, removeCount) -> {
            throw new RuntimeException("test failure");
        };

        var task = buildTask("default-failed", action, listener, false, session.getKeycloakSessionFactory());
        task.run();

        assertThat(listener.events, hasSize(1));
        assertThat(listener.events.get(0).outcome, is(Outcome.FAILED));
        assertThat(listener.events.get(0).removed, is(0));
    }

    @TestOnServer
    public void testDefaultTaskPartialOutcome(KeycloakSession session) {
        var listener = new RecordingListener();
        var callCount = new AtomicInteger();
        ExpirationAction action = (s, realmId, currentTime, maxRemoval, removeCount) -> {
            if (callCount.incrementAndGet() == 2) {
                throw new RuntimeException("second batch fails");
            }
            removeCount.accept(REMOVALS_PER_BATCH);
            return true;
        };

        var task = buildTask("default-partial", action, listener, false, session.getKeycloakSessionFactory());
        task.run();

        assertThat(listener.events, hasSize(1));
        assertThat(listener.events.get(0).outcome, is(Outcome.PARTIAL));
        assertThat(listener.events.get(0).removed, is(REMOVALS_PER_BATCH));
    }

    // -- RealmAwareExpirationTask tests --

    @TestOnServer
    public void testRealmAwareTaskPerRealmCleanup(KeycloakSession session) {
        String realmId1 = session.realms().getRealmByName(REALM_NAME_1).getId();
        String realmId2 = session.realms().getRealmByName(REALM_NAME_2).getId();

        var listener = new RecordingListener();
        var cleanedRealms = ConcurrentHashMap.<String>newKeySet();
        ExpirationAction action = (s, realmId, currentTime, maxRemoval, removeCount) -> {
            cleanedRealms.add(realmId);
            removeCount.accept(REMOVALS_PER_BATCH);
            return false;
        };

        var task = buildTask("realm-cleanup", action, listener, true, session.getKeycloakSessionFactory());
        task.run();

        assertThat(cleanedRealms.contains(realmId1), is(true));
        assertThat(cleanedRealms.contains(realmId2), is(true));
        for (var event : listener.events) {
            if (realmId1.equals(event.realmId) || realmId2.equals(event.realmId)) {
                assertThat(event.outcome, is(Outcome.OK));
                assertThat(event.removed, is(REMOVALS_PER_BATCH));
            }
        }
    }

    @TestOnServer
    public void testRealmAwareTaskCoordinationSkipsDuplicateRuns(KeycloakSession session) {
        String realmId1 = session.realms().getRealmByName(REALM_NAME_1).getId();
        String realmId2 = session.realms().getRealmByName(REALM_NAME_2).getId();

        var listener = new RecordingListener();
        var callCount = new AtomicInteger();
        ExpirationAction action = (s, realmId, currentTime, maxRemoval, removeCount) -> {
            if (realmId.equals(realmId1) || realmId.equals(realmId2)) {
                callCount.incrementAndGet();
            }
            return false;
        };

        var task = buildTask("realm-coord", action, listener, true, session.getKeycloakSessionFactory());

        try {
            // first run: both test realms cleaned
            task.run();
            assertThat(callCount.get(), is(2));

            // second run: both skipped
            task.run();
            assertThat(callCount.get(), is(2));

            // advance time
            Time.setOffset(INTERVAL_SECONDS + 1);

            // third run: both realms cleaned again
            task.run();
            assertThat(callCount.get(), is(4));
        } finally {
            Time.setOffset(0);
        }
    }

    @TestOnServer
    public void testRealmAwareTaskFailureInOneRealmDoesNotBlockOthers(KeycloakSession session) {
        String realmId1 = session.realms().getRealmByName(REALM_NAME_1).getId();
        String realmId2 = session.realms().getRealmByName(REALM_NAME_2).getId();

        var listener = new RecordingListener();
        ExpirationAction action = (s, realmId, currentTime, maxRemoval, removeCount) -> {
            if (realmId.equals(realmId1)) {
                throw new RuntimeException("realm 1 fails");
            }
            if (realmId.equals(realmId2)) {
                removeCount.accept(REMOVALS_PER_BATCH);
            }
            return false;
        };

        var task = buildTask("realm-partial-fail", action, listener, true, session.getKeycloakSessionFactory());
        task.run();

        var realm1Event = listener.events.stream().filter(e -> realmId1.equals(e.realmId)).findFirst().orElseThrow();
        assertThat(realm1Event.outcome, is(Outcome.FAILED));
        assertThat(realm1Event.removed, is(0));

        var realm2Event = listener.events.stream().filter(e -> realmId2.equals(e.realmId)).findFirst().orElseThrow();
        assertThat(realm2Event.outcome, is(Outcome.OK));
        assertThat(realm2Event.removed, is(REMOVALS_PER_BATCH));
    }

    // -- Concurrency guard test --

    @TestOnServer
    public void testConcurrencyGuardSkipsDuplicateRun(KeycloakSession session) throws Exception {
        var listener = new RecordingListener();
        var actionStarted = new CountDownLatch(1);
        var actionCanProceed = new CountDownLatch(1);
        var callCount = new AtomicInteger();

        ExpirationAction action = (s, realmId, currentTime, maxRemoval, removeCount) -> {
            callCount.incrementAndGet();
            actionStarted.countDown();
            try {
                actionCanProceed.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false;
        };

        // use a real async executor so the first run blocks inside the action
        Executor asyncExecutor = r -> new Thread(r).start();
        var task = buildTask("concurrency-guard", action, listener, false, asyncExecutor, session.getKeycloakSessionFactory());

        // start first run in background
        task.run();
        actionStarted.await(10, TimeUnit.SECONDS);

        // attempt second run while first is in progress — should be skipped
        task.run();

        // unblock first run
        actionCanProceed.countDown();

        // give the background thread time to finish
        Thread.sleep(200);

        assertThat(callCount.get(), is(1));
    }

    // -- Helpers --

    private ExpirationTask buildTask(String entityId, ExpirationAction action, ExpirationListener listener, boolean realmAware, KeycloakSessionFactory factory) {
        return buildTask(entityId, action, listener, realmAware, SYNCHRONOUS, factory);
    }

    private ExpirationTask buildTask(String entityId, ExpirationAction action, ExpirationListener listener, boolean realmAware, Executor executor, KeycloakSessionFactory factory) {
        return ExpirationTask.builder()
                .withFactory(factory)
                .withAction(action)
                .withEntityId(entityId)
                .withExecutor(executor)
                .withInterval(INTERVAL_SECONDS, TimeUnit.SECONDS)
                .withTimeout(INTERVAL_SECONDS, TimeUnit.SECONDS)
                .withListener(listener)
                .withRealmExpiration(realmAware)
                .build();
    }

    private static ExpirationAction noopAction() {
        return (session, realmId, currentTime, maxRemoval, removeCount) -> false;
    }

    private static final class RecordingListener implements ExpirationListener {
        final List<Event> events = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void onTaskRun(String realmId, Outcome outcome, int removed, Duration duration) {
            events.add(new Event(realmId, outcome, removed, duration));
        }
    }

    private record Event(String realmId, Outcome outcome, int removed, Duration duration) {
    }

    private static final class ExpirationTestRealm1 implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name(REALM_NAME_1);
        }
    }

    private static final class ExpirationTestRealm2 implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name(REALM_NAME_2);
        }
    }
}
