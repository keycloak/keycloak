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

package org.keycloak.connections.jpa.support;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.models.KeycloakSession;

/**
 * Registry of listeners to notify after {@link EntityManagers#flush(KeycloakSession, boolean)} clears the
 * persistence context for a session. Kept separate from {@link EntityManagers} - which deals directly with
 * {@code jakarta.persistence.EntityManager} - so that callers which only need to register a listener for
 * session-scoped caches holding entity-backed models do not pull in the JPA API as a dependency.
 */
public class EntityManagerClearListeners {

    private static final String CLEAR_LISTENERS = "ENTITY_MANAGER_CLEAR_LISTENERS";

    @SuppressWarnings("unchecked")
    public static void onClear(KeycloakSession session, Runnable listener) {
        List<Runnable> listeners = session.getAttribute(CLEAR_LISTENERS, List.class);
        if (listeners == null) {
            listeners = new ArrayList<>();
            session.setAttribute(CLEAR_LISTENERS, listeners);
        }
        listeners.add(listener);
    }

    @SuppressWarnings("unchecked")
    static void notifyListeners(KeycloakSession session) {
        List<Runnable> listeners = session.getAttribute(CLEAR_LISTENERS, List.class);
        if (listeners != null) {
            listeners.forEach(Runnable::run);
        }
    }
}
