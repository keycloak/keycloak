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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserLoginFailureProvider;

public class JpaUserLoginFailureProvider implements UserLoginFailureProvider {

    private final KeycloakSession session;
    private final Set<LoginFailureKey> notInDatabaseCache = new HashSet<>();
    private final Map<LoginFailureKey, UserLoginFailureModel> entityInSession = new HashMap();

    public JpaUserLoginFailureProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public UserLoginFailureModel getUserLoginFailure(RealmModel realm, String userId) {
        var key = new LoginFailureKey(realm.getId(), userId);
        if (notInDatabaseCache.contains(key)) {
            // JPA will cache existing entries in the current persistence context. But if the entry doesn't exist, it would try to look it up multiple times.
            // So with this small cache on the session level, it would only look it up once if it doesn't exist.
            return null;
        }
        UserLoginFailureModel model = entityInSession.get(key);
        if (model != null) {
            // The Model class will refresh the entity, and we need to ensure that no changes get lost.
            // We ensure this by having each entity wrapped with the model only once per session.
            return model;
        }
        var em = getEntityManager();
        var entity = em.find(LoginFailureEntity.class, key);
        if (entity == null) {
            notInDatabaseCache.add(key);
            return null;
        }
        model = new UserLoginFailureAdapter(em, entity);
        entityInSession.put(key, model);
        return model;
    }

    @Override
    public UserLoginFailureModel addUserLoginFailure(RealmModel realm, String userId) {
        var em = getEntityManager();
        em.createNamedQuery("insertLoginFailure")
                .setParameter("realmId", realm.getId())
                .setParameter("userId", userId)
                .executeUpdate();
        var key = new LoginFailureKey(realm.getId(), userId);
        notInDatabaseCache.remove(key);
        var entity = em.find(LoginFailureEntity.class, key);
        UserLoginFailureModel model = new UserLoginFailureAdapter(em, entity);
        entityInSession.put(key, model);
        return model;
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
        entityInSession.remove(key);
        // em.flush() should not be necessary, as there shouldn't be any stale entries.
    }

    @Override
    public void removeAllUserLoginFailures(RealmModel realm) {
        var em = getEntityManager();
        em.createNamedQuery("deleteLoginFailureByRealm")
                    .setParameter("realmId", realm.getId())
                    .executeUpdate();
    }

    @Override
    public void close() {
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }
}
