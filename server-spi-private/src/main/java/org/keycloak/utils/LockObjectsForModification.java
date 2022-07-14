/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.utils;

import org.keycloak.models.KeycloakSession;

/**
 * This flags the session that all information loaded from the stores should be locked as the service layer
 * plans to modify it.
 *
 * This is just a hint to the underlying storage, and a store might choose to ignore it.
 * The lock for any object retrieved from the session will be kept until the end of the transaction.
 *
 * If the store supports it, this could prevent exceptions due to optimistic locking
 * problems later in the processing. If the caller retrieved objects without this wrapper, they would still be
 * able to modify those objects, and those changes would be written to the store at the end of the transaction at the lastet,
 * but they won't be locked.
 *
 *
 * @author Alexander Schwartz
 */
public class LockObjectsForModification {

    private static final String ATTRIBUTE = LockObjectsForModification.class.getCanonicalName();

    public static LockObjectsForModification.Enabled enable(KeycloakSession session) {
        return new Enabled(session);
    }

    public static boolean isEnabled(KeycloakSession session) {
        return session.getAttribute(ATTRIBUTE) != null;
    }

    public static <V> V lockObjectsForModification(KeycloakSession session, CallableWithoutThrowingAnException<V> callable) {
        if (LockObjectsForModification.isEnabled(session)) {
            // If someone nests the call, and it would already be locked, don't try to lock it a second time.
            // Otherwise, the inner unlocking might also unlock the outer lock.
            return callable.call();
        }
        try (LockObjectsForModification.Enabled ignored = LockObjectsForModification.enable(session)) {
            return callable.call();
        }
    }

    @FunctionalInterface
    public interface CallableWithoutThrowingAnException<V> {
        /**
         * Computes a result.
         *
         * @return computed result
         */
        V call();
    }

    public static class Enabled implements AutoCloseable {

        private final KeycloakSession session;

        public Enabled(KeycloakSession session) {
            this.session = session;
            session.setAttribute(ATTRIBUTE, Boolean.TRUE);
        }

        @Override
        public void close() {
            session.removeAttribute(ATTRIBUTE);
        }
    }
}
