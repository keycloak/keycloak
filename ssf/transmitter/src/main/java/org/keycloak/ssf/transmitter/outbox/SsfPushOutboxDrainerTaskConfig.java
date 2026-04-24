package org.keycloak.ssf.transmitter.outbox;

import java.time.Duration;

/**
 * Tuning parameters for {@link SsfPushOutboxDrainerTask}. Kept separate
 * from the task's collaborators (context, factories, metrics binder) so
 * the constructor stays readable and new knobs (additional retention
 * windows, thresholds, etc.) have an obvious home.
 *
 * <p>Retention fields accept {@code null} or a non-positive {@link Duration}
 * to disable the corresponding purge — see {@link SsfPushOutboxDrainerTask}
 * for the per-field semantics.
 */
public record SsfPushOutboxDrainerTaskConfig(
        int batchSize,
        SsfPushOutboxBackoff backoff,
        Duration deadLetterRetention,
        Duration deliveredRetention) {
}
