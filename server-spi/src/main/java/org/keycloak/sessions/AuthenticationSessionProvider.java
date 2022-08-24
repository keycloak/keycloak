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

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

import java.util.Map;

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
     * Creates a new root authentication session specified by the provided id and realm.
     * @param id {@code String} Id of newly created root authentication session. If {@code null} a random id will be generated.
     * @param realm {@code RealmModel} Can't be {@code null}.
     * @return Returns created {@code RootAuthenticationSessionModel}. Never returns {@code null}.
     * @deprecated Use {@link #createRootAuthenticationSession(RealmModel, String)} createRootAuthenticationSession} instead.
     */
    @Deprecated
    default RootAuthenticationSessionModel createRootAuthenticationSession(String id, RealmModel realm) {
        return createRootAuthenticationSession(realm, id);
    }

    /**
     * Creates a new root authentication session specified by the provided realm and id.
     * @param realm {@code RealmModel} Can't be {@code null}.
     * @param id {@code String} Id of newly created root authentication session. If {@code null} a random id will be generated.
     * @return Returns created {@code RootAuthenticationSessionModel}. Never returns {@code null}.
     */
    RootAuthenticationSessionModel createRootAuthenticationSession(RealmModel realm, String id);

    /**
     * Returns the root authentication session specified by the provided realm and id.
     * @param realm {@code RealmModel} Can't be {@code null}.
     * @param authenticationSessionId {@code RootAuthenticationSessionModel} If {@code null} then {@code null} will be returned.
     * @return Returns found {@code RootAuthenticationSessionModel} or {@code null} if no root authentication session is found.
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
    void removeAllExpired();

    /**
     * Removes all expired root authentication sessions for the given realm.
     * @param realm {@code RealmModel} Can't be {@code null}.
     *
     *
     * @deprecated manual removal of expired entities should not be used anymore. It is responsibility of the store
     *             implementation to handle expirable entities
     */
    void removeExpired(RealmModel realm);

    /**
     * Removes all associated root authentication sessions to the given realm which was removed.
     * @param realm {@code RealmModel} Can't be {@code null}.
     */
    void onRealmRemoved(RealmModel realm);

    /**
     * Removes all associated root authentication sessions to the given realm and client which was removed.
     * @param realm {@code RealmModel} Can't be {@code null}.
     * @param client {@code ClientModel} Can't be {@code null}.
     */
    void onClientRemoved(RealmModel realm, ClientModel client);

    /**
     * Requests update of authNotes of a root authentication session that is not owned
     * by this instance but might exist somewhere in the cluster.
     * 
     * @param compoundId {@code AuthenticationSessionCompoundId} The method has no effect if {@code null}.
     * @param authNotesFragment {@code Map<String, String>} Map with authNote values.
     * Auth note is removed if the corresponding value in the map is {@code null}. Map itself can't be {@code null}.
     */
    void updateNonlocalSessionAuthNotes(AuthenticationSessionCompoundId compoundId, Map<String, String> authNotesFragment);
}
