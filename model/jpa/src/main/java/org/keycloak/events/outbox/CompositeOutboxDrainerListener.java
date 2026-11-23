package org.keycloak.events.outbox;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

/**
 * Fan-out {@link OutboxDrainerListener}; see
 * {@link OutboxDrainerListener#composite}.
 */
final class CompositeOutboxDrainerListener implements OutboxDrainerListener {

    private static final Logger log = Logger.getLogger(CompositeOutboxDrainerListener.class);

    private final List<OutboxDrainerListener> delegates;

    private CompositeOutboxDrainerListener(List<OutboxDrainerListener> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    static OutboxDrainerListener of(OutboxDrainerListener... listeners) {
        List<OutboxDrainerListener> kept = new ArrayList<>();
        if (listeners != null) {
            for (OutboxDrainerListener l : listeners) {
                if (l == null || l == NOOP) {
                    continue;
                }
                if (l instanceof CompositeOutboxDrainerListener c) {
                    kept.addAll(c.delegates);
                } else {
                    kept.add(l);
                }
            }
        }
        return switch (kept.size()) {
            case 0 -> NOOP;
            case 1 -> kept.get(0);
            default -> new CompositeOutboxDrainerListener(kept);
        };
    }

    private void each(String callback, Consumer<OutboxDrainerListener> call) {
        for (OutboxDrainerListener delegate : delegates) {
            try {
                call.accept(delegate);
            } catch (RuntimeException e) {
                log.warnf(e, "Outbox drainer listener %s threw in %s — skipping it", delegate.getClass().getName(), callback);
            }
        }
    }

    @Override
    public void onTickStart(KeycloakSession session, String entryKind) {
        each("onTickStart", l -> l.onTickStart(session, entryKind));
    }

    @Override
    public void onTickEnd(KeycloakSession session, OutboxDrainerTickSummary summary) {
        each("onTickEnd", l -> l.onTickEnd(session, summary));
    }

    @Override
    public void onDelivered(KeycloakSession session, OutboxEntry row) {
        each("onDelivered", l -> l.onDelivered(session, row));
    }

    @Override
    public void onRetryScheduled(KeycloakSession session, OutboxEntry row, Instant nextAttemptAt, String reason) {
        each("onRetryScheduled", l -> l.onRetryScheduled(session, row, nextAttemptAt, reason));
    }

    @Override
    public void onDeferred(KeycloakSession session, OutboxEntry row, Instant notBefore, String reason) {
        each("onDeferred", l -> l.onDeferred(session, row, notBefore, reason));
    }

    @Override
    public void onDeadLetter(KeycloakSession session, OutboxEntry row, DeadLetterCause cause, String reason) {
        each("onDeadLetter", l -> l.onDeadLetter(session, row, cause, reason));
    }
}
