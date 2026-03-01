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

package org.keycloak.models.jpa.session;

import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.keycloak.common.util.Time;
import org.keycloak.models.jpa.entities.RevokedTokenEntity;
import org.keycloak.models.session.RevokedToken;
import org.keycloak.models.session.RevokedTokenPersisterProvider;

/**
 * @author Alexander Schwartz
 */
public class JpaRevokedTokensPersisterProvider implements RevokedTokenPersisterProvider {

    private final EntityManager em;

    public JpaRevokedTokensPersisterProvider(EntityManager em) {
        this.em = em;
    }

    @Override
    public void revokeToken(String tokenId, long lifetime) {
        RevokedTokenEntity revokedTokenEntity = em.find(RevokedTokenEntity.class, tokenId);
        long expire = Time.currentTime() + lifetime;
        if (revokedTokenEntity != null) {
            // The token has already been revoked.
            // There shouldn't be much need to update the expiry of the token, let's do it anyway to be on the safe side.
            if (revokedTokenEntity.getExpire() < expire) {
                revokedTokenEntity.setExpire(expire);
            }
            return;
        }

        revokedTokenEntity = new RevokedTokenEntity();
        revokedTokenEntity.setId(tokenId);
        revokedTokenEntity.setExpire(expire);
        em.persist(revokedTokenEntity);
    }

    @Override
    public Stream<RevokedToken> getAllRevokedTokens() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<RevokedTokenEntity> cq = cb.createQuery(RevokedTokenEntity.class);
        Root<RevokedTokenEntity> rootEntry = cq.from(RevokedTokenEntity.class);
        CriteriaQuery<RevokedTokenEntity> all = cq.select(rootEntry)
                .where(cb.gt(rootEntry.get("expire"), Time.currentTime()));

        TypedQuery<RevokedTokenEntity> allQuery = em.createQuery(all);
        return allQuery.getResultStream().map(revokedTokenEntity -> new RevokedToken(revokedTokenEntity.getId(), revokedTokenEntity.getExpire()));
    }

    @Override
    public void expireTokens() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<RevokedTokenEntity> cd = cb.createCriteriaDelete(RevokedTokenEntity.class);
        Root<RevokedTokenEntity> rootEntry = cd.from(RevokedTokenEntity.class);
        cd.where(cb.lt(rootEntry.get("expire"), Time.currentTime()));
        em.createQuery(cd).executeUpdate();
    }

    @Override
    public void close() {
        // noop
    }
}
