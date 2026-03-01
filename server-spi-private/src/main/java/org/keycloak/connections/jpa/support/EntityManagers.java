/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.keycloak.models.KeycloakSession;

import org.hibernate.Session;

public class EntityManagers {

    static final String ENTITY_MANAGER_PROXIES = "ENTITY_MANAGER_PROXIES";

    private static final ThreadLocal<Boolean> batchMode = new ThreadLocal<Boolean>();

    static void runInBatchMode(Runnable runnable) {
        boolean isBatched = isBatchMode();
        batchMode.set(true);
        try {
            runnable.run();
        } finally {
            if (!isBatched) {
                batchMode.remove();
            }
        }
    }

    public static boolean isBatchMode() {
        return Boolean.TRUE.equals(batchMode.get());
    }

    static void forEachEntityManager(KeycloakSession session, Consumer<EntityManager> op) {
        try {
            getEntityManagerProxies(session).map(EntityManagerProxy::getEntityManager)
                    .filter(EntityManager::isOpen).forEach(op);
        } catch (Exception e) {
            // this was run directly on the unproxied entitymanagers, so the exception will need converted
            throw EntityManagerProxy.convert(e);
        }
    }

    static Stream<EntityManagerProxy> getEntityManagerProxies(KeycloakSession session) {
        return Optional.ofNullable((Set<EntityManagerProxy>) session.getAttribute(ENTITY_MANAGER_PROXIES, Set.class))
                .map(Set::stream).orElse(Stream.of());
    }

    /**
     * Flush and optionally clear all the currently in use {@link EntityManager}s
     */
    public static void flush(KeycloakSession session, boolean clear) {
        forEachEntityManager(session, em -> {
            em.flush(); // TODO: avoid if read-only
            if (clear) {
                em.clear();
            }
        });
    }

    /**
     * Run the operation in batch mode with a pre-flush.
     * <p>
     * It is desirable to use nestedEntityManagers=true to keep the existing context free of newly created entities.
     * <p>
     * flush and detach operations are NOT automatically inhibited in batch mode. For even greater performance, and
     * statement level batching by Hibernate, you may use the {@link #isBatchMode()} to conditionally not perform those
     * operations - however keep in mind that especially when running with nestedEntityManagers=false, the current
     * persistence context will accumulate anything that is not detached.
     * <p>
     * WARNING: Any queries run while batching will be in COMMIT mode, so they cannot see non-flushed changes made
     * within the batch. Most of Keycloak's JPA code however persists and flushes together.
     *
     * @param nestedEntityManagers - if true run with isolated EntityManagers WARNING: Any entities passed into the task that
     *   that will get persisted must not already be associated with an open EntityManager.
     */
    public static void runInBatch(KeycloakSession session, Runnable runnable, boolean nestedEntityManagers) {
        Map<EntityManagerProxy, Session> previous = new HashMap<EntityManagerProxy, Session>();

        flush(session, false); // make sure the state entering the batch processing is committed

        // create a localized entitymanager with a shared transaction coordinator, so nothing is left behind
        if (nestedEntityManagers) {
            getEntityManagerProxies(session).forEach(p -> {
                if (!p.getEntityManager().isOpen()) {
                    return;
                }
                Session em = p.getEntityManager().unwrap(Session.class);
                Session derived = em.sessionWithOptions().connection().openSession();
                previous.put(p, em);
                p.setEntityManager(derived);
            });
        }

        try {
            runInBatchMode(runnable);
            if (nestedEntityManagers) {
                flush(session, true);
            }
        } finally {
            // restablish the old entitymanagers
            if (nestedEntityManagers) {
                getEntityManagerProxies(session).forEach(p -> {
                    EntityManager current = p.getEntityManager();
                    EntityManager old = previous.get(p);
                    if (old != null) {
                        if (current.isOpen()) {
                            current.close();
                        }
                        p.setEntityManager(old);
                    } // else - created during the batch run, so it's enough that it was flushed / cleared
                });
            }
        }
    }

}
