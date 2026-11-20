package org.keycloak.events.outbox;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.OutboxEntryEntity;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Assert;
import org.junit.Test;

public class OutboxMetricsListenerTest {

    private static final String KIND = "test-kind";

    /** Listener with a canned depth source; no session or store needed. */
    private static OutboxMetricsListener listener(SimpleMeterRegistry registry,
                                                  Map<String, Map<String, Map<OutboxEntryStatus, Long>>> depthByKind,
                                                  List<String> depthKinds) {
        return new OutboxMetricsListener(KIND, s -> {
            throw new AssertionError("store must not be touched");
        }, registry) {
            @Override
            protected List<String> depthKinds() {
                return depthKinds;
            }

            @Override
            protected Map<String, Map<OutboxEntryStatus, Long>> countDepth(KeycloakSession session, String kind) {
                return depthByKind.getOrDefault(kind, Map.of());
            }
        };
    }

    private static OutboxDrainerTickSummary summary(Duration took) {
        return new OutboxDrainerTickSummary(KIND, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, took);
    }

    private static OutboxEntry row(String realmId) {
        OutboxEntryEntity row = new OutboxEntryEntity();
        row.setEntryKind(KIND);
        row.setRealmId(realmId);
        row.setOwnerId("owner");
        return row;
    }

    @Test
    public void tickEnd_recordsTickMetersAndDepthPerKindRealmStatus() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OutboxMetricsListener listener = listener(registry, Map.of(
                KIND, Map.of("realm-a", Map.of(OutboxEntryStatus.PENDING, 3L, OutboxEntryStatus.DEAD_LETTER, 1L)),
                "other-kind", Map.of("realm-a", Map.of(OutboxEntryStatus.PENDING, 2L))),
                List.of(KIND, "other-kind"));

        Assert.assertEquals(0.0, registry.get(OutboxMetricsListener.METER_TICK_LAST_AT).tag("kind", KIND).gauge().value(), 0.0);

        listener.onTickEnd(null, summary(Duration.ofMillis(80)));

        Assert.assertEquals(1.0, registry.get(OutboxMetricsListener.METER_TICK).tag("kind", KIND).counter().count(), 0.0);
        Assert.assertEquals(1, registry.get(OutboxMetricsListener.METER_TICK_DURATION).tag("kind", KIND).timer().count());
        Assert.assertTrue(registry.get(OutboxMetricsListener.METER_TICK_LAST_AT).tag("kind", KIND).gauge().value() > 0);
        Assert.assertEquals(3.0, registry.get(OutboxMetricsListener.METER_DEPTH)
                .tags("kind", KIND, "realm", "realm-a", "status", "PENDING").gauge().value(), 0.0);
        Assert.assertEquals(1.0, registry.get(OutboxMetricsListener.METER_DEPTH)
                .tags("kind", KIND, "realm", "realm-a", "status", "DEAD_LETTER").gauge().value(), 0.0);
        Assert.assertEquals(2.0, registry.get(OutboxMetricsListener.METER_DEPTH)
                .tags("kind", "other-kind", "realm", "realm-a", "status", "PENDING").gauge().value(), 0.0);

        listener.onTickEnd(null, summary(Duration.ofMillis(10)));
        Assert.assertEquals(2.0, registry.get(OutboxMetricsListener.METER_TICK).tag("kind", KIND).counter().count(), 0.0);
        Assert.assertEquals(2, registry.get(OutboxMetricsListener.METER_TICK_DURATION).tag("kind", KIND).timer().count());
    }

    @Test
    public void depthKeyMissingFromLaterSnapshot_readsZeroAndKeepsTheGauge() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OutboxMetricsListener listener = listener(registry, Map.of(), List.of(KIND));

        listener.updateDepthSnapshot(Map.of(new OutboxMetricsListener.KindRealmStatus(KIND, "realm-a", OutboxEntryStatus.PENDING), 4L));
        Assert.assertEquals(4.0, registry.get(OutboxMetricsListener.METER_DEPTH)
                .tags("kind", KIND, "realm", "realm-a", "status", "PENDING").gauge().value(), 0.0);

        listener.updateDepthSnapshot(Map.of());
        Assert.assertEquals(0.0, registry.get(OutboxMetricsListener.METER_DEPTH)
                .tags("kind", KIND, "realm", "realm-a", "status", "PENDING").gauge().value(), 0.0);
    }

    @Test
    public void transitions_areCountedByRealmAndOutcomeWithoutOwnerLabel() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OutboxMetricsListener listener = listener(registry, Map.of(), List.of(KIND));

        listener.onDelivered(null, row("realm-a"));
        listener.onDelivered(null, row("realm-a"));
        listener.onRetryScheduled(null, row("realm-a"), null, "boom");
        listener.onDeferred(null, row("realm-b"), null, "later");
        listener.onDeadLetter(null, row("realm-a"), OutboxDrainerListener.DeadLetterCause.ATTEMPTS_EXHAUSTED, "exhausted");
        listener.onDeadLetter(null, row("realm-a"), OutboxDrainerListener.DeadLetterCause.ORPHANED, "gone");

        Assert.assertEquals(2.0, registry.get(OutboxMetricsListener.METER_TRANSITIONS)
                .tags("kind", KIND, "realm", "realm-a", "outcome", "delivered").counter().count(), 0.0);
        Assert.assertEquals(1.0, registry.get(OutboxMetricsListener.METER_TRANSITIONS)
                .tags("kind", KIND, "realm", "realm-a", "outcome", "retry").counter().count(), 0.0);
        Assert.assertEquals(1.0, registry.get(OutboxMetricsListener.METER_TRANSITIONS)
                .tags("kind", KIND, "realm", "realm-b", "outcome", "defer").counter().count(), 0.0);
        Assert.assertEquals(2.0, registry.get(OutboxMetricsListener.METER_TRANSITIONS)
                .tags("kind", KIND, "realm", "realm-a", "outcome", "dead_letter").counter().count(), 0.0);
        Assert.assertEquals(1.0, registry.get(OutboxMetricsListener.METER_DEAD_LETTER)
                .tags("kind", KIND, "realm", "realm-a", "cause", "attempts_exhausted").counter().count(), 0.0);
        Assert.assertEquals(1.0, registry.get(OutboxMetricsListener.METER_DEAD_LETTER)
                .tags("kind", KIND, "realm", "realm-a", "cause", "orphaned").counter().count(), 0.0);

        Assert.assertTrue("no meter carries an owner label",
                registry.getMeters().stream().noneMatch(m -> m.getId().getTag("owner") != null));
    }

    @Test
    public void nullRealmId_becomesUnknownLabel() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OutboxMetricsListener listener = listener(registry, Map.of(), List.of(KIND));

        listener.onDelivered(null, row(null));

        Assert.assertEquals(1.0, registry.get(OutboxMetricsListener.METER_TRANSITIONS)
                .tags("kind", KIND, "realm", "unknown", "outcome", "delivered").counter().count(), 0.0);
    }
}
