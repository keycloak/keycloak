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
 *
 * <p>{@code transmitterDisabledBackoff} controls how far the drainer
 * pushes {@code next_attempt_at} when it skips a row whose realm has
 * the SSF transmitter feature switched off. {@code null} or a
 * non-positive value falls back to
 * {@link SsfPushOutboxDrainerTask#DEFAULT_TRANSMITTER_DISABLED_BACKOFF}.
 *
 * <p>{@code pendingMaxAge} is a backstop that promotes any
 * {@code PENDING} row older than this duration to {@code DEAD_LETTER},
 * so rows that would otherwise get stuck (e.g. realm with transmitter
 * disabled, no per-receiver {@code ssf.maxEventAgeSeconds}, no realm
 * removal) eventually graduate to a terminal state and are caught by
 * the dead-letter retention purge. {@code null} or a non-positive
 * value disables the backstop.
 */
public record SsfPushOutboxDrainerTaskConfig(
        int batchSize,
        SsfPushOutboxBackoff backoff,
        Duration deadLetterRetention,
        Duration deliveredRetention,
        Duration transmitterDisabledBackoff,
        Duration pendingMaxAge) {
}
