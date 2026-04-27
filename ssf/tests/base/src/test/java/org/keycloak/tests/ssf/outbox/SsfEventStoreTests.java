package org.keycloak.tests.ssf.outbox;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.keycloak.common.Profile;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.transmitter.outbox.SsfOutboxCleanupTask;
import org.keycloak.ssf.transmitter.outbox.SsfEventEntity;
import org.keycloak.ssf.transmitter.outbox.SsfEventStatus;
import org.keycloak.ssf.transmitter.store.SsfEventStore;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link SsfEventStore}. Each test runs a lambda
 * against a real Keycloak session via {@code runOnServer}, exercising
 * the store against the actual JPA provider and the SSF outbox schema.
 *
 * <p>Tests are isolated by pinning each one to a unique synthetic
 * {@code realmId}; teardown calls {@link SsfEventStore#deleteByRealm}
 * to remove every row enqueued during the test. No real Keycloak
 * realms/clients are created — the store treats realmId/clientId as
 * opaque strings, so synthetic UUIDs exercise the full path.
 */
@KeycloakIntegrationTest(config = SsfEventStoreTests.OutboxStoreServerConfig.class)
public class SsfEventStoreTests {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    // REALM_ID is VARCHAR(36), so we use a bare UUID — exactly 36 chars.
    // Prefixed labels like "outbox-test-realm-<uuid>" overflow the column.
    private final String testRealmId = UUID.randomUUID().toString();

    @AfterEach
    public void cleanupRealm() {
        final String realmId = testRealmId;
        runOnServer.run(session -> new SsfEventStore(session).deleteByRealm(realmId));
    }

    @Test
    public void enqueuePendingPush_persistsRowWithExpectedDefaults() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            SsfEventStore store = new SsfEventStore(session);
            String id = store.enqueuePendingPush(realmId, "client-a", "stream-a",
                    "jti-happy-path", "caep.credentialChange", "signed-set");
            Assertions.assertNotNull(id);

            em(session).flush();
            em(session).clear();

            SsfEventEntity row = findById(session, id);
            Assertions.assertNotNull(row, "row should have been persisted");
            Assertions.assertEquals(realmId, row.getRealmId());
            Assertions.assertEquals("client-a", row.getClientId());
            Assertions.assertEquals("stream-a", row.getStreamId());
            Assertions.assertEquals("jti-happy-path", row.getJti());
            Assertions.assertEquals("caep.credentialChange", row.getEventType());
            Assertions.assertEquals("signed-set", row.getEncodedSet());
            Assertions.assertEquals(SsfEventEntity.DELIVERY_METHOD_PUSH, row.getDeliveryMethod());
            Assertions.assertEquals(SsfEventStatus.PENDING, row.getStatus());
            Assertions.assertEquals(0, row.getAttempts());
            Assertions.assertNotNull(row.getCreatedAt());
            Assertions.assertNotNull(row.getNextAttemptAt());
            Assertions.assertNull(row.getDeliveredAt());
            Assertions.assertNull(row.getLastError());
        });
    }

    @Test
    public void enqueuePendingPush_deduplicatesOnClientAndJti() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            SsfEventStore store = new SsfEventStore(session);
            String first = store.enqueuePendingPush(realmId, "client-dedup", "stream-a",
                    "jti-dedup", "caep.credentialChange", "encoded-1");
            String second = store.enqueuePendingPush(realmId, "client-dedup", "stream-b",
                    "jti-dedup", "caep.credentialChange", "encoded-2");

            Assertions.assertEquals(first, second,
                    "second enqueue for the same (clientId, jti) should return the original id");

            em(session).flush();
            em(session).clear();

            long count = em(session)
                    .createNamedQuery("SsfEventEntity.countByClientAndStatus", Long.class)
                    .setParameter("clientId", "client-dedup")
                    .setParameter("status", SsfEventStatus.PENDING)
                    .getSingleResult();
            Assertions.assertEquals(1, count, "dedup must not create a second row");

            SsfEventEntity row = findById(session, first);
            Assertions.assertEquals("encoded-1", row.getEncodedSet(),
                    "original encodedSet must be preserved across dedup");
            Assertions.assertEquals("stream-a", row.getStreamId(),
                    "original streamId must be preserved across dedup");
        });
    }

    @Test
    public void lockDueForPush_returnsOnlyRowsWithNextAttemptAtInThePast() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            persistRaw(session, realmId, "client-due", "stream-a", "jti-past",
                    SsfEventStatus.PENDING, 0, now.minusSeconds(10), now.minusSeconds(30));
            persistRaw(session, realmId, "client-due", "stream-b", "jti-future",
                    SsfEventStatus.PENDING, 0, now.plusSeconds(60), now.minusSeconds(30));
            em(session).flush();
            em(session).clear();

            List<SsfEventEntity> due = new SsfEventStore(session).lockDueForPush(10);
            Assertions.assertEquals(1, due.size(),
                    "only rows whose nextAttemptAt is in the past should be returned");
            Assertions.assertEquals("jti-past", due.get(0).getJti());
        });
    }

    @Test
    public void lockDueForPush_respectsBatchLimit() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            for (int i = 0; i < 5; i++) {
                persistRaw(session, realmId, "client-batch", "stream-a", "jti-b-" + i,
                        SsfEventStatus.PENDING, 0,
                        now.minusSeconds(i + 1L), now.minusSeconds(10));
            }
            em(session).flush();
            em(session).clear();

            List<SsfEventEntity> batch = new SsfEventStore(session).lockDueForPush(3);
            Assertions.assertEquals(3, batch.size());
        });
    }

    @Test
    public void markDelivered_setsStatusAndDeliveredAtAndClearsLastError() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            SsfEventEntity row = persistRaw(session, realmId, "client-d", "stream-a",
                    "jti-deliver", SsfEventStatus.PENDING, 2,
                    Instant.now(), Instant.now());
            row.setLastError("earlier transient failure");
            em(session).flush();
            em(session).clear();

            new SsfEventStore(session).markDelivered(findById(session, row.getId()));
            em(session).flush();
            em(session).clear();

            SsfEventEntity after = findById(session, row.getId());
            Assertions.assertEquals(SsfEventStatus.DELIVERED, after.getStatus());
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
            SsfEventEntity row = persistRaw(session, realmId, "client-f", "stream-a",
                    "jti-fail", SsfEventStatus.PENDING, 2,
                    now.minusSeconds(1), now);
            em(session).flush();
            em(session).clear();

            // Truncate to microseconds up front — PostgreSQL TIMESTAMP
            // stores micros and rounds half-up, while H2 preserves nanos.
            // Truncating before the write means every backend reads back
            // the exact same Instant we persisted.
            Instant nextAttempt = now.plus(Duration.ofMinutes(5)).truncatedTo(ChronoUnit.MICROS);
            String hugeError = "x".repeat(3000);
            new SsfEventStore(session)
                    .recordFailure(findById(session, row.getId()), nextAttempt, hugeError);
            em(session).flush();
            em(session).clear();

            SsfEventEntity after = findById(session, row.getId());
            Assertions.assertEquals(SsfEventStatus.PENDING, after.getStatus(),
                    "recordFailure keeps the row in PENDING for the next drainer tick");
            Assertions.assertEquals(3, after.getAttempts(), "attempts must be incremented");
            Assertions.assertEquals(nextAttempt, after.getNextAttemptAt());
            Assertions.assertNotNull(after.getLastError());
            Assertions.assertTrue(after.getLastError().length() <= 2048,
                    "lastError must be truncated to the column width");
            Assertions.assertTrue(after.getLastError().endsWith("..."),
                    "truncated lastError must carry an ellipsis marker");
        });
    }

    @Test
    public void markDeadLetter_setsStatusAndIncrementsAttemptsAndStoresError() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            SsfEventEntity row = persistRaw(session, realmId, "client-dl", "stream-a",
                    "jti-dead", SsfEventStatus.PENDING, 8,
                    Instant.now().minusSeconds(1), Instant.now());
            em(session).flush();
            em(session).clear();

            new SsfEventStore(session)
                    .markDeadLetter(findById(session, row.getId()), "giving up after 8 retries");
            em(session).flush();
            em(session).clear();

            SsfEventEntity after = findById(session, row.getId());
            Assertions.assertEquals(SsfEventStatus.DEAD_LETTER, after.getStatus());
            Assertions.assertEquals(9, after.getAttempts(),
                    "markDeadLetter bumps attempts so the terminal count reflects the final try");
            Assertions.assertEquals("giving up after 8 retries", after.getLastError());
        });
    }

    @Test
    public void purgeDeliveredOlderThan_purgesOnlyOldDeliveredRows() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();

            SsfEventEntity oldDelivered = persistRaw(session, realmId, "client-p", "stream-a",
                    "jti-old", SsfEventStatus.DELIVERED, 1,
                    now.minusSeconds(60), now.minus(Duration.ofDays(2)));
            oldDelivered.setDeliveredAt(now.minus(Duration.ofDays(2)));

            SsfEventEntity freshDelivered = persistRaw(session, realmId, "client-p", "stream-b",
                    "jti-fresh", SsfEventStatus.DELIVERED, 1,
                    now.minusSeconds(60), now.minusSeconds(60));
            freshDelivered.setDeliveredAt(now.minusSeconds(60));

            SsfEventEntity deadLetter = persistRaw(session, realmId, "client-p", "stream-c",
                    "jti-dl", SsfEventStatus.DEAD_LETTER, 8,
                    now.minusSeconds(60), now.minus(Duration.ofDays(5)));

            em(session).flush();
            em(session).clear();

            int purged = new SsfEventStore(session)
                    .purgeDeliveredOlderThan(now.minus(Duration.ofDays(1)));
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

            SsfEventEntity oldDead = persistRaw(session, realmId, "client-q", "stream-a",
                    "jti-old-dl", SsfEventStatus.DEAD_LETTER, 8,
                    now.minusSeconds(60), now.minus(Duration.ofDays(40)));

            SsfEventEntity freshDead = persistRaw(session, realmId, "client-q", "stream-b",
                    "jti-fresh-dl", SsfEventStatus.DEAD_LETTER, 8,
                    now.minusSeconds(60), now.minusSeconds(60));

            SsfEventEntity oldDelivered = persistRaw(session, realmId, "client-q", "stream-c",
                    "jti-delivered", SsfEventStatus.DELIVERED, 1,
                    now.minusSeconds(60), now.minus(Duration.ofDays(40)));
            oldDelivered.setDeliveredAt(now.minus(Duration.ofDays(40)));

            em(session).flush();
            em(session).clear();

            int purged = new SsfEventStore(session)
                    .purgeDeadLetterOlderThan(now.minus(Duration.ofDays(30)));
            Assertions.assertEquals(1, purged);

            em(session).flush();
            em(session).clear();

            Assertions.assertNull(findById(session, oldDead.getId()),
                    "dead-letter rows older than the cutoff must be purged");
            Assertions.assertNotNull(findById(session, freshDead.getId()),
                    "dead-letter rows newer than the cutoff must survive");
            Assertions.assertNotNull(findById(session, oldDelivered.getId()),
                    "delivered rows must not be touched by the dead-letter purge");
        });
    }

    @Test
    public void migrateDeliveryMethodForClient_retargetsQueuedRowsAndLeavesTerminalRowsAlone() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();

            SsfEventEntity pending = persistRaw(session, realmId, "client-migrate", "stream-a",
                    "jti-migrate-pending", SsfEventStatus.PENDING, 0,
                    now.minusSeconds(5), now.minusSeconds(5));

            SsfEventEntity held = persistRaw(session, realmId, "client-migrate", "stream-a",
                    "jti-migrate-held", SsfEventStatus.HELD, 0,
                    now.minusSeconds(5), now.minusSeconds(5));

            SsfEventEntity delivered = persistRaw(session, realmId, "client-migrate", "stream-a",
                    "jti-migrate-delivered", SsfEventStatus.DELIVERED, 1,
                    now.minusSeconds(5), now.minusSeconds(5));
            delivered.setDeliveredAt(now.minusSeconds(1));

            SsfEventEntity deadLetter = persistRaw(session, realmId, "client-migrate", "stream-a",
                    "jti-migrate-dead", SsfEventStatus.DEAD_LETTER, 8,
                    now.minusSeconds(5), now.minusSeconds(5));

            SsfEventEntity otherClient = persistRaw(session, realmId, "client-other", "stream-b",
                    "jti-migrate-other", SsfEventStatus.PENDING, 0,
                    now.minusSeconds(5), now.minusSeconds(5));

            em(session).flush();
            em(session).clear();

            int migrated = new SsfEventStore(session)
                    .migrateDeliveryMethodForClient("client-migrate",
                            SsfEventEntity.DELIVERY_METHOD_POLL);
            Assertions.assertEquals(2, migrated,
                    "only PENDING + HELD rows for the target client should migrate");

            em(session).flush();
            em(session).clear();

            Assertions.assertEquals(SsfEventEntity.DELIVERY_METHOD_POLL,
                    findById(session, pending.getId()).getDeliveryMethod(),
                    "PENDING rows must be retargeted to the new delivery method");
            Assertions.assertEquals(SsfEventEntity.DELIVERY_METHOD_POLL,
                    findById(session, held.getId()).getDeliveryMethod(),
                    "HELD rows must be retargeted to the new delivery method");

            Assertions.assertEquals(SsfEventEntity.DELIVERY_METHOD_PUSH,
                    findById(session, delivered.getId()).getDeliveryMethod(),
                    "DELIVERED rows are terminal and must not be migrated");
            Assertions.assertEquals(SsfEventEntity.DELIVERY_METHOD_PUSH,
                    findById(session, deadLetter.getId()).getDeliveryMethod(),
                    "DEAD_LETTER rows are terminal and must not be migrated");

            Assertions.assertEquals(SsfEventEntity.DELIVERY_METHOD_PUSH,
                    findById(session, otherClient.getId()).getDeliveryMethod(),
                    "rows for other clients must not be migrated");
        });
    }

    @Test
    public void migrateDeliveryMethodForClient_isIdempotentWhenTargetMethodAlreadyInUse() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            persistRaw(session, realmId, "client-noop", "stream-a", "jti-noop",
                    SsfEventStatus.PENDING, 0, now, now);
            em(session).flush();
            em(session).clear();

            int migrated = new SsfEventStore(session)
                    .migrateDeliveryMethodForClient("client-noop",
                            SsfEventEntity.DELIVERY_METHOD_PUSH);
            Assertions.assertEquals(0, migrated,
                    "rows already using the target delivery method must not be counted");
        });
    }

    @Test
    public void deleteByRealm_removesAllRowsForRealmAcrossStatuses() {
        final String realmId = testRealmId;
        final String otherRealmId = UUID.randomUUID().toString();
        try {
            runOnServer.run(session -> {
                Instant now = Instant.now();
                persistRaw(session, realmId, "c1", "s1", "jti-pending",
                        SsfEventStatus.PENDING, 0, now, now);
                persistRaw(session, realmId, "c1", "s1", "jti-delivered",
                        SsfEventStatus.DELIVERED, 1, now, now);
                persistRaw(session, realmId, "c1", "s1", "jti-dl",
                        SsfEventStatus.DEAD_LETTER, 8, now, now);

                SsfEventEntity keeper = persistRaw(session, otherRealmId, "c2", "s2",
                        "jti-other-realm", SsfEventStatus.PENDING, 0, now, now);

                em(session).flush();
                em(session).clear();

                int deleted = new SsfEventStore(session).deleteByRealm(realmId);
                Assertions.assertEquals(3, deleted,
                        "deleteByRealm must remove every row for the realm regardless of status");

                em(session).flush();
                em(session).clear();

                Assertions.assertNotNull(findById(session, keeper.getId()),
                        "rows from other realms must not be touched");
            });
        } finally {
            runOnServer.run(session -> new SsfEventStore(session).deleteByRealm(otherRealmId));
        }
    }

    @Test
    public void deleteBatchByClient_stopsAtBatchSizeAndReportsDeletedCount() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            for (int i = 0; i < 7; i++) {
                persistRaw(session, realmId, "client-batch", "stream-a", "jti-batch-" + i,
                        SsfEventStatus.PENDING, 0, now, now);
            }
            SsfEventEntity otherClient = persistRaw(session, realmId, "client-other",
                    "stream-other", "jti-batch-other", SsfEventStatus.PENDING, 0, now, now);

            em(session).flush();
            em(session).clear();

            SsfEventStore store = new SsfEventStore(session);
            int firstBatch = store.deleteBatchByClient("client-batch", 3);
            Assertions.assertEquals(3, firstBatch,
                    "first batch must delete exactly batchSize rows");

            em(session).flush();
            em(session).clear();

            int secondBatch = store.deleteBatchByClient("client-batch", 3);
            Assertions.assertEquals(3, secondBatch);

            em(session).flush();
            em(session).clear();

            int thirdBatch = store.deleteBatchByClient("client-batch", 3);
            Assertions.assertEquals(1, thirdBatch,
                    "final batch must only delete the remaining row");

            em(session).flush();
            em(session).clear();

            int fourthBatch = store.deleteBatchByClient("client-batch", 3);
            Assertions.assertEquals(0, fourthBatch,
                    "once drained the batch must return 0 so the caller can stop");

            Assertions.assertNotNull(findById(session, otherClient.getId()),
                    "rows for other clients must not be touched by a scoped batch delete");
        });
    }

    @Test
    public void deleteBatchByClient_deletesAcrossAllStatuses() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant now = Instant.now();
            // Async client-removed cleanup is scoped to the client, not a
            // single status — the rows are obsolete regardless of their
            // current state (unlike stream-disable, which must preserve
            // DELIVERED + DEAD_LETTER for audit/dedup).
            persistRaw(session, realmId, "client-all", "stream-a", "jti-all-pending",
                    SsfEventStatus.PENDING, 0, now, now);
            persistRaw(session, realmId, "client-all", "stream-a", "jti-all-held",
                    SsfEventStatus.HELD, 0, now, now);
            persistRaw(session, realmId, "client-all", "stream-a", "jti-all-delivered",
                    SsfEventStatus.DELIVERED, 1, now, now);
            persistRaw(session, realmId, "client-all", "stream-a", "jti-all-dead",
                    SsfEventStatus.DEAD_LETTER, 8, now, now);

            em(session).flush();
            em(session).clear();

            SsfEventStore store = new SsfEventStore(session);
            int deleted = store.deleteBatchByClient("client-all", 100);
            Assertions.assertEquals(4, deleted,
                    "batched delete must remove every row for the client regardless of status");
        });
    }

    @Test
    public void deleteBatchByRealm_respectsBatchSizeAndIsolatesOtherRealms() {
        final String realmId = testRealmId;
        final String otherRealmId = UUID.randomUUID().toString();
        try {
            runOnServer.run(session -> {
                Instant now = Instant.now();
                for (int i = 0; i < 5; i++) {
                    persistRaw(session, realmId, "c" + (i % 2), "stream-r", "jti-realm-batch-" + i,
                            SsfEventStatus.PENDING, 0, now, now);
                }
                SsfEventEntity keeper = persistRaw(session, otherRealmId, "c-other",
                        "stream-other", "jti-other-realm", SsfEventStatus.PENDING, 0, now, now);

                em(session).flush();
                em(session).clear();

                SsfEventStore store = new SsfEventStore(session);
                int firstBatch = store.deleteBatchByRealm(realmId, 2);
                Assertions.assertEquals(2, firstBatch);

                em(session).flush();
                em(session).clear();

                int secondBatch = store.deleteBatchByRealm(realmId, 2);
                Assertions.assertEquals(2, secondBatch);

                em(session).flush();
                em(session).clear();

                int thirdBatch = store.deleteBatchByRealm(realmId, 2);
                Assertions.assertEquals(1, thirdBatch);
                Assertions.assertEquals(0, store.deleteBatchByRealm(realmId, 2));

                Assertions.assertNotNull(findById(session, keeper.getId()),
                        "rows from other realms must not be touched");
            });
        } finally {
            runOnServer.run(session -> new SsfEventStore(session).deleteByRealm(otherRealmId));
        }
    }

    @Test
    public void deleteBatchByClient_rejectsNonPositiveBatchSize() {

        runOnServer.run(session -> {
            SsfEventStore store = new SsfEventStore(session);
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> store.deleteBatchByClient("any", 0),
                    "batch size 0 must be rejected so caller bugs don't silently loop");
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> store.deleteBatchByClient("any", -1));
        });
    }

    @Test
    public void cleanupTask_drainsAllRowsAcrossMultipleBatches() {
        final String realmId = testRealmId;

        // Persist in one tx so the rows are visible to the fresh
        // sessions the cleanup task opens per batch.
        runOnServer.run(session -> {
            Instant now = Instant.now();
            for (int i = 0; i < 12; i++) {
                persistRaw(session, realmId, "client-task", "stream-t", "jti-task-" + i,
                        SsfEventStatus.PENDING, 0, now, now);
            }
            persistRaw(session, realmId, "client-keeper", "stream-k", "jti-keeper",
                    SsfEventStatus.PENDING, 0, now, now);
        });

        // Run the task from a fresh tx; the task itself opens its own
        // nested sessions via runJobInTransaction for each batch. Uses
        // a small batchSize so we actually iterate rather than drain
        // in one shot.
        runOnServer.run(session -> new SsfOutboxCleanupTask(session.getKeycloakSessionFactory(),
                SsfEventStore::new,
                SsfOutboxCleanupTask.Scope.CLIENT,
                "client-task",
                5,
                SsfOutboxCleanupTask.DEFAULT_MAX_BATCHES).run());

        runOnServer.run(session -> {
            long remaining = em(session)
                    .createNamedQuery("SsfEventEntity.countByClientAndStatus", Long.class)
                    .setParameter("clientId", "client-task")
                    .setParameter("status", SsfEventStatus.PENDING)
                    .getSingleResult();
            Assertions.assertEquals(0, remaining,
                    "cleanup task must drain all rows for the target client");

            long keeperRows = em(session)
                    .createNamedQuery("SsfEventEntity.countByClientAndStatus", Long.class)
                    .setParameter("clientId", "client-keeper")
                    .setParameter("status", SsfEventStatus.PENDING)
                    .getSingleResult();
            Assertions.assertEquals(1, keeperRows,
                    "rows for other clients must not be touched by the task");
        });
    }

    @Test
    public void cleanupTask_stopsAtMaxBatchesAndLeavesRemainderToDrainer() {
        final String realmId = testRealmId;

        runOnServer.run(session -> {
            Instant now = Instant.now();
            for (int i = 0; i < 10; i++) {
                persistRaw(session, realmId, "client-cap", "stream-c", "jti-cap-" + i,
                        SsfEventStatus.PENDING, 0, now, now);
            }
        });

        // maxBatches=2 with batchSize=3 deletes 6 rows, leaves 4 behind.
        // Verifies the safety cap doesn't spin forever on a runaway backlog.
        runOnServer.run(session -> new SsfOutboxCleanupTask(session.getKeycloakSessionFactory(),
                SsfEventStore::new,
                SsfOutboxCleanupTask.Scope.CLIENT,
                "client-cap",
                3,
                2).run());

        runOnServer.run(session -> {
            long remaining = em(session)
                    .createNamedQuery("SsfEventEntity.countByClientAndStatus", Long.class)
                    .setParameter("clientId", "client-cap")
                    .setParameter("status", SsfEventStatus.PENDING)
                    .getSingleResult();
            Assertions.assertEquals(4, remaining,
                    "task must stop after maxBatches × batchSize rows even if more remain");
        });
    }

    @Test
    public void cleanupTask_realmScopeDrainsAcrossAllClients() {
        final String realmId = testRealmId;
        final String otherRealmId = UUID.randomUUID().toString();
        try {
            runOnServer.run(session -> {
                Instant now = Instant.now();
                for (int i = 0; i < 4; i++) {
                    persistRaw(session, realmId, "c" + (i % 2), "s", "jti-realmdrain-" + i,
                            SsfEventStatus.PENDING, 0, now, now);
                }
                persistRaw(session, otherRealmId, "co", "so", "jti-realmdrain-other",
                        SsfEventStatus.PENDING, 0, now, now);
            });

            runOnServer.run(session -> new SsfOutboxCleanupTask(session.getKeycloakSessionFactory(),
                    SsfEventStore::new,
                    SsfOutboxCleanupTask.Scope.REALM,
                    realmId,
                    10,
                    SsfOutboxCleanupTask.DEFAULT_MAX_BATCHES).run());

            runOnServer.run(session -> {
                int remaining = new SsfEventStore(session).deleteBatchByRealm(realmId, 100);
                Assertions.assertEquals(0, remaining,
                        "realm-scope task must drain every client's rows in the realm");

                long surviving = em(session)
                        .createNamedQuery("SsfEventEntity.countByClientAndStatus", Long.class)
                        .setParameter("clientId", "co")
                        .setParameter("status", SsfEventStatus.PENDING)
                        .getSingleResult();
                Assertions.assertEquals(1, surviving,
                        "rows in other realms must survive");
            });
        } finally {
            runOnServer.run(session -> new SsfEventStore(session).deleteByRealm(otherRealmId));
        }
    }

    // -- helpers (static so the serialized lambdas don't capture the
    //    enclosing test instance on the remote side) --------------------

    private static EntityManager em(KeycloakSession session) {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    private static SsfEventEntity findById(KeycloakSession session, String id) {
        return em(session).find(SsfEventEntity.class, id);
    }

    private static SsfEventEntity persistRaw(KeycloakSession session,
                                                    String realmId,
                                                    String clientId,
                                                    String streamId,
                                                    String jti,
                                                    SsfEventStatus status,
                                                    int attempts,
                                                    Instant nextAttemptAt,
                                                    Instant createdAt) {
        SsfEventEntity e = new SsfEventEntity();
        e.setId(UUID.randomUUID().toString());
        e.setRealmId(realmId);
        e.setClientId(clientId);
        e.setStreamId(streamId);
        e.setJti(jti);
        e.setEventType("test.event");
        e.setEncodedSet("encoded-" + jti);
        e.setDeliveryMethod(SsfEventEntity.DELIVERY_METHOD_PUSH);
        e.setStatus(status);
        e.setAttempts(attempts);
        e.setNextAttemptAt(nextAttemptAt);
        e.setCreatedAt(createdAt);
        em(session).persist(e);
        return e;
    }

    public static class OutboxStoreServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            return configured;
        }
    }
}
