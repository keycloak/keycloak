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

package org.keycloak.singleobject.jpa;

import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SingleUseObjectProvider;

/**
 * JPA-based {@link SingleUseObjectProvider} that stores single-use objects in a relational database.
 * <p>
 * Expired entries are not removed on access; a periodic {@link org.keycloak.expiration.jpa.ExpirationTask} handles
 * cleanup. Read operations filter out expired rows via query predicates.
 */
public class JpaSingleUseObjectProvider implements SingleUseObjectProvider {

    private final KeycloakSession session;

    public JpaSingleUseObjectProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void put(String key, long lifespanSeconds, Map<String, String> notes) {
        if (lifespanSeconds <= 0) {
            throw new IllegalArgumentException("lifespanSeconds must be positive");
        }
        getEntityManager().createNamedQuery("insertOrOverwriteSingleUseObject")
                .setParameter("id", key)
                .setParameter("notes", SingleUseObjectSerialization.notesToString(key, notes))
                .setParameter("expire", Time.currentTime() + lifespanSeconds)
                .executeUpdate();
    }

    @Override
    public Map<String, String> get(String key) {
        var notes = getEntityManager().createNamedQuery("findSingleUseObjectNotes", String.class)
                .setParameter("id", key)
                .setParameter("currentTime", Time.currentTime())
                .getSingleResultOrNull();
        if (notes == null) {
            return null;
        }
        return SingleUseObjectSerialization.parseNotes(key, notes);
    }

    @Override
    public Map<String, String> remove(String key) {
        var em = getEntityManager();
        var entity = em.find(SingleUseObjectEntity.class, key, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) {
            return null;
        }
        em.remove(entity);
        if (isExpired(entity.getExpire())) {
            return null;
        }
        return SingleUseObjectSerialization.getNotes(entity);
    }

    @Override
    public boolean replace(String key, Map<String, String> notes) {
        var rows = getEntityManager().createNamedQuery("updateIfNotExpiredSingleUseObject")
                .setParameter("id", key)
                .setParameter("notes", SingleUseObjectSerialization.notesToString(key, notes))
                .setParameter("currentTime", Time.currentTime())
                .executeUpdate();
        return rows == 1;
    }

    @Override
    public boolean putIfAbsent(String key, long lifespanInSeconds) {
        var currentTime = Time.currentTime();
        var rows = getEntityManager().createNamedQuery("insertIfAbsentOrExpiredSingleUseObject")
                .setParameter("id", key)
                .setParameter("notes", SingleUseObjectSerialization.notesToString(key, Map.of()))
                .setParameter("expire", currentTime + lifespanInSeconds)
                .setParameter("currentTime", currentTime)
                .executeUpdate();
        return rows == 1;
    }

    @Override
    public boolean contains(String key) {
        var expireTime = getEntityManager().createNamedQuery("findSingleUseObjectExpireTime", Long.class)
                .setParameter("id", key)
                .getSingleResultOrNull();
        return expireTime != null && !isExpired(expireTime);
    }

    @Override
    public void close() {

    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    private static boolean isExpired(long expire) {
        return expire <= Time.currentTime();
    }
}
