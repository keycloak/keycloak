/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
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
package org.keycloak.tests.events.outbox;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.outbox.OutboxCleanupTask;
import org.keycloak.events.outbox.OutboxStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.OutboxEntryEntity;
import org.keycloak.models.jpa.entities.OutboxEntryStatus;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link OutboxStore}. Each test runs a lambda
 * against a real Keycloak session via {@code runOnServer}, exercising
 * the store against the actual JPA provider and the {@code OUTBOX_ENTRY}
 * schema.
 *
 * <p>Lives at {@code tests/base} alongside the other core integration
 * tests ({@code tests/db}, {@code tests/events}, etc.) — the outbox
 * is core Keycloak infrastructure, not SSF-specific. Tests use the
 * synthetic entryKinds {@code "test-kind"} and {@code "test-kind-other"}
 * so the suite verifies generic store semantics independent of any
 * specific consumer.
 *
 * <p>Tests are isolated by pinning each one to a unique synthetic
 * {@code realmId}; teardown calls {@link OutboxStore#deleteByRealm}
 * for both test entryKinds to remove every row enqueued during the
 * test. No real Keycloak realms / clients are created — the store
 * treats realmId / ownerId as opaque strings, so synthetic UUIDs
 * exercise the full path.
 */
@KeycloakIntegrationTest(config = OutboxStoreTests.OutboxStoreServerConfig.class)
public class OutboxStoreTests {

    /** Synthetic entryKind used by these tests — not tied to SSF semantics. */
    private static final String TEST_KIND = "test-kind";

    /**
     * A second synthetic entryKind for cross-kind isolation tests, so we
     * verify that {@code (entryKind, ...)} compound indexes really do
     * keep traffic from different kinds separated.
     */
    private static final String OTHER_KIND = "test-kind-other";

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    // REALM_ID is VARCHAR(36), so we use a bare UUID — exactly 36 chars.
    // Prefixed labels like "outbox-test-realm-<uuid>" overflow the column.
    private final String testRealmId = UUID.randomUUID().toString();

    @AfterEach
    public void cleanupRealm() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            OutboxStore store = new OutboxStore(session);
            store.deleteByRealm(TEST_KIND, realmId);
            store.deleteByRealm(OTHER_KIND, realmId);
        });
    }

    // -- Enqueue ---------------------------------------------------------

    @Test
    public void enqueuePending_persistsRowWithExpectedDefaults() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            OutboxStore store = new OutboxStore(session);
            String id = store.enqueuePending(TEST_KIND, realmId, "owner-a", "container-a",
                    "corr-happy", "test.event", "payload-1", null);
            Assertions.assertNotNull(id);

            em(session).flush();
            em(session).clear();

            OutboxEntryEntity row = findById(session, id);
            Assertions.assertNotNull(row, "row should have been persisted");
            Assertions.assertEquals(TEST_KIND, row.getEntryKind());
            Assertions.assertEquals(realmId, row.getRealmId());
            Assertions.assertEquals("owner-a", row.getOwnerId());
            Assertions.assertEquals("container-a", row.getContainerId());
            Assertions.assertEquals("corr-happy", row.getCorrelationId());
            Assertions.assertEquals("test.event", row.getEntryType());
            Assertions.assertEquals("payload-1", row.getPayload());
            Assertions.assertNull(row.getMetadata());
            Assertions.assertEquals(OutboxEntryStatus.PENDING, row.getStatus());
            Assertions.assertEquals(0, row.getAttempts());
            Assertions.assertNotNull(row.getCreatedAt());
            Assertions.assertNotNull(row.getNextAttemptAt());
            Assertions.assertNull(row.getDeliveredAt());
            Assertions.assertNull(row.getLastError());
        });
    }

    @Test
    public void enqueuePending_deduplicatesOnEntryKindOwnerAndCorrelationId() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            OutboxStore store = new OutboxStore(session);
            String first = store.enqueuePending(TEST_KIND, realmId, "owner-dedup", "container-a",
                    "corr-dedup", "test.event", "payload-1", null);
            String second = store.enqueuePending(TEST_KIND, realmId, "owner-dedup", "container-b",
                    "corr-dedup", "test.event", "payload-2", null);

            Assertions.assertEquals(first, second,
                    "second enqueue for the same (entryKind, ownerId, correlationId) should return the original id");

            em(session).flush();
            em(session).clear();

            OutboxEntryEntity row = findById(session, first);
            Assertions.assertEquals("payload-1", row.getPayload(),
                    "original payload must be preserved across dedup");
            Assertions.assertEquals("container-a", row.getContainerId(),
                    "original containerId must be preserved across dedup");
        });
    }

    @Test
    public void enqueuePending_doesNotDedupAcrossEntryKinds() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            OutboxStore store = new OutboxStore(session);
            // Same (ownerId, correlationId) but different kinds — both must persist.
            String first = store.enqueuePending(TEST_KIND, realmId, "owner-x", null,
                    "corr-shared", "test.event", "p-1", null);
            String second = store.enqueuePending(OTHER_KIND, realmId, "owner-x", null,
                    "corr-shared", "test.event", "p-2", null);

            Assertions.assertNotEquals(first, second,
                    "different entryKinds with the same (ownerId, correlationId) must persist as separate rows");
        });
    }

    @Test
    public void enqueueHeld_persistsRowInHeldStatus() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            OutboxStore store = new OutboxStore(session);
            String id = store.enqueueHeld(TEST_KIND, realmId, "owner-h", null,
                    "corr-held", "test.event", "payload", null);
            em(session).flush();
            em(session).clear();

            OutboxEntryEntity row = findById(session, id);
            Assertions.assertEquals(OutboxEntryStatus.HELD, row.getStatus(),
                    "enqueueHeld must persist the row with HELD status");
        });
    }

    // -- Drainer reads ---------------------------------------------------

    @Test
    public void lockDueForDrain_returnsOnlyRowsWithNextAttemptAtInThePast() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            persistRaw(session, TEST_KIND, realmId, "owner-due", null, "corr-past",
                    OutboxEntryStatus.PENDING, 0, now.minusSeconds(10), now.minusSeconds(30));
            persistRaw(session, TEST_KIND, realmId, "owner-due", null, "corr-future",
                    OutboxEntryStatus.PENDING, 0, now.plusSeconds(60), now.minusSeconds(30));
            em(session).flush();
            em(session).clear();

            List<OutboxEntryEntity> due = new OutboxStore(session).lockDueForDrain(TEST_KIND, 10);
            Assertions.assertEquals(1, due.size(),
                    "only rows whose nextAttemptAt is in the past should be returned");
            Assertions.assertEquals("corr-past", due.get(0).getCorrelationId());
        });
    }

    @Test
    public void lockDueForDrain_respectsBatchLimit() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            for (int i = 0; i < 5; i++) {
                persistRaw(session, TEST_KIND, realmId, "owner-batch", null, "corr-b-" + i,
                        OutboxEntryStatus.PENDING, 0,
                        now.minusSeconds(i + 1L), now.minusSeconds(10));
            }
            em(session).flush();
            em(session).clear();

            List<OutboxEntryEntity> batch = new OutboxStore(session).lockDueForDrain(TEST_KIND, 3);
            Assertions.assertEquals(3, batch.size());
        });
    }

    @Test
    public void lockDueForDrain_filtersByEntryKind() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            persistRaw(session, TEST_KIND, realmId, "o", null, "corr-test-kind",
                    OutboxEntryStatus.PENDING, 0, now.minusSeconds(10), now.minusSeconds(30));
            persistRaw(session, OTHER_KIND, realmId, "o", null, "corr-other-kind",
                    OutboxEntryStatus.PENDING, 0, now.minusSeconds(10), now.minusSeconds(30));
            em(session).flush();
            em(session).clear();

            List<OutboxEntryEntity> due = new OutboxStore(session).lockDueForDrain(TEST_KIND, 10);
            Assertions.assertEquals(1, due.size(),
                    "lockDueForDrain must filter on entryKind");
            Assertions.assertEquals("corr-test-kind", due.get(0).getCorrelationId());
        });
    }

    // -- Row transitions -------------------------------------------------

    @Test
    public void markDelivered_setsStatusAndDeliveredAtAndClearsLastError() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            OutboxEntryEntity row = persistRaw(session, TEST_KIND, realmId, "owner-d", null,
                    "corr-deliver", OutboxEntryStatus.PENDING, 2,
                    Instant.now(), Instant.now());
            row.setLastError("earlier transient failure");
            em(session).flush();
            em(session).clear();

            new OutboxStore(session).markDelivered(findById(session, row.getId()));
            em(session).flush();
            em(session).clear();

            OutboxEntryEntity after = findById(session, row.getId());
            Assertions.assertEquals(OutboxEntryStatus.DELIVERED, after.getStatus());
            Assertions.assertNotNull(after.getDeliveredAt());
            Assertions.assertNull(after.getLastError(),
                    "lastError should be cleared once delivery succeeds");
        });
    }

    @Test
    public void recordFailure_bumpsAttemptsAndSchedulesRetryAndTruncatesLongError() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            OutboxEntryEntity row = persistRaw(session, TEST_KIND, realmId, "owner-f", null,
                    "corr-fail", OutboxEntryStatus.PENDING, 2,
                    now.minusSeconds(1), now);
            em(session).flush();
            em(session).clear();

            // Truncate to microseconds up front — PostgreSQL TIMESTAMP
            // stores micros and rounds half-up, while H2 preserves
            // nanos. Truncating before the write means every backend
            // reads back the exact same Instant we persisted.
            Instant nextAttempt = now.plus(Duration.ofMinutes(5)).truncatedTo(ChronoUnit.MICROS);
            String hugeError = "x".repeat(3000);
            new OutboxStore(session)
                    .recordFailure(findById(session, row.getId()), nextAttempt, hugeError);
            em(session).flush();
            em(session).clear();

            OutboxEntryEntity after = findById(session, row.getId());
            Assertions.assertEquals(OutboxEntryStatus.PENDING, after.getStatus(),
                    "recordFailure keeps the row in PENDING for the next drainer tick");
            Assertions.assertEquals(3, after.getAttempts(), "attempts must be incremented");
            Assertions.assertEquals(nextAttempt, after.getNextAttemptAt());
            Assertions.assertNotNull(after.getLastError());
            Assertions.assertTrue(after.getLastError().length() <= OutboxStore.MAX_LAST_ERROR_LENGTH,
                    "lastError must be truncated to the column width");
            Assertions.assertTrue(after.getLastError().endsWith("..."),
                    "truncated lastError must carry an ellipsis marker");
        });
    }

    @Test
    public void markDeadLetter_setsStatusAndIncrementsAttemptsAndStoresError() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            OutboxEntryEntity row = persistRaw(session, TEST_KIND, realmId, "owner-dl", null,
                    "corr-dead", OutboxEntryStatus.PENDING, 8,
                    Instant.now().minusSeconds(1), Instant.now());
            em(session).flush();
            em(session).clear();

            new OutboxStore(session)
                    .markDeadLetter(findById(session, row.getId()), "giving up after 8 retries");
            em(session).flush();
            em(session).clear();

            OutboxEntryEntity after = findById(session, row.getId());
            Assertions.assertEquals(OutboxEntryStatus.DEAD_LETTER, after.getStatus());
            Assertions.assertEquals(9, after.getAttempts(),
                    "markDeadLetter bumps attempts so the terminal count reflects the final try");
            Assertions.assertEquals("giving up after 8 retries", after.getLastError());
        });
    }

    @Test
    public void promoteStaleQueuedToDeadLetter_promotesOnlyOldQueuedRows() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();

            // Old PENDING — should be promoted.
            OutboxEntryEntity oldPending = persistRaw(session, TEST_KIND, realmId, "owner-stale", null,
                    "corr-old-pending", OutboxEntryStatus.PENDING, 0,
                    now.plusSeconds(60), now.minus(Duration.ofDays(3)));

            // Old HELD — should be promoted (HELD is in QUEUED).
            OutboxEntryEntity oldHeld = persistRaw(session, TEST_KIND, realmId, "owner-stale", null,
                    "corr-old-held", OutboxEntryStatus.HELD, 0,
                    now.plusSeconds(60), now.minus(Duration.ofDays(3)));

            // Fresh PENDING — survives, hasn't aged out.
            OutboxEntryEntity freshPending = persistRaw(session, TEST_KIND, realmId, "owner-stale", null,
                    "corr-fresh-pending", OutboxEntryStatus.PENDING, 0,
                    now.plusSeconds(60), now.minusSeconds(60));

            // Old DELIVERED — terminal already, must not be touched.
            OutboxEntryEntity oldDelivered = persistRaw(session, TEST_KIND, realmId, "owner-stale", null,
                    "corr-old-delivered", OutboxEntryStatus.DELIVERED, 1,
                    now.plusSeconds(60), now.minus(Duration.ofDays(3)));
            oldDelivered.setDeliveredAt(now.minus(Duration.ofDays(3)));

            // Old DEAD_LETTER — terminal already, must not have its
            // attempts or lastError clobbered by the bulk update.
            OutboxEntryEntity oldDeadLetter = persistRaw(session, TEST_KIND, realmId, "owner-stale", null,
                    "corr-old-dl", OutboxEntryStatus.DEAD_LETTER, 8,
                    now.plusSeconds(60), now.minus(Duration.ofDays(3)));
            oldDeadLetter.setLastError("original dead-letter reason");

            em(session).flush();
            em(session).clear();

            int promoted = new OutboxStore(session).promoteStaleQueuedToDeadLetter(
                    TEST_KIND, now.minus(Duration.ofDays(2)),
                    "queued exceeded pendingMaxAge");
            Assertions.assertEquals(2, promoted,
                    "old PENDING + HELD rows must be promoted; other statuses are skipped");

            em(session).flush();
            em(session).clear();

            OutboxEntryEntity afterOldPending = findById(session, oldPending.getId());
            Assertions.assertEquals(OutboxEntryStatus.DEAD_LETTER, afterOldPending.getStatus());
            Assertions.assertEquals(0, afterOldPending.getAttempts(),
                    "stale-promotion must not bump attempts — these rows didn't actually retry");
            Assertions.assertEquals("queued exceeded pendingMaxAge",
                    afterOldPending.getLastError());

            Assertions.assertEquals(OutboxEntryStatus.DEAD_LETTER,
                    findById(session, oldHeld.getId()).getStatus(),
                    "HELD rows in the QUEUED set must also be promoted");
            Assertions.assertEquals(OutboxEntryStatus.PENDING,
                    findById(session, freshPending.getId()).getStatus(),
                    "PENDING rows newer than the cutoff must survive");
            Assertions.assertEquals(OutboxEntryStatus.DELIVERED,
                    findById(session, oldDelivered.getId()).getStatus(),
                    "DELIVERED rows must not be touched by the backstop");

            OutboxEntryEntity afterOldDeadLetter = findById(session, oldDeadLetter.getId());
            Assertions.assertEquals(OutboxEntryStatus.DEAD_LETTER, afterOldDeadLetter.getStatus());
            Assertions.assertEquals("original dead-letter reason", afterOldDeadLetter.getLastError(),
                    "already-DEAD_LETTER rows must keep their original lastError");
        });
    }

    // -- Stats -----------------------------------------------------------

    @Test
    public void countStatusesForRealm_returnsGroupedCountsAndIgnoresOtherRealms() {
        final String realmId = testRealmId;
        final String otherRealmId = UUID.randomUUID().toString();
        try {
            runOnServer.run(session -> {
                Instant now = Instant.now();
                persistRaw(session, TEST_KIND, realmId, "o", null, "p1",
                        OutboxEntryStatus.PENDING, 0, now, now);
                persistRaw(session, TEST_KIND, realmId, "o", null, "p2",
                        OutboxEntryStatus.PENDING, 0, now, now);
                persistRaw(session, TEST_KIND, realmId, "o", null, "d1",
                        OutboxEntryStatus.DELIVERED, 1, now, now);
                persistRaw(session, TEST_KIND, realmId, "o", null, "dl1",
                        OutboxEntryStatus.DEAD_LETTER, 8, now, now);
                persistRaw(session, TEST_KIND, otherRealmId, "o2", null, "other",
                        OutboxEntryStatus.PENDING, 0, now, now);
                em(session).flush();
                em(session).clear();

                Map<OutboxEntryStatus, Long> counts = new OutboxStore(session)
                        .countStatusesForRealm(TEST_KIND, realmId);
                Assertions.assertEquals(2L, counts.get(OutboxEntryStatus.PENDING));
                Assertions.assertEquals(1L, counts.get(OutboxEntryStatus.DELIVERED));
                Assertions.assertEquals(1L, counts.get(OutboxEntryStatus.DEAD_LETTER));
                Assertions.assertNull(counts.get(OutboxEntryStatus.HELD),
                        "statuses with zero rows must be absent (no synthetic zero rows)");
            });
        } finally {
            runOnServer.run(session -> new OutboxStore(session).deleteByRealm(TEST_KIND, otherRealmId));
        }
    }

    @Test
    public void oldestCreatedAtPerStatusForRealm_returnsEarliestCreatedAtPerStatus() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            Instant oldPending = now.minus(Duration.ofDays(3)).truncatedTo(ChronoUnit.MICROS);
            Instant midPending = now.minus(Duration.ofHours(6)).truncatedTo(ChronoUnit.MICROS);
            Instant freshDelivered = now.minusSeconds(120).truncatedTo(ChronoUnit.MICROS);

            persistRaw(session, TEST_KIND, realmId, "o", null, "old",
                    OutboxEntryStatus.PENDING, 0, now, oldPending);
            persistRaw(session, TEST_KIND, realmId, "o", null, "mid",
                    OutboxEntryStatus.PENDING, 0, now, midPending);
            persistRaw(session, TEST_KIND, realmId, "o", null, "delivered",
                    OutboxEntryStatus.DELIVERED, 1, now, freshDelivered);
            em(session).flush();
            em(session).clear();

            Map<OutboxEntryStatus, Instant> oldest = new OutboxStore(session)
                    .oldestCreatedAtPerStatusForRealm(TEST_KIND, realmId);
            Assertions.assertEquals(oldPending, oldest.get(OutboxEntryStatus.PENDING),
                    "PENDING must report the oldest createdAt across PENDING rows");
            Assertions.assertEquals(freshDelivered, oldest.get(OutboxEntryStatus.DELIVERED));
            Assertions.assertNull(oldest.get(OutboxEntryStatus.DEAD_LETTER));
        });
    }

    @Test
    public void countStatusesForOwner_returnsGroupedCountsAndIgnoresOtherOwners() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            persistRaw(session, TEST_KIND, realmId, "owner-stats", null, "cs-p1",
                    OutboxEntryStatus.PENDING, 0, now, now);
            persistRaw(session, TEST_KIND, realmId, "owner-stats", null, "cs-p2",
                    OutboxEntryStatus.PENDING, 0, now, now);
            persistRaw(session, TEST_KIND, realmId, "owner-stats", null, "cs-d",
                    OutboxEntryStatus.DELIVERED, 1, now, now);
            persistRaw(session, TEST_KIND, realmId, "owner-stats-other", null, "cs-other",
                    OutboxEntryStatus.PENDING, 0, now, now);
            em(session).flush();
            em(session).clear();

            Map<OutboxEntryStatus, Long> counts = new OutboxStore(session)
                    .countStatusesForOwner(TEST_KIND, "owner-stats");
            Assertions.assertEquals(2L, counts.get(OutboxEntryStatus.PENDING),
                    "PENDING count must include only the target owner's rows");
            Assertions.assertEquals(1L, counts.get(OutboxEntryStatus.DELIVERED));
            Assertions.assertNull(counts.get(OutboxEntryStatus.DEAD_LETTER));
        });
    }

    @Test
    public void oldestCreatedAtPerStatusForOwner_returnsEarliestCreatedAtPerStatus() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            Instant oldPending = now.minus(Duration.ofDays(2)).truncatedTo(ChronoUnit.MICROS);
            Instant midPending = now.minus(Duration.ofHours(3)).truncatedTo(ChronoUnit.MICROS);
            Instant otherOwnerPending = now.minus(Duration.ofDays(5)).truncatedTo(ChronoUnit.MICROS);

            persistRaw(session, TEST_KIND, realmId, "owner-old", null, "co-old",
                    OutboxEntryStatus.PENDING, 0, now, oldPending);
            persistRaw(session, TEST_KIND, realmId, "owner-old", null, "co-mid",
                    OutboxEntryStatus.PENDING, 0, now, midPending);
            persistRaw(session, TEST_KIND, realmId, "owner-old-other", null, "co-other",
                    OutboxEntryStatus.PENDING, 0, now, otherOwnerPending);
            em(session).flush();
            em(session).clear();

            Map<OutboxEntryStatus, Instant> oldest = new OutboxStore(session)
                    .oldestCreatedAtPerStatusForOwner(TEST_KIND, "owner-old");
            Assertions.assertEquals(oldPending, oldest.get(OutboxEntryStatus.PENDING),
                    "PENDING oldest must be the target owner's earliest createdAt");
            Assertions.assertNull(oldest.get(OutboxEntryStatus.DEAD_LETTER));
        });
    }

    // -- Owner-scoped reads (POLL) --------------------------------------

    @Test
    public void lockPendingForOwner_returnsOnlyPendingForTheOwnerInArrivalOrder() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            persistRaw(session, TEST_KIND, realmId, "owner-lp", null, "first",
                    OutboxEntryStatus.PENDING, 0, now, now.minusSeconds(60));
            persistRaw(session, TEST_KIND, realmId, "owner-lp", null, "second",
                    OutboxEntryStatus.PENDING, 0, now, now.minusSeconds(30));
            persistRaw(session, TEST_KIND, realmId, "owner-lp", null, "delivered",
                    OutboxEntryStatus.DELIVERED, 1, now, now.minusSeconds(50));
            persistRaw(session, TEST_KIND, realmId, "owner-lp-other", null, "other",
                    OutboxEntryStatus.PENDING, 0, now, now.minusSeconds(70));
            em(session).flush();
            em(session).clear();

            List<OutboxEntryEntity> rows = new OutboxStore(session)
                    .lockPendingForOwner(TEST_KIND, "owner-lp", 10);
            Assertions.assertEquals(2, rows.size(),
                    "only PENDING rows for the target owner should be returned");
            Assertions.assertEquals("first", rows.get(0).getCorrelationId(),
                    "rows should be returned in arrival (createdAt ASC) order");
            Assertions.assertEquals("second", rows.get(1).getCorrelationId());
        });
    }

    @Test
    public void countForOwnerByStatus_filtersOnEntryKindOwnerAndStatus() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            persistRaw(session, TEST_KIND, realmId, "owner-c", null, "p1",
                    OutboxEntryStatus.PENDING, 0, now, now);
            persistRaw(session, TEST_KIND, realmId, "owner-c", null, "p2",
                    OutboxEntryStatus.PENDING, 0, now, now);
            persistRaw(session, TEST_KIND, realmId, "owner-c", null, "d1",
                    OutboxEntryStatus.DELIVERED, 1, now, now);
            persistRaw(session, OTHER_KIND, realmId, "owner-c", null, "other",
                    OutboxEntryStatus.PENDING, 0, now, now);
            em(session).flush();
            em(session).clear();

            long pending = new OutboxStore(session)
                    .countForOwnerByStatus(TEST_KIND, "owner-c", OutboxEntryStatus.PENDING);
            Assertions.assertEquals(2, pending,
                    "count must filter by entryKind, ownerId, and status");
        });
    }

    @Test
    public void ackPendingForOwner_transitionsMatchingRowsToDelivered() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            persistRaw(session, TEST_KIND, realmId, "owner-ack", null, "ack-1",
                    OutboxEntryStatus.PENDING, 0, now, now);
            persistRaw(session, TEST_KIND, realmId, "owner-ack", null, "ack-2",
                    OutboxEntryStatus.PENDING, 0, now, now);
            persistRaw(session, TEST_KIND, realmId, "owner-ack", null, "ack-other-status",
                    OutboxEntryStatus.DELIVERED, 1, now, now);
            persistRaw(session, TEST_KIND, realmId, "owner-ack-other", null, "ack-other-owner",
                    OutboxEntryStatus.PENDING, 0, now, now);
            em(session).flush();
            em(session).clear();

            // Include an unknown id and a wrong-owner id to confirm
            // they are silently skipped rather than erroring.
            var acked = new OutboxStore(session)
                    .ackPendingForOwner(TEST_KIND, "owner-ack",
                            List.of("ack-1", "ack-2", "ack-unknown", "ack-other-owner"));
            Assertions.assertEquals(2, acked.size());
            Assertions.assertTrue(acked.contains("ack-1"));
            Assertions.assertTrue(acked.contains("ack-2"));
        });
    }

    @Test
    public void nackPendingForOwner_transitionsMatchingRowsToDeadLetter() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            persistRaw(session, TEST_KIND, realmId, "owner-nack", null, "nack-1",
                    OutboxEntryStatus.PENDING, 0, now, now);
            persistRaw(session, TEST_KIND, realmId, "owner-nack", null, "nack-2",
                    OutboxEntryStatus.PENDING, 0, now, now);
            em(session).flush();
            em(session).clear();

            var nacked = new OutboxStore(session)
                    .nackPendingForOwner(TEST_KIND, "owner-nack",
                            Map.of("nack-1", "receiver rejected", "nack-2", "schema mismatch"));
            Assertions.assertEquals(2, nacked.size());

            em(session).flush();
            em(session).clear();

            for (String corr : List.of("nack-1", "nack-2")) {
                OutboxEntryEntity row = new OutboxStore(session)
                        .findByOwnerAndCorrelationId(TEST_KIND, "owner-nack", corr);
                Assertions.assertEquals(OutboxEntryStatus.DEAD_LETTER, row.getStatus());
                Assertions.assertNotNull(row.getLastError(),
                        "nack must record the receiver-supplied reason in lastError");
            }
        });
    }

    // -- Owner-scoped lifecycle -----------------------------------------

    @Test
    public void releaseHeldForOwner_transitionsHeldToPendingWithFreshNextAttemptAt() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            OutboxEntryEntity heldA = persistRaw(session, TEST_KIND, realmId, "owner-rel", null, "held-a",
                    OutboxEntryStatus.HELD, 0, now.minusSeconds(60), now);
            OutboxEntryEntity heldB = persistRaw(session, TEST_KIND, realmId, "owner-rel", null, "held-b",
                    OutboxEntryStatus.HELD, 0, now.minusSeconds(60), now);
            OutboxEntryEntity pending = persistRaw(session, TEST_KIND, realmId, "owner-rel", null, "pending",
                    OutboxEntryStatus.PENDING, 0, now.plusSeconds(60), now);
            em(session).flush();
            em(session).clear();

            int released = new OutboxStore(session).releaseHeldForOwner(TEST_KIND, "owner-rel");
            Assertions.assertEquals(2, released);

            em(session).flush();
            em(session).clear();

            for (String id : List.of(heldA.getId(), heldB.getId())) {
                OutboxEntryEntity after = findById(session, id);
                Assertions.assertEquals(OutboxEntryStatus.PENDING, after.getStatus(),
                        "HELD rows must transition back to PENDING");
            }
            Assertions.assertEquals(OutboxEntryStatus.PENDING,
                    findById(session, pending.getId()).getStatus(),
                    "already-PENDING rows must not be touched");
        });
    }

    @Test
    public void holdPendingForOwner_transitionsPendingToHeld() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            OutboxEntryEntity p = persistRaw(session, TEST_KIND, realmId, "owner-hold", null, "p",
                    OutboxEntryStatus.PENDING, 0, now, now);
            OutboxEntryEntity d = persistRaw(session, TEST_KIND, realmId, "owner-hold", null, "d",
                    OutboxEntryStatus.DELIVERED, 1, now, now);
            em(session).flush();
            em(session).clear();

            int held = new OutboxStore(session).holdPendingForOwner(TEST_KIND, "owner-hold");
            Assertions.assertEquals(1, held);

            em(session).flush();
            em(session).clear();

            Assertions.assertEquals(OutboxEntryStatus.HELD, findById(session, p.getId()).getStatus());
            Assertions.assertEquals(OutboxEntryStatus.DELIVERED, findById(session, d.getId()).getStatus(),
                    "terminal rows must not be touched");
        });
    }

    @Test
    public void deadLetterQueuedForOwner_transitionsPendingAndHeldToDeadLetter() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            OutboxEntryEntity p = persistRaw(session, TEST_KIND, realmId, "owner-dl", null, "p",
                    OutboxEntryStatus.PENDING, 0, now, now);
            OutboxEntryEntity h = persistRaw(session, TEST_KIND, realmId, "owner-dl", null, "h",
                    OutboxEntryStatus.HELD, 0, now, now);
            OutboxEntryEntity d = persistRaw(session, TEST_KIND, realmId, "owner-dl", null, "d",
                    OutboxEntryStatus.DELIVERED, 1, now, now);
            em(session).flush();
            em(session).clear();

            int updated = new OutboxStore(session)
                    .deadLetterQueuedForOwner(TEST_KIND, "owner-dl", "stream disabled");
            Assertions.assertEquals(2, updated);

            em(session).flush();
            em(session).clear();

            Assertions.assertEquals(OutboxEntryStatus.DEAD_LETTER, findById(session, p.getId()).getStatus());
            Assertions.assertEquals(OutboxEntryStatus.DEAD_LETTER, findById(session, h.getId()).getStatus());
            Assertions.assertEquals(OutboxEntryStatus.DELIVERED, findById(session, d.getId()).getStatus());
        });
    }

    @Test
    public void deadLetterQueuedForOwnerNotMatchingTypes_filtersOnEntryType() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            OutboxEntryEntity allowed = persistRawWithType(session, TEST_KIND, realmId, "owner-types",
                    "type-a", "allowed", OutboxEntryStatus.PENDING, 0, now, now);
            OutboxEntryEntity dropped = persistRawWithType(session, TEST_KIND, realmId, "owner-types",
                    "type-b", "dropped", OutboxEntryStatus.PENDING, 0, now, now);
            em(session).flush();
            em(session).clear();

            int updated = new OutboxStore(session)
                    .deadLetterQueuedForOwnerNotMatchingTypes(TEST_KIND, "owner-types",
                            java.util.Set.of("type-a"), "events_requested narrowed");
            Assertions.assertEquals(1, updated);

            em(session).flush();
            em(session).clear();

            Assertions.assertEquals(OutboxEntryStatus.PENDING, findById(session, allowed.getId()).getStatus(),
                    "rows whose entryType is in the allow-list must survive");
            Assertions.assertEquals(OutboxEntryStatus.DEAD_LETTER, findById(session, dropped.getId()).getStatus(),
                    "rows whose entryType is not in the allow-list must be dead-lettered");
        });
    }

    @Test
    public void deadLetterQueuedForOwnerNotMatchingTypes_emptyAllowListFallsBackToFullDeadLetter() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            persistRawWithType(session, TEST_KIND, realmId, "owner-empty", "type-a", "a",
                    OutboxEntryStatus.PENDING, 0, now, now);
            persistRawWithType(session, TEST_KIND, realmId, "owner-empty", "type-b", "b",
                    OutboxEntryStatus.PENDING, 0, now, now);
            em(session).flush();
            em(session).clear();

            int updated = new OutboxStore(session)
                    .deadLetterQueuedForOwnerNotMatchingTypes(TEST_KIND, "owner-empty",
                            java.util.Set.of(), "no events accepted");
            Assertions.assertEquals(2, updated,
                    "an empty allow-list must fall back to the unfiltered dead-letter variant");
        });
    }

    @Test
    public void migrateEntryKindForOwner_retargetsQueuedRowsAndLeavesTerminalRowsAlone() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();

            OutboxEntryEntity pending = persistRaw(session, TEST_KIND, realmId, "owner-mig", null,
                    "p", OutboxEntryStatus.PENDING, 0, now, now);
            OutboxEntryEntity held = persistRaw(session, TEST_KIND, realmId, "owner-mig", null,
                    "h", OutboxEntryStatus.HELD, 0, now, now);
            OutboxEntryEntity delivered = persistRaw(session, TEST_KIND, realmId, "owner-mig", null,
                    "d", OutboxEntryStatus.DELIVERED, 1, now, now);
            OutboxEntryEntity deadLetter = persistRaw(session, TEST_KIND, realmId, "owner-mig", null,
                    "dl", OutboxEntryStatus.DEAD_LETTER, 8, now, now);
            OutboxEntryEntity otherOwner = persistRaw(session, TEST_KIND, realmId, "owner-mig-other", null,
                    "other", OutboxEntryStatus.PENDING, 0, now, now);
            em(session).flush();
            em(session).clear();

            int migrated = new OutboxStore(session)
                    .migrateEntryKindForOwner(TEST_KIND, OTHER_KIND, "owner-mig");
            Assertions.assertEquals(2, migrated,
                    "only PENDING + HELD rows for the target owner should migrate");

            em(session).flush();
            em(session).clear();

            Assertions.assertEquals(OTHER_KIND, findById(session, pending.getId()).getEntryKind());
            Assertions.assertEquals(OTHER_KIND, findById(session, held.getId()).getEntryKind());
            Assertions.assertEquals(TEST_KIND, findById(session, delivered.getId()).getEntryKind(),
                    "DELIVERED rows are terminal and must not migrate");
            Assertions.assertEquals(TEST_KIND, findById(session, deadLetter.getId()).getEntryKind(),
                    "DEAD_LETTER rows are terminal and must not migrate");
            Assertions.assertEquals(TEST_KIND, findById(session, otherOwner.getId()).getEntryKind(),
                    "rows for other owners must not migrate");
        });
    }

    // -- Admin / cascade deletes ----------------------------------------

    @Test
    public void deleteByRealm_removesAllRowsForRealmAcrossStatuses() {
        final String realmId = testRealmId;
        final String otherRealmId = UUID.randomUUID().toString();
        try {
            runOnServer.run(session -> {
                Instant now = Instant.now();
                persistRaw(session, TEST_KIND, realmId, "c", null, "p",
                        OutboxEntryStatus.PENDING, 0, now, now);
                persistRaw(session, TEST_KIND, realmId, "c", null, "d",
                        OutboxEntryStatus.DELIVERED, 1, now, now);
                persistRaw(session, TEST_KIND, realmId, "c", null, "dl",
                        OutboxEntryStatus.DEAD_LETTER, 8, now, now);
                OutboxEntryEntity keeper = persistRaw(session, TEST_KIND, otherRealmId, "c2", null,
                        "other", OutboxEntryStatus.PENDING, 0, now, now);
                em(session).flush();
                em(session).clear();

                int deleted = new OutboxStore(session).deleteByRealm(TEST_KIND, realmId);
                Assertions.assertEquals(3, deleted,
                        "deleteByRealm must remove every row for the realm regardless of status");

                em(session).flush();
                em(session).clear();

                Assertions.assertNotNull(findById(session, keeper.getId()),
                        "rows from other realms must not be touched");
            });
        } finally {
            runOnServer.run(session -> new OutboxStore(session).deleteByRealm(TEST_KIND, otherRealmId));
        }
    }

    @Test
    public void deleteByRealmAndStatus_deletesOnlyMatchingStatusInRealm() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            OutboxEntryEntity dl1 = persistRaw(session, TEST_KIND, realmId, "c1", null, "dl-1",
                    OutboxEntryStatus.DEAD_LETTER, 8, now, now);
            OutboxEntryEntity dl2 = persistRaw(session, TEST_KIND, realmId, "c1", null, "dl-2",
                    OutboxEntryStatus.DEAD_LETTER, 8, now, now);
            OutboxEntryEntity pending = persistRaw(session, TEST_KIND, realmId, "c1", null, "pending",
                    OutboxEntryStatus.PENDING, 0, now, now);
            em(session).flush();
            em(session).clear();

            int deleted = new OutboxStore(session)
                    .deleteByRealmAndStatus(TEST_KIND, realmId, OutboxEntryStatus.DEAD_LETTER);
            Assertions.assertEquals(2, deleted);

            em(session).flush();
            em(session).clear();

            Assertions.assertNull(findById(session, dl1.getId()));
            Assertions.assertNull(findById(session, dl2.getId()));
            Assertions.assertNotNull(findById(session, pending.getId()),
                    "rows in other statuses must survive");
        });
    }

    @Test
    public void deleteByRealmAndStatusOlderThan_deletesOnlyOldEnoughMatchingRows() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            OutboxEntryEntity oldDl = persistRaw(session, TEST_KIND, realmId, "c-age", null, "old-dl",
                    OutboxEntryStatus.DEAD_LETTER, 8, now, now.minus(Duration.ofDays(10)));
            OutboxEntryEntity freshDl = persistRaw(session, TEST_KIND, realmId, "c-age", null, "fresh-dl",
                    OutboxEntryStatus.DEAD_LETTER, 8, now, now.minusSeconds(60));
            em(session).flush();
            em(session).clear();

            int deleted = new OutboxStore(session)
                    .deleteByRealmAndStatusOlderThan(TEST_KIND, realmId,
                            OutboxEntryStatus.DEAD_LETTER, now.minus(Duration.ofDays(7)));
            Assertions.assertEquals(1, deleted);

            em(session).flush();
            em(session).clear();

            Assertions.assertNull(findById(session, oldDl.getId()));
            Assertions.assertNotNull(findById(session, freshDl.getId()));
        });
    }

    @Test
    public void deleteByOwnerAndStatus_deletesOnlyMatchingStatusForOwner() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            OutboxEntryEntity targetDl = persistRaw(session, TEST_KIND, realmId, "owner-target", null,
                    "td", OutboxEntryStatus.DEAD_LETTER, 8, now, now);
            OutboxEntryEntity targetPending = persistRaw(session, TEST_KIND, realmId, "owner-target", null,
                    "tp", OutboxEntryStatus.PENDING, 0, now, now);
            OutboxEntryEntity siblingDl = persistRaw(session, TEST_KIND, realmId, "owner-sibling", null,
                    "sd", OutboxEntryStatus.DEAD_LETTER, 8, now, now);
            em(session).flush();
            em(session).clear();

            int deleted = new OutboxStore(session)
                    .deleteByOwnerAndStatus(TEST_KIND, "owner-target", OutboxEntryStatus.DEAD_LETTER);
            Assertions.assertEquals(1, deleted);

            em(session).flush();
            em(session).clear();

            Assertions.assertNull(findById(session, targetDl.getId()));
            Assertions.assertNotNull(findById(session, targetPending.getId()),
                    "rows in other statuses for the target owner must survive");
            Assertions.assertNotNull(findById(session, siblingDl.getId()),
                    "DEAD_LETTER rows for sibling owners must survive");
        });
    }

    @Test
    public void deleteQueuedByRealm_deletesPendingAndHeldOnlyInRealm() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            OutboxEntryEntity pending = persistRaw(session, TEST_KIND, realmId, "c", null, "p",
                    OutboxEntryStatus.PENDING, 0, now, now);
            OutboxEntryEntity held = persistRaw(session, TEST_KIND, realmId, "c", null, "h",
                    OutboxEntryStatus.HELD, 0, now, now);
            OutboxEntryEntity delivered = persistRaw(session, TEST_KIND, realmId, "c", null, "d",
                    OutboxEntryStatus.DELIVERED, 1, now, now);
            OutboxEntryEntity deadLetter = persistRaw(session, TEST_KIND, realmId, "c", null, "dl",
                    OutboxEntryStatus.DEAD_LETTER, 8, now, now);
            em(session).flush();
            em(session).clear();

            int deleted = new OutboxStore(session).deleteQueuedByRealm(TEST_KIND, realmId);
            Assertions.assertEquals(2, deleted);

            em(session).flush();
            em(session).clear();

            Assertions.assertNull(findById(session, pending.getId()));
            Assertions.assertNull(findById(session, held.getId()));
            Assertions.assertNotNull(findById(session, delivered.getId()));
            Assertions.assertNotNull(findById(session, deadLetter.getId()));
        });
    }

    @Test
    public void deleteQueuedByOwner_deletesPendingAndHeldOnlyForOwner() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            OutboxEntryEntity targetP = persistRaw(session, TEST_KIND, realmId, "owner-target", null,
                    "tp", OutboxEntryStatus.PENDING, 0, now, now);
            OutboxEntryEntity targetH = persistRaw(session, TEST_KIND, realmId, "owner-target", null,
                    "th", OutboxEntryStatus.HELD, 0, now, now);
            OutboxEntryEntity siblingP = persistRaw(session, TEST_KIND, realmId, "owner-sibling", null,
                    "sp", OutboxEntryStatus.PENDING, 0, now, now);
            em(session).flush();
            em(session).clear();

            int deleted = new OutboxStore(session).deleteQueuedByOwner(TEST_KIND, "owner-target");
            Assertions.assertEquals(2, deleted);

            em(session).flush();
            em(session).clear();

            Assertions.assertNull(findById(session, targetP.getId()));
            Assertions.assertNull(findById(session, targetH.getId()));
            Assertions.assertNotNull(findById(session, siblingP.getId()),
                    "queued rows for sibling owners must survive");
        });
    }

    // -- Retention purges -----------------------------------------------

    @Test
    public void purgeDeliveredOlderThan_purgesOnlyOldDeliveredRows() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();

            OutboxEntryEntity oldDelivered = persistRaw(session, TEST_KIND, realmId, "c-p", null, "old",
                    OutboxEntryStatus.DELIVERED, 1, now.minusSeconds(60), now.minus(Duration.ofDays(2)));
            oldDelivered.setDeliveredAt(now.minus(Duration.ofDays(2)));

            OutboxEntryEntity freshDelivered = persistRaw(session, TEST_KIND, realmId, "c-p", null, "fresh",
                    OutboxEntryStatus.DELIVERED, 1, now.minusSeconds(60), now.minusSeconds(60));
            freshDelivered.setDeliveredAt(now.minusSeconds(60));

            OutboxEntryEntity deadLetter = persistRaw(session, TEST_KIND, realmId, "c-p", null, "dl",
                    OutboxEntryStatus.DEAD_LETTER, 8, now.minusSeconds(60), now.minus(Duration.ofDays(5)));

            em(session).flush();
            em(session).clear();

            int purged = new OutboxStore(session)
                    .purgeDeliveredOlderThan(TEST_KIND, now.minus(Duration.ofDays(1)));
            Assertions.assertEquals(1, purged);

            em(session).flush();
            em(session).clear();

            Assertions.assertNull(findById(session, oldDelivered.getId()),
                    "delivered rows older than the cutoff must be purged");
            Assertions.assertNotNull(findById(session, freshDelivered.getId()),
                    "delivered rows newer than the cutoff must survive");
            Assertions.assertNotNull(findById(session, deadLetter.getId()),
                    "dead-letter rows must not be touched by the delivered-rows purge");
        });
    }

    @Test
    public void purgeDeadLetterOlderThan_purgesOnlyOldDeadLetterRows() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();

            OutboxEntryEntity oldDead = persistRaw(session, TEST_KIND, realmId, "c-q", null, "old",
                    OutboxEntryStatus.DEAD_LETTER, 8, now.minusSeconds(60), now.minus(Duration.ofDays(40)));
            OutboxEntryEntity freshDead = persistRaw(session, TEST_KIND, realmId, "c-q", null, "fresh",
                    OutboxEntryStatus.DEAD_LETTER, 8, now.minusSeconds(60), now.minusSeconds(60));
            OutboxEntryEntity oldDelivered = persistRaw(session, TEST_KIND, realmId, "c-q", null, "delivered",
                    OutboxEntryStatus.DELIVERED, 1, now.minusSeconds(60), now.minus(Duration.ofDays(40)));
            oldDelivered.setDeliveredAt(now.minus(Duration.ofDays(40)));

            em(session).flush();
            em(session).clear();

            int purged = new OutboxStore(session)
                    .purgeDeadLetterOlderThan(TEST_KIND, now.minus(Duration.ofDays(30)));
            Assertions.assertEquals(1, purged);

            em(session).flush();
            em(session).clear();

            Assertions.assertNull(findById(session, oldDead.getId()));
            Assertions.assertNotNull(findById(session, freshDead.getId()));
            Assertions.assertNotNull(findById(session, oldDelivered.getId()),
                    "delivered rows must not be touched by the dead-letter purge");
        });
    }

    // -- Cleanup task ----------------------------------------------------

    @Test
    public void cleanupTask_realmScopeDeletesEveryRowForTheRealm() {
        final String realmId = testRealmId;
        final String otherRealmId = UUID.randomUUID().toString();
        try {
            runOnServer.run(session -> {
                Instant now = Instant.now();
                for (int i = 0; i < 4; i++) {
                    persistRaw(session, TEST_KIND, realmId, "c" + (i % 2), null, "task-" + i,
                            OutboxEntryStatus.PENDING, 0, now, now);
                }
                persistRaw(session, TEST_KIND, otherRealmId, "co", null, "other",
                        OutboxEntryStatus.PENDING, 0, now, now);
            });

            runOnServer.run(session -> new OutboxCleanupTask(session.getKeycloakSessionFactory(),
                    OutboxStore::new, TEST_KIND, OutboxCleanupTask.Scope.REALM, realmId).run());

            runOnServer.run(session -> {
                int remaining = new OutboxStore(session).deleteByRealm(TEST_KIND, realmId);
                Assertions.assertEquals(0, remaining,
                        "realm-scoped cleanup task must drain every row in the realm");
            });
        } finally {
            runOnServer.run(session -> new OutboxStore(session).deleteByRealm(TEST_KIND, otherRealmId));
        }
    }

    @Test
    public void cleanupTask_ownerScopeDeletesEveryRowForTheOwner() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            for (int i = 0; i < 5; i++) {
                persistRaw(session, TEST_KIND, realmId, "owner-task", null, "ot-" + i,
                        OutboxEntryStatus.PENDING, 0, now, now);
            }
            OutboxEntryEntity keeper = persistRaw(session, TEST_KIND, realmId, "owner-keeper", null,
                    "keeper", OutboxEntryStatus.PENDING, 0, now, now);
            em(session).flush();
            em(session).clear();
            // assertion happens after the cleanup runs in its own session
            Assertions.assertNotNull(keeper.getId());
        });

        runOnServer.run(session -> new OutboxCleanupTask(session.getKeycloakSessionFactory(),
                OutboxStore::new, TEST_KIND, OutboxCleanupTask.Scope.OWNER, "owner-task").run());

        runOnServer.run(session -> {
            int remaining = new OutboxStore(session).deleteByOwner(TEST_KIND, "owner-task");
            Assertions.assertEquals(0, remaining,
                    "owner-scoped cleanup task must drain every row for the owner");
            // The keeper should still be there since it's a different owner.
            int keeperRows = new OutboxStore(session).deleteByOwner(TEST_KIND, "owner-keeper");
            Assertions.assertEquals(1, keeperRows,
                    "rows for other owners must not be touched");
        });
    }

    // -- helpers (static so the serialized lambdas don't capture the
    //    enclosing test instance on the remote side) -------------------

    private static EntityManager em(KeycloakSession session) {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    private static OutboxEntryEntity findById(KeycloakSession session, String id) {
        return em(session).find(OutboxEntryEntity.class, id);
    }

    private static OutboxEntryEntity persistRaw(KeycloakSession session,
                                                String entryKind,
                                                String realmId,
                                                String ownerId,
                                                String containerId,
                                                String correlationId,
                                                OutboxEntryStatus status,
                                                int attempts,
                                                Instant nextAttemptAt,
                                                Instant createdAt) {
        return persistRawWithType(session, entryKind, realmId, ownerId, containerId,
                "test.event", correlationId, status, attempts, nextAttemptAt, createdAt);
    }

    private static OutboxEntryEntity persistRawWithType(KeycloakSession session,
                                                        String entryKind,
                                                        String realmId,
                                                        String ownerId,
                                                        String entryType,
                                                        String correlationId,
                                                        OutboxEntryStatus status,
                                                        int attempts,
                                                        Instant nextAttemptAt,
                                                        Instant createdAt) {
        return persistRawWithType(session, entryKind, realmId, ownerId, null, entryType,
                correlationId, status, attempts, nextAttemptAt, createdAt);
    }

    private static OutboxEntryEntity persistRawWithType(KeycloakSession session,
                                                        String entryKind,
                                                        String realmId,
                                                        String ownerId,
                                                        String containerId,
                                                        String entryType,
                                                        String correlationId,
                                                        OutboxEntryStatus status,
                                                        int attempts,
                                                        Instant nextAttemptAt,
                                                        Instant createdAt) {
        OutboxEntryEntity e = new OutboxEntryEntity();
        e.setId(UUID.randomUUID().toString());
        e.setEntryKind(entryKind);
        e.setRealmId(realmId);
        e.setOwnerId(ownerId);
        e.setContainerId(containerId);
        e.setCorrelationId(correlationId);
        e.setEntryType(entryType);
        e.setPayload("encoded-" + correlationId);
        e.setStatus(status);
        e.setAttempts(attempts);
        e.setNextAttemptAt(nextAttemptAt);
        e.setCreatedAt(createdAt);
        em(session).persist(e);
        return e;
    }

    public static class OutboxStoreServerConfig extends DefaultKeycloakServerConfig {
        // Outbox infrastructure (entity, store, named queries, schema)
        // lives in core model/jpa, so no feature flag is required to
        // exercise it — the table exists in every server. Tests run
        // against the synthetic TEST_KIND so no consumer registration
        // is needed either.
    }
}
