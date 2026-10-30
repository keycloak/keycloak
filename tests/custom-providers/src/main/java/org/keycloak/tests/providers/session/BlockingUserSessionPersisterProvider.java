/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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

package org.keycloak.tests.providers.session;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.EntityManager;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.jpa.session.JpaUserSessionPersisterProvider;

public class BlockingUserSessionPersisterProvider extends JpaUserSessionPersisterProvider {

    private static final AtomicReference<Block> BLOCK = new AtomicReference<>();

    public BlockingUserSessionPersisterProvider(KeycloakSession session, EntityManager entityManager, int expirationBatch) {
        super(session, entityManager, expirationBatch);
    }

    public static void blockAfterLoading(String sessionId) {
        BLOCK.set(new Block(sessionId));
    }

    public static boolean hasLoaded() {
        Block block = BLOCK.get();
        return block != null && block.loaded.getCount() == 0;
    }

    public static void release() {
        Block block = BLOCK.getAndSet(null);
        if (block != null) {
            block.release.countDown();
        }
    }

    @Override
    public UserSessionModel loadUserSession(RealmModel realm, String userSessionId, boolean offline) {
        UserSessionModel loaded = super.loadUserSession(realm, userSessionId, offline);
        blockAfterMaterializingChildren(loaded, userSessionId);
        return loaded;
    }

    private static void blockAfterMaterializingChildren(UserSessionModel loaded, String userSessionId) {
        Block block = BLOCK.get();
        if (loaded != null && block != null && Objects.equals(block.sessionId, userSessionId)
                && block.claimed.compareAndSet(false, true)) {
            // The JPA adapter loads client sessions lazily. Materialize them before signalling so
            // the test can deterministically race a mutation against both parent and child snapshots.
            loaded.getAuthenticatedClientSessions().size();
            block.loaded.countDown();
            try {
                if (!block.release.await(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release blocked session load");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting to release blocked session load", e);
            }
        }
    }

    private static class Block {
        private final String sessionId;
        private final AtomicBoolean claimed = new AtomicBoolean();
        private final CountDownLatch loaded = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        private Block(String sessionId) {
            this.sessionId = sessionId;
        }
    }
}
