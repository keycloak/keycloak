package org.keycloak.ssf.transmitter.outbox;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.keycloak.events.outbox.OutboxDrainerListener;
import org.keycloak.events.outbox.OutboxDrainerTickSummary;
import org.keycloak.events.outbox.OutboxEntry;
import org.keycloak.events.outbox.OutboxEntryStatus;
import org.keycloak.events.outbox.OutboxMetricsListener;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.OutboxEntryEntity;
import org.keycloak.ssf.transmitter.metrics.SsfMetricsBinder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SsfOutboxMetricsListenerTest {

    private static SsfOutboxMetricsListener listener(SimpleMeterRegistry registry, SsfMetricsBinder binder,
                                                     Map<String, Map<OutboxEntryStatus, Long>> push,
                                                     Map<String, Map<OutboxEntryStatus, Long>> poll) {
        return new SsfOutboxMetricsListener(binder, s -> {
            throw new AssertionError("store must not be touched");
        }, registry) {
            @Override
            protected Map<String, Map<OutboxEntryStatus, Long>> countDepth(KeycloakSession session, String kind) {
                return SsfOutboxKinds.PUSH.equals(kind) ? push : poll;
            }
        };
    }

    private static OutboxEntry row() {
        OutboxEntryEntity row = new OutboxEntryEntity();
        row.setEntryKind(SsfOutboxKinds.PUSH);
        row.setRealmId("realm-id");
        row.setOwnerId("client-uuid");
        return row;
    }

    @Test
    public void depthKinds_coverPushAndPoll() {
        Assertions.assertEquals(List.of(SsfOutboxKinds.PUSH, SsfOutboxKinds.POLL),
                listener(new SimpleMeterRegistry(), SsfMetricsBinder.NOOP, Map.of(), Map.of()).depthKinds());
    }

    @Test
    public void tickEnd_snapshotsPushAndPollDepthUnderTheirOwnKind() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SsfOutboxMetricsListener listener = listener(registry, SsfMetricsBinder.NOOP,
                Map.of("realm-a", Map.of(OutboxEntryStatus.PENDING, 3L)),
                Map.of("realm-a", Map.of(OutboxEntryStatus.PENDING, 2L), "realm-b", Map.of(OutboxEntryStatus.HELD, 5L)));

        listener.onTickEnd(null, new OutboxDrainerTickSummary(SsfOutboxKinds.PUSH, 3, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0,
                Duration.ofMillis(120)));

        Assertions.assertEquals(1.0, registry.get(OutboxMetricsListener.METER_TICK).tag("kind", SsfOutboxKinds.PUSH).counter().count());
        Assertions.assertEquals(3.0, registry.get(OutboxMetricsListener.METER_DEPTH)
                .tags("kind", SsfOutboxKinds.PUSH, "realm", "realm-a", "status", "PENDING").gauge().value());
        Assertions.assertEquals(2.0, registry.get(OutboxMetricsListener.METER_DEPTH)
                .tags("kind", SsfOutboxKinds.POLL, "realm", "realm-a", "status", "PENDING").gauge().value());
        Assertions.assertEquals(5.0, registry.get(OutboxMetricsListener.METER_DEPTH)
                .tags("kind", SsfOutboxKinds.POLL, "realm", "realm-b", "status", "HELD").gauge().value());
    }

    @Test
    public void deadLetter_countsGenericAndPerReceiverMeters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SsfMetricsBinder binder = new SsfMetricsBinder(registry);
        SsfOutboxMetricsListener listener = listener(registry, binder, Map.of(), Map.of());

        listener.onDeadLetter(null, row(), OutboxDrainerListener.DeadLetterCause.ATTEMPTS_EXHAUSTED, "exhausted");

        Assertions.assertEquals(1.0, registry.get(OutboxMetricsListener.METER_DEAD_LETTER)
                .tags("kind", SsfOutboxKinds.PUSH, "realm", "realm-id", "cause", "attempts_exhausted").counter().count());
        Assertions.assertEquals(1.0, registry.get(SsfMetricsBinder.METER_PUSH_DELIVERY)
                .tags("realm", "realm-id", "client_id", "client-uuid", "outcome", "dead_letter").counter().count(),
                "per-receiver counter with raw ids when nothing resolves");
        Assertions.assertTrue(registry.find(SsfMetricsBinder.METER_PUSH_DELIVERY_DURATION).timers().isEmpty(),
                "dead-letter is a counter-only outcome");
    }

    @Test
    public void nullBinder_isSafe() {
        SsfOutboxMetricsListener listener = listener(new SimpleMeterRegistry(), null, Map.of(), Map.of());
        listener.onDeadLetter(null, row(), OutboxDrainerListener.DeadLetterCause.HANDLER, "x");
    }
}
