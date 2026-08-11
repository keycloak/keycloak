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

import java.util.Objects;
import java.util.function.Function;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;

import org.jboss.logging.Logger;

/**
 * Background cleanup task that drains outbox rows owned by a removed
 * realm or owner (e.g. receiver client). Runs in a single bounded
 * transaction so the admin's original removal transaction can commit
 * immediately instead of carrying a six-digit {@code DELETE} on its
 * back.
 *
 * <p>Submitted by a feature's lifecycle listener (e.g. SSF's
 * {@code RealmRemovedEvent} / {@code ClientRemovedEvent} handler) to
 * a Keycloak-managed executor. The task itself opens a fresh session
 * via {@link KeycloakModelUtils#runJobInTransaction} so it doesn't
 * inherit the caller's transaction or session lifecycle.
 *
 * <p>Crash safety: if the node running this task dies mid-flight, the
 * remaining rows are orphaned. That's bounded by the
 * {@code pendingMaxAge} backstop on the drainer for queued rows and
 * by the configured retention windows for terminal rows — orphan rows
 * eventually get swept either way.
 */
public class OutboxCleanupTask implements Runnable {

    private static final Logger log = Logger.getLogger(OutboxCleanupTask.class);

    public enum Scope {
        REALM, OWNER
    }

    protected final KeycloakSessionFactory factory;
    protected final Function<KeycloakSession, OutboxStore> storeFactory;
    protected final String entryKind;
    protected final Scope scope;
    protected final String key;

    public OutboxCleanupTask(KeycloakSessionFactory factory,
                             Function<KeycloakSession, OutboxStore> storeFactory,
                             String entryKind,
                             Scope scope,
                             String key) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.storeFactory = Objects.requireNonNull(storeFactory, "storeFactory");
        this.entryKind = Objects.requireNonNull(entryKind, "entryKind");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.key = Objects.requireNonNull(key, "key");
    }

    @Override
    public void run() {
        try {
            int[] deletedHolder = new int[1];
            KeycloakModelUtils.runJobInTransaction(factory, session -> {
                OutboxStore store = storeFactory.apply(session);
                deletedHolder[0] = scope == Scope.REALM
                        ? store.deleteByRealm(entryKind, key)
                        : store.deleteByOwner(entryKind, key);
            });
            int deleted = deletedHolder[0];
            if (deleted > 0) {
                log.debugf("Outbox cleanup complete. entryKind=%s scope=%s key=%s deleted=%d",
                        entryKind, scope, key, deleted);
            }
        } catch (RuntimeException e) {
            // Don't re-throw: the executor would log it as an
            // uncaught exception. Orphan rows will be reaped by the
            // drainer's retention purges or the pendingMaxAge
            // backstop on a future tick.
            log.warnf(e, "Outbox cleanup task failed. entryKind=%s scope=%s key=%s", entryKind, scope, key);
        }
    }
}
