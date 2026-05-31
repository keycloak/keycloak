/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.sessions;

import java.util.Map;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface AuthenticationSessionProvider extends Provider {

    /**
     * Creates and registers a new authentication session with random ID. Authentication session
     * entity will be prefilled with current timestamp, the given realm and client.
     * @param realm {@code RealmModel} Can't be {@code null}.
     * @return Returns created {@code RootAuthenticationSessionModel}. Never returns {@code null}.
     */
    RootAuthenticationSessionModel createRootAuthenticationSession(RealmModel realm);

    /**
     * Creates a new root authentication session specified by the provided realm and id.
     *
     * @param realm {@code RealmModel} Can't be {@code null}.
     * @param id    {@code String} Id of newly created root authentication session. If {@code null} a random id will be
     *              generated.
     * @return Returns created {@code RootAuthenticationSessionModel}. Never returns {@code null}.
     * @implNote The JPA implementation will fail if {@link #getRootAuthenticationSession(RealmModel, String)} was
     * invoked previously in the same transaction for the same non-existent id, as the prior call may have acquired a
     * gap lock that blocks the insert.
     * @deprecated Use {@link #getOrCreateRootAuthenticationSession(RealmModel, String)} instead to avoid deadlocks in
     * the database.
     */
    @Deprecated(since = "26.7", forRemoval = true)
    RootAuthenticationSessionModel createRootAuthenticationSession(RealmModel realm, String id);

    /**
     * Atomically returns an existing root authentication session or creates a new one. This is the preferred method
     * when the caller has an id from an external source (e.g. a cookie) that may already exist in the store.
     *
     * @param realm {@code RealmModel} Can't be {@code null}.
     * @param id    {@code String} Id of the root authentication session. If {@code null} a new session with a random id
     *              is created.
     * @return Returns the existing or newly created {@code RootAuthenticationSessionModel}. Never returns {@code null}.
     * @implNote The JPA implementation should use a database-level upsert or a separate transaction for the insert to
     * avoid gap lock deadlocks on InnoDB (MySQL/MariaDB) and key-range lock conflicts on MSSQL.
     */
    default RootAuthenticationSessionModel getOrCreateRootAuthenticationSession(RealmModel realm, String id) {
        if (id == null) {
            return createRootAuthenticationSession(realm);
        }
        var existing = getRootAuthenticationSession(realm, id);
        if (existing != null) {
            return existing;
        }
        return createRootAuthenticationSession(realm, id);
    }

    /**
     * Returns the root authentication session specified by the provided realm and id.
     *
     * @param realm                   {@code RealmModel} Can't be {@code null}.
     * @param authenticationSessionId {@code RootAuthenticationSessionModel} If {@code null} then {@code null} will be
     *                                returned.
     * @return Returns found {@code RootAuthenticationSessionModel} or {@code null} if no root authentication session is
     * found.
     * @implNote The JPA implementation acquires a pessimistic write lock on the returned entity. On databases with gap
     * locking (InnoDB, MSSQL), this can acquire a gap lock if the row does not exist, which may lead to deadlocks if a
     * subsequent insert is attempted in the same transaction. Prefer
     * {@link #getOrCreateRootAuthenticationSession(RealmModel, String)} when the intent is to find or create.
     */
    RootAuthenticationSessionModel getRootAuthenticationSession(RealmModel realm, String authenticationSessionId);

    /**
     * Removes provided root authentication session.
     * @param realm {@code RealmModel} Associated realm to the given root authentication session.
     * @param authenticationSession {@code RootAuthenticationSessionModel} Can't be {@code null}.
     */
    void removeRootAuthenticationSession(RealmModel realm, RootAuthenticationSessionModel authenticationSession);

    /**
     * Remove expired authentication sessions in all the realms
     *
     * @deprecated manual removal of expired entities should not be used anymore. It is responsibility of the store
     *             implementation to handle expirable entities
     */
    @Deprecated(since = "19.0", forRemoval = true)
    default void removeAllExpired() {}

    /**
     * Removes all expired root authentication sessions for the given realm.
     * @param realm {@code RealmModel} Can't be {@code null}.
     *
     *
     * @deprecated manual removal of expired entities should not be used anymore. It is responsibility of the store
     *             implementation to handle expirable entities
     */
    @Deprecated(since = "19.0", forRemoval = true)
    default void removeExpired(RealmModel realm) {}

    /**
     * Removes all associated root authentication sessions to the given realm which was removed.
     * @param realm {@code RealmModel} Can't be {@code null}.
     */
    void onRealmRemoved(RealmModel realm);

    /**
     * Removes all associated root authentication sessions to the given realm and client which was removed.
     * @param realm {@code RealmModel} Can't be {@code null}.
     * @param client {@code ClientModel} Can't be {@code null}.
     * @deprecated to remove, all implementations are empty.
     */
    @Deprecated(since = "26.5", forRemoval = true)
    default void onClientRemoved(RealmModel realm, ClientModel client) {};

    /**
     * Requests update of authNotes of a root authentication session that is not owned
     * by this instance but might exist somewhere in the cluster.
     * 
     * @param compoundId {@code AuthenticationSessionCompoundId} The method has no effect if {@code null}.
     * @param authNotesFragment {@code Map<String, String>} Map with authNote values.
     * Auth note is removed if the corresponding value in the map is {@code null}. Map itself can't be {@code null}.
     * @deprecated For removal in Keycloak 27
     */
    @Deprecated(since = "26.3", forRemoval = true)
    default void updateNonlocalSessionAuthNotes(AuthenticationSessionCompoundId compoundId, Map<String, String> authNotesFragment) {}

    default void migrate(String modelVersion) {
    }
}
