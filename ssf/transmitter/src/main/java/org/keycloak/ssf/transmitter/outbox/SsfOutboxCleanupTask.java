package org.keycloak.ssf.transmitter.outbox;


import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;

import org.jboss.logging.Logger;

/**
 * Background cleanup task that drains SSF outbox rows owned by a removed
 * client or realm. Runs in short, bounded transactions so the admin's
 * original client-/realm-removal transaction can commit immediately
 * instead of carrying a six-digit {@code DELETE} on its back.
 *
 * <p>The listener in
 * {@link org.keycloak.ssf.transmitter.DefaultSsfTransmitterProviderFactory}
 * submits one instance of this task to a Keycloak-managed executor
 * when it observes {@code ClientRemovedEvent} / {@code RealmRemovedEvent}.
 * The task then loops: open a fresh session → delete up to
 * {@link #batchSize} rows → commit → repeat, until either the batch
 * count reaches {@link #maxBatches} or the store returns zero rows.
 *
 * <p>Crash safety: if the node running this task dies mid-flight, the
 * remaining rows are orphaned. That's fine — the push drainer already
 * dead-letters rows whose realm/client/stream lookup returns null on
 * its next tick (see
 * {@code SsfPushOutboxDrainerTask#processPendingEvent}), and the
 * existing dead-letter retention purge deletes them in the background.
 * POLL rows have no such drainer pass; they'd sit until a later
 * client-/realm-removed event for a different target sweeps them, or
 * until a manual purge. Acceptable given the rarity of the crash
 * scenario and the fact that orphan POLL rows are inert (no consumer).
 */
public class SsfOutboxCleanupTask implements Runnable {

    private static final Logger log = Logger.getLogger(SsfOutboxCleanupTask.class);

    /** Default per-transaction batch size. Kept in the 500–5000 band
     *  recommended for short-lived bulk DML against Postgres. */
    public static final int DEFAULT_BATCH_SIZE = 1000;

    /** Safety cap on the number of batches a single task will run.
     *  At the default batch size this handles 10M rows before bailing;
     *  anything above that is almost certainly a runaway and should
     *  surface as a warning rather than spin indefinitely. */
    public static final int DEFAULT_MAX_BATCHES = 10_000;

    public enum Scope {
        CLIENT, REALM
    }

    protected final KeycloakSessionFactory factory;
    protected final Function<KeycloakSession, SsfPendingEventStore> storeFactory;
    protected final Scope scope;
    protected final String key;
    protected final int batchSize;
    protected final int maxBatches;

    public SsfOutboxCleanupTask(KeycloakSessionFactory factory,
                                Function<KeycloakSession, SsfPendingEventStore> storeFactory,
                                Scope scope,
                                String key) {
        this(factory, storeFactory, scope, key, DEFAULT_BATCH_SIZE, DEFAULT_MAX_BATCHES);
    }

    public SsfOutboxCleanupTask(KeycloakSessionFactory factory,
                                Function<KeycloakSession, SsfPendingEventStore> storeFactory,
                                Scope scope,
                                String key,
                                int batchSize,
                                int maxBatches) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.storeFactory = Objects.requireNonNull(storeFactory, "storeFactory");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.key = Objects.requireNonNull(key, "key");
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        if (maxBatches <= 0) {
            throw new IllegalArgumentException("maxBatches must be positive");
        }
        this.batchSize = batchSize;
        this.maxBatches = maxBatches;
    }

    @Override
    public void run() {
        int totalDeleted = 0;
        int iterations = 0;
        try {
            while (iterations < maxBatches) {
                AtomicInteger deletedInBatch = new AtomicInteger(0);
                KeycloakModelUtils.runJobInTransaction(factory, session -> {
                    SsfPendingEventStore store = storeFactory.apply(session);
                    int deleted = scope == Scope.CLIENT
                            ? store.deleteBatchByClient(key, batchSize)
                            : store.deleteBatchByRealm(key, batchSize);
                    deletedInBatch.set(deleted);
                });
                int batchCount = deletedInBatch.get();
                if (batchCount == 0) {
                    break;
                }
                totalDeleted += batchCount;
                iterations++;
            }
        } catch (RuntimeException e) {
            log.warnf(e, "SSF outbox cleanup task failed mid-flight. scope=%s key=%s deletedBefore=%d",
                    scope, key, totalDeleted);
            // Orphan remainder — the push drainer will dead-letter them
            // on its next tick, retention handles deletion. Don't
            // re-throw: the executor would log it as an uncaught
            // exception and we've already accounted for partial progress.
            return;
        }
        if (iterations >= maxBatches) {
            log.warnf("SSF outbox cleanup hit the maxBatches cap — remaining rows left to the drainer. "
                            + "scope=%s key=%s deletedSoFar=%d batchSize=%d maxBatches=%d",
                    scope, key, totalDeleted, batchSize, maxBatches);
            return;
        }
        if (totalDeleted > 0) {
            log.debugf("SSF outbox cleanup complete. scope=%s key=%s totalDeleted=%d batches=%d",
                    scope, key, totalDeleted, iterations);
        }
    }
}
