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

package org.keycloak.authentication.jpa;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.SessionExpiration;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import org.jboss.logging.Logger;

public class JpaAuthenticationSessionProvider extends AbstractKeycloakTransaction implements AuthenticationSessionProvider {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final KeycloakSession session;
    private final int authSessionsLimit;
    private final Map<String, RootAuthenticationSessionAdapter> transientSessions = new HashMap<>();
    private KeycloakTransaction transaction;
    private boolean enlisted;

    public JpaAuthenticationSessionProvider(KeycloakSession session, int authSessionsLimit) {
        this.session = Objects.requireNonNull(session);
        this.authSessionsLimit = authSessionsLimit;
    }

    @Override
    public RootAuthenticationSessionModel createRootAuthenticationSession(RealmModel realm) {
        var model = RootAuthenticationSessionAdapter.create(session, realm, SecretGenerator.SECURE_ID_GENERATOR.get(), Time.currentTime(), authSessionsLimit);
        // Those newly created authentication sessions with a random ID do not exist in the database, so there can not be any conflict.
        // For resource owner password grants, those sessions are created temporarily, so we only insert them if they are not removed within the same session.
        transientSessions.put(model.getEntity().getId(), model);
        prepareTransaction();
        return model;
    }

    private void prepareTransaction() {
        if (!enlisted) {
            enlisted = true;
            session.getTransactionManager().enlistPrepare(this);
        }
    }

    @Override
    public RootAuthenticationSessionModel createRootAuthenticationSession(RealmModel realm, String id) {
        if (id == null) {
            return createRootAuthenticationSession(realm);
        }
        var em = getEntityManager();
        em.createNamedQuery("insertRootAuthSessionIfAbsent")
                .setParameter("id", id)
                .setParameter("realmId", realm.getId())
                .setParameter("timestamp", Time.currentTime())
                .executeUpdate();
        var entity = em.find(RootAuthenticationSessionEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) {
            throw new ModelException("Unable to create or find root authentication session with id '" + id + "'");
        }
        var lifespan = SessionExpiration.getAuthSessionLifespan(realm);
        if (entity.getTimestamp() + lifespan < Time.currentTime()) {
            logger.debugf("Root authentication session with id '%s' is expired.", id);
            return null;
        }
        return RootAuthenticationSessionAdapter.wrapEntity(session, realm,  entity, authSessionsLimit);
    }

    @Override
    public RootAuthenticationSessionModel getRootAuthenticationSession(RealmModel realm, String id) {
        if (id == null) {
            return null;
        }
        var model = transientSessions.get(id);
        if (model != null && Objects.equals(model.getRealm().getId(), realm.getId())) {
            // NOTE: Check if this session belongs to the correct realm to avoid a wrong cross-reference
            // as a second line of defense.
            return model;
        }

        var em = getEntityManager();
        var entity = em.find(RootAuthenticationSessionEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) {
            return null;
        }
        var lifespan = SessionExpiration.getAuthSessionLifespan(realm);
        if (entity.getTimestamp() + lifespan < Time.currentTime()) {
            logger.debugf("Root authentication session with id '%s' is expired.", id);
            em.remove(entity);
            return null;
        }
        return RootAuthenticationSessionAdapter.wrapEntity(session, realm, entity, authSessionsLimit);
    }

    @Override
    public void removeRootAuthenticationSession(RealmModel realm, RootAuthenticationSessionModel authenticationSession) {
        if (transientSessions.remove(authenticationSession.getId()) != null) {
            return;
        }
        var em = getEntityManager();
        if (authenticationSession instanceof RootAuthenticationSessionAdapter adapter) {
            em.remove(adapter.getEntity());
            return;
        }
        var entity = em.find(RootAuthenticationSessionEntity.class, authenticationSession.getId(), LockModeType.PESSIMISTIC_WRITE);
        if (entity != null) {
            em.remove(entity);
        }
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        getEntityManager()
                .createNamedQuery("deleteRootAuthSessionByRealm")
                .setParameter("realmId", realm.getId())
                .executeUpdate();
    }

    @Override
    public void close() {

    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    protected void commitImpl() {
        transientSessions.forEach((key, value) -> getEntityManager().persist(value.getEntity()));
    }

    @Override
    protected void rollbackImpl() {
    }
}
