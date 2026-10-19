package org.keycloak.tests.events.outbox;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.outbox.OutboxBackoff;
import org.keycloak.events.outbox.OutboxConfig;
import org.keycloak.events.outbox.OutboxDelivery;
import org.keycloak.events.outbox.OutboxDeliveryHandler;
import org.keycloak.events.outbox.OutboxDeliveryResult;
import org.keycloak.events.outbox.OutboxDrainerListener;
import org.keycloak.events.outbox.OutboxDrainerTask;
import org.keycloak.events.outbox.OutboxDrainerTickSummary;
import org.keycloak.events.outbox.OutboxEntry;
import org.keycloak.events.outbox.OutboxEntryStatus;
import org.keycloak.events.outbox.OutboxStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.OutboxEntryEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link OutboxDrainerTask}: one tick is run
 * directly against committed rows with a scripted
 * {@link OutboxDeliveryHandler} that doubles as
 * {@link OutboxDrainerListener}, and the resulting row states and
 * listener callbacks are checked. Rows are persisted in a separate
 * server call because the drainer opens its own transactions, which
 * cannot see uncommitted rows of the calling one. Uses the synthetic
 * {@code "test-kind"} like {@link OutboxStoreTests}.
 */
@KeycloakIntegrationTest(config = OutboxDrainerTaskTests.OutboxDrainerServerConfig.class)
public class OutboxDrainerTaskTests {

    private static final String TEST_KIND = "test-kind";

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private final String testRealmId = UUID.randomUUID().toString();

    @AfterEach
    public void cleanupRealm() {
        final String realmId = testRealmId;
        runOnServer.run(session -> new OutboxStore(session).deleteByRealm(TEST_KIND, realmId));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void tick_transitionsRowsPerOutcomeAndNotifiesListener() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant due = Instant.now().minusSeconds(1);
            // correlationId doubles as the scripted outcome, see ScriptedHandler.
            persistRow(session, realmId, "deliver", 0, due);
            persistRow(session, realmId, "retry", 0, due);
            persistRow(session, realmId, "retry-last", 1, due);   // maxAttempts=2 → exhausted
            persistRow(session, realmId, "defer", 1, due);
            persistRow(session, realmId, "dead", 0, due);
            persistRow(session, realmId, "orphan", 0, due);
            persistRow(session, realmId, "throw", 0, due);
            persistRow(session, realmId, "throw-prepare", 0, due);
            persistRow(session, realmId, "not-due", 0, Instant.now().plus(Duration.ofHours(1)));
        });

        Map<String, Object> result = runOnServer.fetch(session -> {
            ScriptedHandler handler = new ScriptedHandler();
            OutboxConfig config = new OutboxConfig(TEST_KIND, 50,
                    new OutboxBackoff(2, List.of(Duration.ofMinutes(5))), null, null, null);
            OutboxDrainerTask task = new OutboxDrainerTask(config, handler, OutboxStore::new);
            task.run(session);

            Map<String, Object> out = snapshot(session);
            out.put("taskName", task.getTaskName());
            out.put("events", handler.events);
            out.put("summary", handler.summary);
            return out;
        }, Map.class);

        Assertions.assertEquals("outbox-drainer:test-kind", result.get("taskName"));

        Assertions.assertEquals("DELIVERED", result.get("status.deliver"));
        Assertions.assertEquals(1, result.get("attempts.deliver"));

        Assertions.assertEquals("PENDING", result.get("status.retry"));
        Assertions.assertEquals(1, result.get("attempts.retry"));
        Assertions.assertEquals(true, result.get("dueLater.retry"), "retry is pushed out by the backoff curve");
        Assertions.assertEquals("scripted retry", result.get("lastError.retry"));

        Assertions.assertEquals("DEAD_LETTER", result.get("status.retry-last"), "second failure exhausts maxAttempts=2");
        Assertions.assertEquals(2, result.get("attempts.retry-last"));

        Assertions.assertEquals("PENDING", result.get("status.defer"));
        Assertions.assertEquals(1, result.get("attempts.defer"), "DEFER must not count an attempt");
        Assertions.assertEquals(true, result.get("dueLater.defer"));
        Assertions.assertEquals("scripted defer", result.get("lastError.defer"));

        Assertions.assertEquals("DEAD_LETTER", result.get("status.dead"));
        Assertions.assertEquals("DEAD_LETTER", result.get("status.orphan"));

        Assertions.assertEquals("PENDING", result.get("status.throw"), "a throwing delivery is treated as RETRY");
        Assertions.assertEquals(1, result.get("attempts.throw"));
        Assertions.assertTrue(((String) result.get("lastError.throw")).startsWith("IllegalStateException"));

        Assertions.assertEquals("PENDING", result.get("status.throw-prepare"), "a throwing prepare is treated as RETRY");
        Assertions.assertEquals(1, result.get("attempts.throw-prepare"));

        Assertions.assertEquals("PENDING", result.get("status.not-due"));
        Assertions.assertEquals(0, result.get("attempts.not-due"), "rows that are not due are not touched");

        for (String corr : List.of("deliver", "retry", "retry-last", "defer", "dead", "orphan", "throw", "throw-prepare", "not-due")) {
            Assertions.assertNull(result.get("claimToken." + corr), "no row stays claimed after the tick: " + corr);
        }

        List<String> events = (List<String>) result.get("events");
        Assertions.assertEquals("tickStart:test-kind", events.get(0));
        Assertions.assertEquals("tickEnd", events.get(events.size() - 1));
        Assertions.assertTrue(events.contains("delivered:deliver"), events.toString());
        Assertions.assertTrue(events.contains("retry:retry"), events.toString());
        Assertions.assertTrue(events.contains("retry:throw"), events.toString());
        Assertions.assertTrue(events.contains("retry:throw-prepare"), events.toString());
        Assertions.assertTrue(events.contains("deferred:defer"), events.toString());
        Assertions.assertTrue(events.contains("deadLetter:retry-last:ATTEMPTS_EXHAUSTED"), events.toString());
        Assertions.assertTrue(events.contains("deadLetter:dead:HANDLER"), events.toString());
        Assertions.assertTrue(events.contains("deadLetter:orphan:ORPHANED"), events.toString());
        Assertions.assertFalse(events.stream().anyMatch(e -> e.endsWith(":not-due")), events.toString());

        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        Assertions.assertEquals(TEST_KIND, summary.get("entryKind"));
        Assertions.assertEquals(8, summary.get("claimed"));
        Assertions.assertEquals(8, summary.get("processed"));
        Assertions.assertEquals(1, summary.get("delivered"));
        Assertions.assertEquals(3, summary.get("retried"));
        Assertions.assertEquals(1, summary.get("deferred"));
        Assertions.assertEquals(3, summary.get("deadLettered"));
        Assertions.assertEquals(0, summary.get("released"));
        Assertions.assertEquals(0, summary.get("claimLost"));
        Assertions.assertEquals(0, summary.get("stalePromoted"));
    }

    @Test
    public void tick_survivesAThrowingListener() {
        final String realmId = testRealmId;
        runOnServer.run(session -> persistRow(session, realmId, "deliver", 0, Instant.now().minusSeconds(1)));

        String status = runOnServer.fetch(session -> {
            OutboxConfig config = new OutboxConfig(TEST_KIND, 50, new OutboxBackoff(), null, null, null);
            new OutboxDrainerTask(config, new ScriptedHandler(), OutboxStore::new, new BrokenListener()).run(session);
            return (String) snapshot(session).get("status.deliver");
        }, String.class);

        Assertions.assertEquals("DELIVERED", status, "a listener failure must not affect the row transition");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void tick_budgetReleasesUnprocessedClaimedRowsForTheNextTick() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant base = Instant.now().minusSeconds(10);
            // Arrival order = next_attempt_at order: slow-1 is delivered
            // first, then the budget is gone.
            persistRow(session, realmId, "slow-1", 0, base);
            persistRow(session, realmId, "slow-2", 0, base.plusSeconds(1));
            persistRow(session, realmId, "slow-3", 0, base.plusSeconds(2));
        });

        Map<String, Object> result = runOnServer.fetch(session -> {
            ScriptedHandler handler = new ScriptedHandler();
            OutboxConfig config = new OutboxConfig(TEST_KIND, 50, new OutboxBackoff(), null, null, null,
                    Duration.ofMinutes(5), Duration.ofMillis(100));
            new OutboxDrainerTask(config, handler, OutboxStore::new).run(session);

            Map<String, Object> out = snapshot(session);
            out.put("summary", handler.summary);
            return out;
        }, Map.class);

        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        Assertions.assertEquals(3, summary.get("claimed"));
        Assertions.assertEquals(1, summary.get("processed"), "the budget is checked before each row, so one row is always processed");
        Assertions.assertEquals(1, summary.get("delivered"));
        Assertions.assertEquals(2, summary.get("released"));

        Assertions.assertEquals("DELIVERED", result.get("status.slow-1"));
        for (String corr : List.of("slow-2", "slow-3")) {
            Assertions.assertEquals("PENDING", result.get("status." + corr));
            Assertions.assertEquals(0, result.get("attempts." + corr), "released rows did not spend an attempt");
            Assertions.assertNull(result.get("claimToken." + corr), "released rows carry no token");
            Assertions.assertEquals(false, result.get("dueLater." + corr), "released rows are due now");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void tick_leavesRowsLeasedByAnotherTickAlone() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            persistRow(session, realmId, "deliver", 0, Instant.now().minusSeconds(1));
            em(session).flush();
            // Simulate a sibling tick holding the row under a live lease.
            List<OutboxEntryEntity> claimed = new OutboxStore(session)
                    .claimDueForDrain(TEST_KIND, 10, "sibling-token", Duration.ofMinutes(5));
            if (claimed.size() != 1) {
                throw new AssertionError("expected to claim exactly one row, got " + claimed.size());
            }
        });

        Map<String, Object> result = runOnServer.fetch(session -> {
            ScriptedHandler handler = new ScriptedHandler();
            OutboxConfig config = new OutboxConfig(TEST_KIND, 50, new OutboxBackoff(), null, null, null);
            new OutboxDrainerTask(config, handler, OutboxStore::new).run(session);

            Map<String, Object> out = snapshot(session);
            out.put("summary", handler.summary);
            return out;
        }, Map.class);

        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        Assertions.assertEquals(0, summary.get("claimed"), "a row under a live lease is not due");
        Assertions.assertEquals("PENDING", result.get("status.deliver"));
        Assertions.assertEquals("sibling-token", result.get("claimToken.deliver"));
        Assertions.assertEquals(0, result.get("attempts.deliver"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void tick_doesNotRecordAnOutcomeWhenTheClaimWasTakenOverDuringDelivery() {
        final String realmId = testRealmId;
        runOnServer.run(session -> persistRow(session, realmId, "takeover", 0, Instant.now().minusSeconds(1)));

        Map<String, Object> result = runOnServer.fetch(session -> {
            ScriptedHandler handler = new ScriptedHandler();
            OutboxConfig config = new OutboxConfig(TEST_KIND, 50, new OutboxBackoff(), null, null, null);
            new OutboxDrainerTask(config, handler, OutboxStore::new).run(session);

            Map<String, Object> out = snapshot(session);
            out.put("summary", handler.summary);
            out.put("events", handler.events);
            return out;
        }, Map.class);

        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        Assertions.assertEquals(1, summary.get("processed"));
        Assertions.assertEquals(1, summary.get("claimLost"));
        Assertions.assertEquals(0, summary.get("delivered"), "the handler delivered, but the record was skipped");
        Assertions.assertEquals("PENDING", result.get("status.takeover"));
        Assertions.assertEquals(0, result.get("attempts.takeover"));
        Assertions.assertEquals("other-tick", result.get("claimToken.takeover"), "the new holder's token survives");
        Assertions.assertFalse(((List<String>) result.get("events")).contains("delivered:takeover"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void tick_ownerFairClaimCapsEachOwnerPerRoundAndStillFillsTheBatch() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant base = Instant.now().minusSeconds(100);
            // owner-a flooded the queue first; owner-b has two newer rows.
            for (int i = 0; i < 6; i++) {
                persistRow(session, realmId, "owner-a", "deliver-" + i, 0, base.plusSeconds(i));
            }
            persistRow(session, realmId, "owner-b", "deliver-0", 0, base.plusSeconds(50));
            persistRow(session, realmId, "owner-b", "deliver-1", 0, base.plusSeconds(51));
        });

        Map<String, Object> result = runOnServer.fetch(session -> {
            ScriptedHandler handler = new ScriptedHandler();
            // batch 5, at most 2 per owner per round:
            //   round 1: a=2, b=2  round 2: a=1 (batch full)
            OutboxConfig config = new OutboxConfig(TEST_KIND, 5, new OutboxBackoff(), null, null, null,
                    Duration.ofMinutes(5), null, 2);
            new OutboxDrainerTask(config, handler, OutboxStore::new).run(session);

            em(session).clear();
            OutboxStore store = new OutboxStore(session);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("a.delivered", store.countForOwnerByStatus(TEST_KIND, "owner-a", OutboxEntryStatus.DELIVERED));
            out.put("a.pending", store.countForOwnerByStatus(TEST_KIND, "owner-a", OutboxEntryStatus.PENDING));
            out.put("b.delivered", store.countForOwnerByStatus(TEST_KIND, "owner-b", OutboxEntryStatus.DELIVERED));
            out.put("b.pending", store.countForOwnerByStatus(TEST_KIND, "owner-b", OutboxEntryStatus.PENDING));
            out.put("order", handler.events.stream().filter(e -> e.startsWith("delivered:")).toList());
            out.put("summary", handler.summary);
            return out;
        }, Map.class);

        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        Assertions.assertEquals(5, summary.get("claimed"), "the batch is filled despite the per-owner cap");
        Assertions.assertEquals(5, summary.get("delivered"));
        Assertions.assertEquals(3, result.get("a.delivered"));
        Assertions.assertEquals(3, result.get("a.pending"));
        Assertions.assertEquals(2, result.get("b.delivered"), "owner-b gets its share although owner-a's rows are older");
        Assertions.assertEquals(0, result.get("b.pending"));
        Assertions.assertEquals(
                List.of("delivered:deliver-0", "delivered:deliver-1", "delivered:deliver-0", "delivered:deliver-1", "delivered:deliver-2"),
                result.get("order"),
                "deliveries interleave owners round by round (a0 a1 b0 b1 a2)");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void tick_ownerFairClaimWithoutDueRowsClaimsNothing() {
        final String realmId = testRealmId;
        runOnServer.run(session -> persistRow(session, realmId, "deliver", 0, Instant.now().plus(Duration.ofHours(1))));

        Map<String, Object> summary = runOnServer.fetch(session -> {
            ScriptedHandler handler = new ScriptedHandler();
            OutboxConfig config = new OutboxConfig(TEST_KIND, 5, new OutboxBackoff(), null, null, null,
                    Duration.ofMinutes(5), null, 2);
            new OutboxDrainerTask(config, handler, OutboxStore::new).run(session);
            return handler.summary;
        }, Map.class);

        Assertions.assertEquals(0, summary.get("claimed"));
        Assertions.assertEquals(0, summary.get("processed"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void tick_deliversOwnersInParallelAndKeepsEachOwnersOrder() {
        final String realmId = testRealmId;
        runOnServer.run(session -> {
            Instant base = Instant.now().minusSeconds(100);
            for (String owner : List.of("owner-p1", "owner-p2", "owner-p3")) {
                persistRow(session, realmId, owner, "deliver-slow-0", 0, base);
                persistRow(session, realmId, owner, "deliver-slow-1", 0, base.plusSeconds(1));
            }
        });

        Map<String, Object> result = runOnServer.fetch(session -> {
            ScriptedHandler handler = new ScriptedHandler();
            OutboxConfig config = new OutboxConfig(TEST_KIND, 50, new OutboxBackoff(), null, null, null,
                    Duration.ofMinutes(5), null, null, 3);
            new OutboxDrainerTask(config, handler, OutboxStore::new).run(session);

            em(session).clear();
            OutboxStore store = new OutboxStore(session);
            Map<String, Object> out = new LinkedHashMap<>();
            for (String owner : List.of("owner-p1", "owner-p2", "owner-p3")) {
                out.put(owner + ".delivered", store.countForOwnerByStatus(TEST_KIND, owner, OutboxEntryStatus.DELIVERED));
                for (OutboxEntryEntity r : store.listByOwner(TEST_KIND, owner, OutboxEntryStatus.DELIVERED, 0, 10)) {
                    out.put(owner + "." + r.getCorrelationId(), r.getDeliveredAt().toEpochMilli());
                }
            }
            out.put("threads", new ArrayList<>(handler.threads));
            out.put("summary", handler.summary);
            return out;
        }, Map.class);

        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        Assertions.assertEquals(6, summary.get("delivered"));
        for (String owner : List.of("owner-p1", "owner-p2", "owner-p3")) {
            Assertions.assertEquals(2, result.get(owner + ".delivered"));
            long first = ((Number) result.get(owner + ".deliver-slow-0")).longValue();
            long second = ((Number) result.get(owner + ".deliver-slow-1")).longValue();
            Assertions.assertTrue(first <= second, "an owner's rows are delivered in order: " + owner + " " + first + " " + second);
        }
        List<String> threads = (List<String>) result.get("threads");
        Assertions.assertTrue(threads.size() >= 2, "deliveries ran on more than one worker thread: " + threads);
    }

    /**
     * Reads every row of the test owner back through a fresh query
     * (the drainer committed in its own transactions) into a flat,
     * JSON-friendly map keyed by {@code <field>.<correlationId>}.
     */
    private static Map<String, Object> snapshot(KeycloakSession session) {
        em(session).clear();
        Map<String, Object> out = new LinkedHashMap<>();
        for (OutboxEntryEntity row : new OutboxStore(session).listByOwner(TEST_KIND, "owner-drain", null, 0, 50)) {
            out.put("status." + row.getCorrelationId(), row.getStatus().name());
            out.put("attempts." + row.getCorrelationId(), row.getAttempts());
            out.put("lastError." + row.getCorrelationId(), row.getLastError());
            out.put("claimToken." + row.getCorrelationId(), row.getClaimToken());
            out.put("dueLater." + row.getCorrelationId(), row.getNextAttemptAt().isAfter(Instant.now()));
        }
        return out;
    }

    // -- scripted collaborators (Serializable: they travel inside the lambda) --

    /**
     * Maps the row's correlationId to an outcome and records every
     * listener callback into {@link #events}.
     */
    public static class ScriptedHandler implements OutboxDeliveryHandler, OutboxDrainerListener, Serializable {

        final List<String> events = Collections.synchronizedList(new ArrayList<>());
        final Set<String> threads = Collections.synchronizedSet(new TreeSet<>());
        final Map<String, Object> summary = new LinkedHashMap<>();

        @Override
        public String entryKind() {
            return TEST_KIND;
        }

        @Override
        public OutboxDelivery prepare(KeycloakSession session, OutboxEntry row) {
            String corr = row.getCorrelationId();
            if ("throw-prepare".equals(corr)) {
                throw new IllegalStateException("scripted throw in prepare");
            }
            if ("takeover".equals(corr)) {
                // Another tick (or an admin re-arm) takes the row over
                // while we are preparing: committed in a transaction of
                // its own, like a sibling node would.
                String id = row.getId();
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), s -> {
                    OutboxEntryEntity current = s.getProvider(JpaConnectionProvider.class)
                            .getEntityManager().find(OutboxEntryEntity.class, id);
                    current.setClaimToken("other-tick");
                });
                return OutboxDelivery.settled(OutboxDeliveryResult.delivered());
            }
            if (corr.startsWith("deliver-")) {
                return () -> {
                    threads.add(Thread.currentThread().getName());
                    if (corr.startsWith("deliver-slow")) {
                        sleep(150);
                    }
                    return OutboxDeliveryResult.delivered();
                };
            }
            return switch (corr) {
                case "deliver" -> OutboxDelivery.settled(OutboxDeliveryResult.delivered());
                case "retry", "retry-last" -> OutboxDelivery.settled(OutboxDeliveryResult.retry("scripted retry"));
                case "defer" -> OutboxDelivery.settled(OutboxDeliveryResult.defer(Duration.ofMinutes(10), "scripted defer"));
                case "dead" -> OutboxDelivery.settled(OutboxDeliveryResult.deadLetter("scripted dead letter"));
                case "orphan" -> OutboxDelivery.settled(OutboxDeliveryResult.orphaned("scripted orphan"));
                case "throw" -> () -> {
                    throw new IllegalStateException("scripted throw");
                };
                case "slow-1", "slow-2", "slow-3" -> () -> {
                    sleep(150);
                    return OutboxDeliveryResult.delivered();
                };
                default -> throw new AssertionError("unexpected row " + corr);
            };
        }

        private static void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void onTickStart(KeycloakSession session, String entryKind) {
            events.add("tickStart:" + entryKind);
        }

        @Override
        public void onTickEnd(KeycloakSession session, OutboxDrainerTickSummary s) {
            events.add("tickEnd");
            summary.put("entryKind", s.entryKind());
            summary.put("claimed", s.claimed());
            summary.put("processed", s.processed());
            summary.put("delivered", s.delivered());
            summary.put("retried", s.retried());
            summary.put("deferred", s.deferred());
            summary.put("deadLettered", s.deadLettered());
            summary.put("released", s.released());
            summary.put("claimLost", s.claimLost());
            summary.put("stalePromoted", s.stalePromoted());
        }

        @Override
        public void onDelivered(KeycloakSession session, OutboxEntry row) {
            events.add("delivered:" + row.getCorrelationId());
        }

        @Override
        public void onRetryScheduled(KeycloakSession session, OutboxEntry row, Instant nextAttemptAt, String reason) {
            events.add("retry:" + row.getCorrelationId());
        }

        @Override
        public void onDeferred(KeycloakSession session, OutboxEntry row, Instant notBefore, String reason) {
            events.add("deferred:" + row.getCorrelationId());
        }

        @Override
        public void onDeadLetter(KeycloakSession session, OutboxEntry row, DeadLetterCause cause, String reason) {
            events.add("deadLetter:" + row.getCorrelationId() + ":" + cause);
        }
    }

    public static class BrokenListener implements OutboxDrainerListener, Serializable {

        @Override
        public void onTickStart(KeycloakSession session, String entryKind) {
            throw new IllegalStateException("listener failure on tick start");
        }

        @Override
        public void onDelivered(KeycloakSession session, OutboxEntry row) {
            throw new IllegalStateException("listener failure on delivered");
        }
    }

    private static EntityManager em(KeycloakSession session) {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    private static OutboxEntryEntity persistRow(KeycloakSession session, String realmId, String correlationId,
                                                int attempts, Instant nextAttemptAt) {
        return persistRow(session, realmId, "owner-drain", correlationId, attempts, nextAttemptAt);
    }

    private static OutboxEntryEntity persistRow(KeycloakSession session, String realmId, String ownerId,
                                                String correlationId, int attempts, Instant nextAttemptAt) {
        OutboxEntryEntity e = new OutboxEntryEntity();
        e.setId(UUID.randomUUID().toString());
        e.setEntryKind(TEST_KIND);
        e.setRealmId(realmId);
        e.setOwnerId(ownerId);
        e.setCorrelationId(correlationId);
        e.setEntryType("test.event");
        e.setPayload("encoded-" + correlationId);
        e.setStatus(OutboxEntryStatus.PENDING);
        e.setAttempts(attempts);
        e.setNextAttemptAt(nextAttemptAt);
        e.setCreatedAt(Instant.now().minusSeconds(5));
        em(session).persist(e);
        return e;
    }

    public static class OutboxDrainerServerConfig extends DefaultKeycloakServerConfig {
    }
}
