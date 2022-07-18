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
import org.keycloak.models.UserSessionModel;

import java.util.HashSet;
import java.util.Set;

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

    public static boolean isEnabled(KeycloakSession session, Class<?> model) {
        Set<Class<?>> lockedModels = getAttribute(session);
        return lockedModels != null && lockedModels.contains(model);
    }

    private static Set<Class<?>> getAttribute(KeycloakSession session) {
        //noinspection unchecked
        return (Set<Class<?>>) session.getAttribute(ATTRIBUTE);
    }

    private static Set<Class<?>> getOrCreateAttribute(KeycloakSession session) {
        Set<Class<?>> attribute = getAttribute(session);
        if (attribute == null) {
            attribute = new HashSet<>();
            session.setAttribute(ATTRIBUTE, attribute);
        }
        return attribute;
    }

    public static <V> V lockUserSessionsForModification(KeycloakSession session, CallableWithoutThrowingAnException<V> callable) {
        return lockObjectsForModification(session, UserSessionModel.class, callable);
    }

    private static <V> V lockObjectsForModification(KeycloakSession session, Class<?> model, CallableWithoutThrowingAnException<V> callable) {
        if (LockObjectsForModification.isEnabled(session, model)) {
            // If someone nests the call, and it would already be locked, don't try to lock it a second time.
            // Otherwise, the inner unlocking might also unlock the outer lock.
            return callable.call();
        }
        try (LockObjectsForModification.Enabled ignored = new Enabled(session, model)) {
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
        private final Class<?> model;

        public Enabled(KeycloakSession session, Class<?> model) {
            this.session = session;
            this.model = model;
            getOrCreateAttribute(session).add(model);
        }

        @Override
        public void close() {
            getOrCreateAttribute(session).remove(model);
        }
    }
}
