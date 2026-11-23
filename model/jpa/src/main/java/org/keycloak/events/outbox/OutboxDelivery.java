package org.keycloak.events.outbox;

import java.util.Objects;

/**
 * Phase two of a delivery, produced by
 * {@link OutboxDeliveryHandler#prepare(org.keycloak.models.KeycloakSession, OutboxEntry)}
 * and executed by the drainer <em>outside any transaction</em>, on a
 * thread with no bound {@code KeycloakSession}. Everything the
 * delivery needs (destination URL, credentials, HTTP client, payload)
 * must have been captured during {@code prepare}.
 *
 * <p>Return {@link #settled(OutboxDeliveryResult)} from {@code prepare}
 * when the outcome is already known there (destination gone, paused,
 * backed off) and no call has to be made.
 */
@FunctionalInterface
public interface OutboxDelivery {

    /**
     * Performs the delivery. A thrown {@link RuntimeException} is
     * treated as {@link OutboxDeliveryResult#retry(String)}.
     */
    OutboxDeliveryResult execute();

    static OutboxDelivery settled(OutboxDeliveryResult result) {
        Objects.requireNonNull(result, "result");
        return () -> result;
    }
}
