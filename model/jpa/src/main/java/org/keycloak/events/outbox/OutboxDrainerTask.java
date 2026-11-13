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
package org.keycloak.events.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.jpa.entities.OutboxEntryEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.utils.KeycloakSessionUtil;

import org.jboss.logging.Logger;

/**
 * Drains the generic outbox for one registered {@code entryKind}: claims
 * due PENDING rows, hands each off to the kind's
 * {@link OutboxDeliveryHandler}, and transitions the row based on the
 * returned {@link OutboxDeliveryOutcome}.
 *
 * <p>One drainer instance per registered kind. Schedule it through
 * {@link OutboxDrainerScheduler} so ticks run on an executor rather
 * than on the shared timer thread, under a
 * {@code ClusterAwareScheduledTaskRunner} keyed by {@link #getTaskName()}
 * so that in an HA deployment one node drains a given kind at a time.
 * The key is unique per {@code entryKind}; the runner's default (the
 * class name) is shared by every drainer instance and would make them
 * exclude each other.
 *
 * <p>Transaction layout of one tick. The session handed to
 * {@link #run} is used only to reach the session factory and the
 * delivery executor; it needs no transaction, and
 * {@link OutboxDrainerRunner} indeed runs the tick without one. All
 * database work, the tick-level listener callbacks included, runs in
 * short transactions the drainer opens itself, and no session is bound
 * to the tick thread while a delivery executes:
 * <ol>
 *   <li><b>Claim</b> — one short transaction locks up to
 *       {@link OutboxConfig#batchSize()} due rows ({@code FOR UPDATE
 *       SKIP LOCKED}), stamps a per-tick token and a lease end
 *       ({@code claimed_until} = now + {@link OutboxConfig#claimLease()});
 *       rows with a live lease are not due for other ticks, and
 *       neither is any row of an owner that has a row in flight, so
 *       an owner is delivered by one tick at a time even when a tick
 *       outlives the cluster lock while delivering. That fence reads
 *       committed leases; it does not serialize two claim
 *       transactions running at the same instant, which is what the
 *       cluster lock of {@link OutboxDrainerRunner} prevents: it is
 *       held at least for the claim lease, taken right before the
 *       claim. After commit no row lock is held. With
 *       {@link OutboxConfig#perOwnerBatchSize()} set the rows are taken
 *       owner by owner, oldest-waiting owner first and at most that
 *       many per owner per round, so one destination cannot fill the
 *       batch (see {@link #claimOwnerFair}).</li>
 *   <li><b>Prepare</b> — per row, in a short transaction, the handler's
 *       {@link OutboxDeliveryHandler#prepare prepare} resolves what the
 *       delivery needs. The row is re-read first; if it no longer
 *       carries the tick's token, is no longer PENDING or no longer of
 *       this kind (taken over by another tick, or held, dead-lettered,
 *       re-armed or migrated by an admin) the row is skipped.</li>
 *   <li><b>Deliver</b> — {@link OutboxDelivery#execute()} runs outside
 *       any transaction, with no session bound to the thread. No
 *       connection is held while a destination is being called, and a
 *       slow destination blocks neither other rows nor other ticks.</li>
 *   <li><b>Record</b> — per row, in another short transaction, the
 *       row is re-read with a write lock and the transition applied
 *       only if it still carries the tick's token and passes the same
 *       status and kind check. The result's optional
 *       {@link OutboxDeliveryResult#effect() effect} runs in this
 *       transaction after the transition; if it throws, the record is
 *       rolled back and the row is retried after its lease. A lease that expired mid-delivery lets
 *       another tick re-claim the row; the late tick's record is then
 *       skipped rather than overwriting the newer attempt, and so is a
 *       result for a row an admin transitioned meanwhile.</li>
 *   <li><b>Budget</b> — {@link OutboxConfig#tickBudget()} starts when
 *       the claim has returned and is checked before each row; the
 *       first row of a tick runs regardless, so a tick always makes
 *       progress. Claimed rows the tick does not get to are handed
 *       back (token and lease cleared, still due) in one
 *       transaction.</li>
 *   <li><b>Housekeeping</b> — one final transaction expires queued rows
 *       older than {@link OutboxConfig#pendingMaxAge()} to DEAD_LETTER,
 *       row by row and at most {@link OutboxConfig#batchSize()} per
 *       tick so each reaches {@link OutboxDrainerListener#onDeadLetter}
 *       with cause {@code PENDING_MAX_AGE}, and purges DELIVERED /
 *       DEAD_LETTER rows past their retention.</li>
 * </ol>
 *
 * <p>With {@link OutboxConfig#deliveryConcurrency()} above one, the
 * claimed rows are grouped by owner and the groups are spread over
 * that many workers, the tick thread being one of them and the rest
 * coming from the {@code outbox-delivery-<kind>} executor; one owner's
 * rows stay in order on one worker, different owners are delivered in
 * parallel.
 *
 * <p>Row-level listener callbacks run after the row's record
 * transaction committed, in a short transaction of their own and with
 * a detached row, so a listener failure (a JPA exception included,
 * which would mark a shared transaction rollback-only) can never undo
 * a transition. With delivery workers they run on the worker thread.
 *
 * <p>Crash safety: a node that dies mid-tick leaves its claimed rows
 * with a token and a lease end; once the lease expires they are due
 * again and the next tick re-claims them. No reclaim sweep is needed.
 * Delivery is at-least-once: a delivery that completed but whose
 * record transaction was lost is repeated after the lease.
 *
 * <p>An optional {@link OutboxDrainerListener} observes the tick and
 * every row transition (metrics, dead-letter alerting). Listener
 * failures are logged and never affect the transition.
 *
 * <p>Logging: WARN lines carry ids, kind, correlation id and the cause
 * only. The handler-supplied reason can contain a destination's
 * response text and is logged at DEBUG; it is persisted in
 * {@code last_error} for admin tooling.
 */
public class OutboxDrainerTask implements ScheduledTask {

    private static final Logger log = Logger.getLogger(OutboxDrainerTask.class);

    public static final String TASK_NAME_PREFIX = "outbox-drainer:";

    /** Executor name prefix for delivery workers; the kind is appended. */
    public static final String DELIVERY_EXECUTOR_PREFIX = "outbox-delivery-";

    /** {@code last_error} of rows expired by the {@code pendingMaxAge} backstop. */
    public static final String STALE_REASON = "queued exceeded pendingMaxAge";

    protected final OutboxConfig config;
    protected final OutboxDeliveryHandler handler;
    protected final Function<KeycloakSession, OutboxStore> storeFactory;
    protected final OutboxDrainerListener listener;

    /**
     * Uses the handler as {@link OutboxDrainerListener} if it
     * implements that interface, otherwise no listener.
     */
    public OutboxDrainerTask(OutboxConfig config,
                             OutboxDeliveryHandler handler,
                             Function<KeycloakSession, OutboxStore> storeFactory) {
        this(config, handler, storeFactory,
                handler instanceof OutboxDrainerListener l ? l : OutboxDrainerListener.NOOP);
    }

    public OutboxDrainerTask(OutboxConfig config,
                             OutboxDeliveryHandler handler,
                             Function<KeycloakSession, OutboxStore> storeFactory,
                             OutboxDrainerListener listener) {
        this.config = Objects.requireNonNull(config, "config");
        this.handler = Objects.requireNonNull(handler, "handler");
        this.storeFactory = Objects.requireNonNull(storeFactory, "storeFactory");
        this.listener = listener == null ? OutboxDrainerListener.NOOP : listener;
        if (!Objects.equals(config.entryKind(), handler.entryKind())) {
            throw new IllegalArgumentException(
                    "config.entryKind=" + config.entryKind()
                            + " does not match handler.entryKind=" + handler.entryKind());
        }
    }

    public OutboxConfig getConfig() {
        return config;
    }

    /**
     * Unique per {@code entryKind}; used as the cluster lock key and
     * as the timer / tracing name.
     */
    @Override
    public String getTaskName() {
        return TASK_NAME_PREFIX + config.entryKind();
    }

    /**
     * Runs one tick. {@code session} only provides the session factory
     * and the delivery executor; whether it has a transaction is
     * irrelevant, and it is not bound to the thread while the tick
     * runs, so a delivery never sees a caller's session or connection.
     */
    @Override
    public void run(KeycloakSession session) {
        KeycloakSessionFactory factory = session.getKeycloakSessionFactory();
        KeycloakSession previous = KeycloakSessionUtil.getKeycloakSession();
        KeycloakSessionUtil.setKeycloakSession(null);
        Instant tickStart = Instant.now();
        TickCounters counters = new TickCounters();
        notifyInTransaction(factory, s -> listener.onTickStart(s, config.entryKind()));
        try {
            drain(session, factory, counters);
            housekeeping(factory, counters);
        } finally {
            OutboxDrainerTickSummary summary = counters.summary(config.entryKind(),
                    Duration.between(tickStart, Instant.now()));
            notifyInTransaction(factory, s -> listener.onTickEnd(s, summary));
            KeycloakSessionUtil.setKeycloakSession(previous);
        }
    }

    protected void drain(KeycloakSession session, KeycloakSessionFactory factory, TickCounters counters) {
        String token = UUID.randomUUID().toString();
        List<OutboxEntryEntity> claimed = KeycloakModelUtils.runJobInTransactionWithResult(factory,
                s -> claimBatch(storeFactory.apply(s), token));
        counters.claimed.set(claimed.size());
        if (claimed.isEmpty()) {
            return;
        }
        log.debugf("Outbox drainer claimed %d due row(s) for entryKind=%s token=%s", claimed.size(), config.entryKind(), token);

        // The budget bounds delivering, not claiming: a slow claim must
        // not leave the tick with no time for even its first row, which
        // would release the whole batch tick after tick.
        Instant deadline = config.tickBudget() == null ? null : Instant.now().plus(config.tickBudget());
        List<String> unprocessed = Collections.synchronizedList(new ArrayList<>());
        int concurrency = config.deliveryConcurrency() == null ? 1 : config.deliveryConcurrency();
        if (concurrency > 1 && claimed.size() > 1) {
            processConcurrently(session, factory, claimed, token, deadline, concurrency, counters, unprocessed);
        } else {
            processRows(factory, claimed, token, deadline, counters, unprocessed);
        }
        if (!unprocessed.isEmpty()) {
            releaseUnprocessed(factory, new ArrayList<>(unprocessed), token, counters);
        }
    }

    protected List<OutboxEntryEntity> claimBatch(OutboxStore store, String token) {
        if (config.perOwnerBatchSize() == null) {
            return store.claimDueForDrain(config.entryKind(), config.batchSize(), token, config.claimLease());
        }
        return claimOwnerFair(store, token);
    }

    /**
     * Owner-fair claim: visits the owners that have due rows, oldest
     * waiting first, and takes at most {@code perOwnerBatchSize} rows
     * from each per round. Rounds repeat over the owners that still
     * had a full share until the batch is full or no owner has due
     * rows left, so a batch with few active owners is still filled.
     * The resulting list interleaves owners in round order, which also
     * spreads a budget-limited tick's deliveries across owners.
     */
    protected List<OutboxEntryEntity> claimOwnerFair(OutboxStore store, String token) {
        int batchSize = config.batchSize();
        int perOwner = Math.min(config.perOwnerBatchSize(), batchSize);
        List<String> owners = store.findOwnersWithDueRows(config.entryKind(), batchSize);
        List<OutboxEntryEntity> claimed = new ArrayList<>(batchSize);
        List<String> candidates = owners;
        while (!candidates.isEmpty() && claimed.size() < batchSize) {
            List<String> stillDue = new ArrayList<>(candidates.size());
            for (String owner : candidates) {
                int remaining = batchSize - claimed.size();
                if (remaining <= 0) {
                    break;
                }
                int limit = Math.min(perOwner, remaining);
                List<OutboxEntryEntity> rows = store.claimDueForOwner(config.entryKind(), owner, limit, token, config.claimLease());
                claimed.addAll(rows);
                if (rows.size() == limit) {
                    // Took a full share: the owner may have more.
                    stillDue.add(owner);
                }
            }
            candidates = stillDue;
        }
        if (!claimed.isEmpty()) {
            log.debugf("Outbox owner-fair claim took %d row(s) across %d owner(s) for entryKind=%s", claimed.size(), owners.size(), config.entryKind());
        }
        return claimed;
    }

    /**
     * Processes rows in order on the calling thread, stopping at the
     * deadline; rows not reached are reported as unprocessed. The
     * first row of the tick is exempt from the deadline, so a budget
     * shorter than the claim, the executor setup or the thread
     * scheduling still lets the tick make progress.
     */
    protected void processRows(KeycloakSessionFactory factory, List<OutboxEntryEntity> rows, String token,
                               Instant deadline, TickCounters counters, List<String> unprocessed) {
        boolean budgetExhausted = false;
        for (OutboxEntryEntity row : rows) {
            if (!budgetExhausted && deadline != null && !Instant.now().isBefore(deadline)) {
                budgetExhausted = !counters.firstRowStarted.compareAndSet(false, true);
            }
            if (budgetExhausted) {
                unprocessed.add(row.getId());
                continue;
            }
            counters.firstRowStarted.set(true);
            processClaimed(factory, row, token, counters);
        }
    }

    /**
     * Groups the rows by owner (keeping each owner's order) and spreads
     * the groups over up to {@code concurrency} workers: the tick
     * thread itself plus up to {@code concurrency - 1} threads of the
     * kind's delivery executor, so at most {@code concurrency}
     * deliveries run at once. Falls back to sequential processing when
     * the executor is unavailable or rejects work.
     */
    protected void processConcurrently(KeycloakSession session, KeycloakSessionFactory factory,
                                       List<OutboxEntryEntity> claimed, String token, Instant deadline,
                                       int concurrency, TickCounters counters, List<String> unprocessed) {
        Map<String, List<OutboxEntryEntity>> byOwner = new LinkedHashMap<>();
        for (OutboxEntryEntity row : claimed) {
            byOwner.computeIfAbsent(row.getOwnerId(), k -> new ArrayList<>()).add(row);
        }
        Queue<List<OutboxEntryEntity>> groups = new ConcurrentLinkedQueue<>(byOwner.values());
        // One worker is the tick thread (see below).
        int workers = Math.min(concurrency, byOwner.size()) - 1;
        ExecutorService executor;
        try {
            executor = session.getProvider(ExecutorsProvider.class).getExecutor(DELIVERY_EXECUTOR_PREFIX + config.entryKind());
        } catch (RuntimeException e) {
            log.warnf(e, "Outbox delivery executor unavailable for entryKind=%s — delivering sequentially", config.entryKind());
            processRows(factory, claimed, token, deadline, counters, unprocessed);
            return;
        }
        List<Future<?>> futures = new ArrayList<>(workers);
        try {
            for (int i = 0; i < workers; i++) {
                futures.add(executor.submit(() -> {
                    List<OutboxEntryEntity> group;
                    while ((group = groups.poll()) != null) {
                        processRows(factory, group, token, deadline, counters, unprocessed);
                    }
                }));
            }
        } catch (RejectedExecutionException e) {
            log.warnf("Outbox delivery executor rejected work for entryKind=%s — remaining owners delivered on the tick thread", config.entryKind());
        }
        // The tick thread is the remaining worker; it also picks up
        // whatever a starved pool leaves behind, so the batch always
        // drains.
        List<OutboxEntryEntity> group;
        while ((group = groups.poll()) != null) {
            processRows(factory, group, token, deadline, counters, unprocessed);
        }
        // Every submitted worker is awaited, interruption or not: the
        // tick must not move on to housekeeping, report its summary and
        // release the cluster lock and the overlap guard while workers
        // are still delivering. An interruption is remembered and the
        // flag restored once the workers are done.
        boolean interrupted = false;
        for (Future<?> future : futures) {
            while (true) {
                try {
                    future.get();
                    break;
                } catch (InterruptedException e) {
                    if (!interrupted) {
                        log.warnf("Outbox drainer interrupted while waiting for delivery workers — waiting for them to finish, entryKind=%s", config.entryKind());
                    }
                    interrupted = true;
                } catch (ExecutionException e) {
                    log.warnf(e.getCause(), "Outbox delivery worker failed for entryKind=%s", config.entryKind());
                    break;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Prepares, delivers and records one claimed row, each phase in its
     * own transaction or none. Never throws: a failure to record leaves
     * the row leased, and the lease expiry turns it into a retry.
     */
    protected void processClaimed(KeycloakSessionFactory factory, OutboxEntryEntity claimedRow, String token,
                                  TickCounters counters) {
        counters.processed.incrementAndGet();
        String id = claimedRow.getId();

        OutboxDelivery delivery = prepareInOwnTransaction(factory, id, token);
        if (delivery == null) {
            counters.claimLost.incrementAndGet();
            log.debugf("Outbox claim lost before delivery — skipping. id=%s entryKind=%s token=%s",
                    id, config.entryKind(), token);
            return;
        }

        OutboxDeliveryResult result = executeSafely(delivery, id);

        // Counters and listener callbacks are deferred until the record
        // transaction has committed: a listener must not be able to
        // roll the transition back, and the counters must reflect what
        // was actually recorded.
        Runnable[] afterCommit = new Runnable[1];
        try {
            KeycloakModelUtils.runJobInTransaction(factory, s -> {
                OutboxStore store = storeFactory.apply(s);
                OutboxEntryEntity row = store.findClaimed(id, token);
                if (row == null || !holdsClaim(row, token)) {
                    counters.claimLost.incrementAndGet();
                    log.warnf("Outbox claim lost after delivery — outcome %s not recorded, the row's new holder or state wins (lease expired or admin transition). id=%s entryKind=%s token=%s",
                            result.outcome(), id, config.entryKind(), token);
                    return;
                }
                afterCommit[0] = applyResult(factory, store, row, result, counters);
                if (result.effect() != null) {
                    // Consumer state that must commit with the transition;
                    // a failure here rolls the record back (see the catch).
                    result.effect().accept(s);
                }
            });
        } catch (RuntimeException e) {
            afterCommit[0] = null;
            log.warnf("Outbox failed to record outcome %s (%s) — row stays leased and is retried after the lease. id=%s entryKind=%s",
                    result.outcome(), e.getClass().getSimpleName(), id, config.entryKind());
            log.debugf(e, "Outbox record failure detail. id=%s", id);
        }
        if (afterCommit[0] != null) {
            afterCommit[0].run();
        }
    }

    /**
     * Whether the row is still this tick's to deliver and record: it
     * carries the tick's token, is still PENDING and still of this
     * kind. The token alone is not enough, because an admin operation
     * that holds, dead-letters, re-arms or migrates the row between
     * claim and record must win over the in-flight delivery.
     */
    protected boolean holdsClaim(OutboxEntryEntity row, String token) {
        return token.equals(row.getClaimToken())
                && row.getStatus() == OutboxEntryStatus.PENDING
                && config.entryKind().equals(row.getEntryKind());
    }

    /**
     * Runs the handler's prepare phase in a fresh transaction. Returns
     * {@code null} if the row no longer {@link #holdsClaim holds} the
     * tick's claim. A {@code null} delivery or an exception from the
     * handler becomes a settled RETRY. The delivery is kept even if the
     * transaction fails to commit afterwards (only handler-side
     * database writes, if any, were lost).
     */
    protected OutboxDelivery prepareInOwnTransaction(KeycloakSessionFactory factory, String id, String token) {
        OutboxDelivery[] holder = new OutboxDelivery[1];
        boolean[] claimLost = new boolean[1];
        try {
            KeycloakModelUtils.runJobInTransaction(factory, s -> {
                OutboxEntryEntity row = storeFactory.apply(s).findById(id);
                if (row == null || !holdsClaim(row, token)) {
                    claimLost[0] = true;
                    return;
                }
                holder[0] = prepareSafely(s, row);
            });
        } catch (RuntimeException e) {
            if (holder[0] != null) {
                log.warnf(e, "Outbox prepare transaction failed after the handler returned — keeping the delivery. id=%s entryKind=%s",
                        id, config.entryKind());
            } else {
                log.warnf(e, "Outbox prepare transaction failed — treating as RETRY. id=%s entryKind=%s", id, config.entryKind());
                holder[0] = OutboxDelivery.settled(OutboxDeliveryResult.retry(describe(e)));
            }
        }
        if (claimLost[0]) {
            return null;
        }
        return holder[0];
    }

    protected OutboxDelivery prepareSafely(KeycloakSession session, OutboxEntryEntity row) {
        try {
            OutboxDelivery delivery = handler.prepare(session, row);
            return delivery != null ? delivery
                    : OutboxDelivery.settled(OutboxDeliveryResult.retry("delivery handler returned null delivery"));
        } catch (RuntimeException e) {
            // Class only at WARN: the message may carry a destination's
            // response text, which stays at DEBUG and in last_error.
            log.warnf("Outbox delivery handler threw %s in prepare — treating as RETRY. id=%s entryKind=%s correlationId=%s",
                    e.getClass().getName(), row.getId(), row.getEntryKind(), row.getCorrelationId());
            log.debugf(e, "Outbox prepare failure detail. id=%s", row.getId());
            return OutboxDelivery.settled(OutboxDeliveryResult.retry(describe(e)));
        }
    }

    protected OutboxDeliveryResult executeSafely(OutboxDelivery delivery, String id) {
        try {
            OutboxDeliveryResult result = delivery.execute();
            return result != null ? result : OutboxDeliveryResult.retry("delivery returned null result");
        } catch (RuntimeException e) {
            log.warnf("Outbox delivery threw %s — treating as RETRY. id=%s entryKind=%s",
                    e.getClass().getName(), id, config.entryKind());
            log.debugf(e, "Outbox delivery failure detail. id=%s", id);
            return OutboxDeliveryResult.retry(describe(e));
        }
    }

    protected static String describe(RuntimeException e) {
        return e.getMessage() == null
                ? e.getClass().getSimpleName()
                : e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    /**
     * Applies the transition for {@code result} to the (locked) row in
     * the current transaction and returns the work to do once that
     * transaction has committed: the counter update and the listener
     * callback.
     */
    protected Runnable applyResult(KeycloakSessionFactory factory, OutboxStore store, OutboxEntryEntity row,
                                   OutboxDeliveryResult result, TickCounters counters) {
        switch (result.outcome()) {
            case DELIVERED -> {
                store.markDelivered(row);
                log.debugf("Outbox delivered. id=%s entryKind=%s correlationId=%s attempts=%d",
                        row.getId(), row.getEntryKind(), row.getCorrelationId(), row.getAttempts());
                return () -> {
                    counters.delivered.incrementAndGet();
                    notifyRow(factory, s -> listener.onDelivered(s, row));
                };
            }
            case RETRY -> {
                return handleRetry(factory, store, row, result.errorMessage(), counters);
            }
            case DEFER -> {
                store.deferUntil(row, result.notBefore(), result.errorMessage());
                log.debugf("Outbox deferred without counting an attempt. id=%s entryKind=%s correlationId=%s notBefore=%s reason=%s",
                        row.getId(), row.getEntryKind(), row.getCorrelationId(), result.notBefore(), result.errorMessage());
                Instant notBefore = row.getNextAttemptAt();
                return () -> {
                    counters.deferred.incrementAndGet();
                    notifyRow(factory, s -> listener.onDeferred(s, row, notBefore, result.errorMessage()));
                };
            }
            case DEAD_LETTER -> {
                String reason = result.errorMessage() != null ? result.errorMessage()
                        : "handler returned DEAD_LETTER (attempt " + (row.getAttempts() + 1) + ")";
                log.warnf("Outbox dead-lettered by handler. id=%s entryKind=%s correlationId=%s",
                        row.getId(), row.getEntryKind(), row.getCorrelationId());
                log.debugf("Outbox dead-letter reason. id=%s reason=%s", row.getId(), reason);
                return deadLetter(factory, store, row, OutboxDrainerListener.DeadLetterCause.HANDLER, reason, counters);
            }
            case ORPHANED -> {
                String reason = result.errorMessage() != null ? result.errorMessage()
                        : "handler returned ORPHANED (destination no longer exists)";
                log.warnf("Outbox dead-lettered as orphan. id=%s entryKind=%s correlationId=%s",
                        row.getId(), row.getEntryKind(), row.getCorrelationId());
                log.debugf("Outbox dead-letter reason. id=%s reason=%s", row.getId(), reason);
                return deadLetter(factory, store, row, OutboxDrainerListener.DeadLetterCause.ORPHANED, reason, counters);
            }
            default -> throw new IllegalStateException("unknown outcome " + result.outcome());
        }
    }

    protected Runnable handleRetry(KeycloakSessionFactory factory, OutboxStore store, OutboxEntryEntity row,
                                   String errorMessage, TickCounters counters) {
        int nextAttempts = row.getAttempts() + 1;
        String reason = errorMessage != null ? errorMessage : "delivery failed";
        if (config.backoff().isExhausted(nextAttempts)) {
            log.warnf("Outbox dead-lettered after %d attempts. id=%s entryKind=%s correlationId=%s",
                    nextAttempts, row.getId(), row.getEntryKind(), row.getCorrelationId());
            log.debugf("Outbox dead-letter reason. id=%s reason=%s", row.getId(), reason);
            return deadLetter(factory, store, row, OutboxDrainerListener.DeadLetterCause.ATTEMPTS_EXHAUSTED, reason, counters);
        }
        Instant nextAttemptAt = config.backoff().computeNextAttemptAt(Instant.now(), nextAttempts);
        log.debugf("Outbox scheduling retry. id=%s attempts=%d nextAttemptAt=%s reason=%s",
                row.getId(), nextAttempts, nextAttemptAt, reason);
        store.recordFailure(row, nextAttemptAt, reason);
        return () -> {
            counters.retried.incrementAndGet();
            notifyRow(factory, s -> listener.onRetryScheduled(s, row, nextAttemptAt, reason));
        };
    }

    protected Runnable deadLetter(KeycloakSessionFactory factory, OutboxStore store, OutboxEntryEntity row,
                                  OutboxDrainerListener.DeadLetterCause cause, String reason, TickCounters counters) {
        store.markDeadLetter(row, reason);
        return () -> {
            counters.deadLettered.incrementAndGet();
            notifyRow(factory, s -> listener.onDeadLetter(s, row, cause, reason));
        };
    }

    protected void releaseUnprocessed(KeycloakSessionFactory factory, List<String> ids, String token,
                                      TickCounters counters) {
        try {
            KeycloakModelUtils.runJobInTransaction(factory,
                    s -> counters.released.set(storeFactory.apply(s).releaseClaims(ids, token)));
            log.infof("Outbox tick budget %s exhausted for entryKind=%s — released %d of %d claimed row(s) for the next tick",
                    config.tickBudget(), config.entryKind(), counters.released.get(), ids.size());
        } catch (RuntimeException e) {
            log.warnf(e, "Outbox failed to release %d unprocessed claimed row(s) — they become due again after the lease. entryKind=%s",
                    ids.size(), config.entryKind());
        }
    }

    protected void housekeeping(KeycloakSessionFactory factory, TickCounters counters) {
        List<OutboxEntryEntity> expired = new ArrayList<>();
        try {
            KeycloakModelUtils.runJobInTransaction(factory, s -> {
                OutboxStore store = storeFactory.apply(s);
                expired.addAll(expireStaleQueued(store));
                counters.purgedDelivered.set(purgeDeliveredOlderThanRetention(store));
                counters.purgedDeadLetter.set(purgeDeadLetterOlderThanRetention(store));
            });
        } catch (RuntimeException e) {
            expired.clear();
            log.warnf(e, "Outbox housekeeping failed for entryKind=%s — retried on the next tick", config.entryKind());
        }
        counters.stalePromoted.set(expired.size());
        for (OutboxEntryEntity row : expired) {
            notifyRow(factory, s -> listener.onDeadLetter(s, row, OutboxDrainerListener.DeadLetterCause.PENDING_MAX_AGE, STALE_REASON));
        }
    }

    /**
     * Runs a listener callback, logging and swallowing any failure so
     * an observer bug cannot change a row's transition or abort the
     * tick.
     */
    protected void notify(Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException e) {
            log.warnf(e, "Outbox drainer listener threw for entryKind=%s — ignoring", config.entryKind());
        }
    }

    /**
     * Runs a listener callback in a short transaction of its own
     * (listeners such as the metrics listener read the realm or the
     * depth per realm), with the same failure isolation as
     * {@link #notify}; a transaction the callback breaks is rolled
     * back without affecting anything else.
     */
    protected void notifyInTransaction(KeycloakSessionFactory factory, KeycloakSessionTask callback) {
        notify(() -> KeycloakModelUtils.runJobInTransaction(factory, callback));
    }

    /**
     * {@link #notifyInTransaction} for row-level callbacks, skipped
     * for the no-op listener so a drainer without observers pays no
     * extra transaction per row.
     */
    protected void notifyRow(KeycloakSessionFactory factory, KeycloakSessionTask callback) {
        if (listener == OutboxDrainerListener.NOOP) {
            return;
        }
        notifyInTransaction(factory, callback);
    }

    /**
     * Expires queued rows older than {@link OutboxConfig#pendingMaxAge()}
     * to DEAD_LETTER, one by one so each can reach the listener with
     * cause {@link OutboxDrainerListener.DeadLetterCause#PENDING_MAX_AGE}
     * once the transaction committed. Bounded to
     * {@link OutboxConfig#batchSize()} rows per tick; a larger backlog
     * drains over the following ticks.
     *
     * @return the expired rows, for the listener callbacks.
     */
    protected List<OutboxEntryEntity> expireStaleQueued(OutboxStore store) {
        Duration pendingMaxAge = config.pendingMaxAge();
        if (pendingMaxAge == null || pendingMaxAge.isZero() || pendingMaxAge.isNegative()) {
            return List.of();
        }
        Instant cutoff = Instant.now().minus(pendingMaxAge);
        List<OutboxEntryEntity> stale = store.lockStaleQueued(config.entryKind(), cutoff, config.batchSize());
        for (OutboxEntryEntity row : stale) {
            store.expireQueued(row, STALE_REASON);
        }
        if (!stale.isEmpty()) {
            log.infof("Outbox expired %d stale queued row(s) to DEAD_LETTER (entryKind=%s, pendingMaxAge=%s)",
                    stale.size(), config.entryKind(), pendingMaxAge);
        }
        return stale;
    }

    protected int purgeDeliveredOlderThanRetention(OutboxStore store) {
        Duration retention = config.deliveredRetention();
        if (retention == null || retention.isZero() || retention.isNegative()) {
            return 0;
        }
        return store.purgeDeliveredOlderThan(config.entryKind(), Instant.now().minus(retention));
    }

    protected int purgeDeadLetterOlderThanRetention(OutboxStore store) {
        Duration retention = config.deadLetterRetention();
        if (retention == null || retention.isZero() || retention.isNegative()) {
            return 0;
        }
        return store.purgeDeadLetterOlderThan(config.entryKind(), Instant.now().minus(retention));
    }

    /**
     * Per-tick counters, updated from delivery workers too, folded into
     * an {@link OutboxDrainerTickSummary} at tick end.
     */
    protected static class TickCounters {
        final AtomicInteger claimed = new AtomicInteger();
        final AtomicInteger processed = new AtomicInteger();
        final AtomicInteger delivered = new AtomicInteger();
        final AtomicInteger retried = new AtomicInteger();
        final AtomicInteger deferred = new AtomicInteger();
        final AtomicInteger deadLettered = new AtomicInteger();
        final AtomicInteger released = new AtomicInteger();
        final AtomicInteger claimLost = new AtomicInteger();
        final AtomicInteger stalePromoted = new AtomicInteger();
        final AtomicInteger purgedDelivered = new AtomicInteger();
        final AtomicInteger purgedDeadLetter = new AtomicInteger();
        /** Whether a row was let through the budget check yet; the first one always is. */
        final AtomicBoolean firstRowStarted = new AtomicBoolean();

        OutboxDrainerTickSummary summary(String entryKind, Duration duration) {
            return new OutboxDrainerTickSummary(entryKind, claimed.get(), processed.get(), delivered.get(),
                    retried.get(), deferred.get(), deadLettered.get(), released.get(), claimLost.get(),
                    stalePromoted.get(), purgedDelivered.get(), purgedDeadLetter.get(), duration);
        }
    }
}
