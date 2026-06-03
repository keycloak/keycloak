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

package org.keycloak.loginfailures.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserLoginFailureProvider;

public class JpaUserLoginFailureProvider implements UserLoginFailureProvider {

    private final KeycloakSession session;
    private final int maxRemovals;

    public JpaUserLoginFailureProvider(KeycloakSession session, int maxRemovals) {
        this.session = session;
        this.maxRemovals = maxRemovals;
    }

    @Override
    public UserLoginFailureModel getUserLoginFailure(RealmModel realm, String userId) {
        var key = new LoginFailureKey(realm.getId(), userId);
        var em = getEntityManager();
        var entity = em.find(LoginFailureEntity.class, key);
        if (entity == null) {
            return null;
        }
        return new UserLoginFailureAdapter(em, entity);
    }

    @Override
    public UserLoginFailureModel addUserLoginFailure(RealmModel realm, String userId) {
        var em = getEntityManager();
        em.createNamedQuery("insertLoginFailure")
                .setParameter("realmId", realm.getId())
                .setParameter("userId", userId)
                .executeUpdate();
        var key = new LoginFailureKey(realm.getId(), userId);
        var entity = em.find(LoginFailureEntity.class, key);
        return new UserLoginFailureAdapter(em, entity);
    }

    @Override
    public void removeUserLoginFailure(RealmModel realm, String userId) {
        var key = new LoginFailureKey(realm.getId(), userId);
        var em = getEntityManager();
        var entity = em.find(LoginFailureEntity.class, key, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) {
            return;
        }
        em.remove(entity);
        em.flush();
    }

    @Override
    public void removeAllUserLoginFailures(RealmModel realm) {
        var em = getEntityManager();
        int removed;
        do {
            var userIds = em.createNamedQuery("findLoginFailureUserIdsByRealm", String.class)
                    .setParameter("realmId", realm.getId())
                    .setMaxResults(maxRemovals)
                    .getResultList();
            if (userIds.isEmpty()) {
                return;
            }
            removed = em.createNamedQuery("deleteLoginFailureByRealmAndUserIds")
                    .setParameter("realmId", realm.getId())
                    .setParameter("userIds", userIds)
                    .executeUpdate();
        } while (removed >= maxRemovals);
    }

    @Override
    public void close() {

    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }
}
