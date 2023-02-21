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
package org.keycloak.models.map.storage.jpa.authSession;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.map.authSession.MapRootAuthenticationSessionEntity;
import org.keycloak.models.map.authSession.MapRootAuthenticationSessionEntityDelegate;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_AUTH_SESSION;

import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.authSession.delegate.JpaRootAuthenticationSessionDelegateProvider;
import org.keycloak.models.map.storage.jpa.authSession.entity.JpaAuthenticationSessionEntity;
import org.keycloak.models.map.storage.jpa.authSession.entity.JpaRootAuthenticationSessionEntity;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import java.sql.Connection;
import java.util.UUID;

public class JpaRootAuthenticationSessionMapKeycloakTransaction extends JpaMapKeycloakTransaction<JpaRootAuthenticationSessionEntity, MapRootAuthenticationSessionEntity, RootAuthenticationSessionModel> {

    public JpaRootAuthenticationSessionMapKeycloakTransaction(KeycloakSession session, EntityManager em) {
        super(session, JpaRootAuthenticationSessionEntity.class, RootAuthenticationSessionModel.class, em);
    }

    @Override
    public Selection<JpaRootAuthenticationSessionEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaRootAuthenticationSessionEntity> root) {
        return cb.construct(JpaRootAuthenticationSessionEntity.class, 
            root.get("id"), 
            root.get("version"),
            root.get("entityVersion"), 
            root.get("realmId"), 
            root.get("timestamp"),
            root.get("expiration")
        );
    }

    @Override
    public void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(CURRENT_SCHEMA_VERSION_AUTH_SESSION);
    }

    @Override
    public JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaRootAuthenticationSessionModelCriteriaBuilder();
    }

    @Override
    protected MapRootAuthenticationSessionEntity mapToEntityDelegate(JpaRootAuthenticationSessionEntity original) {
        return new MapRootAuthenticationSessionEntityDelegate(new JpaRootAuthenticationSessionDelegateProvider(original, em));
    }

    @Override
    public boolean delete(String key) {
        int isolationLevel = em.unwrap(Session.class).doReturningWork(Connection::getTransactionIsolation);
        if (isolationLevel == Connection.TRANSACTION_SERIALIZABLE) {
            // If the isolation level is SERIALIZABLE, there is no need to apply the optimistic locking, as the database with its serializable checks
            // takes care that no-one has modified or deleted the row sind the transaction started. On CockroachDB, using optimistic locking with the added
            // version column in a delete-statement will cause a table lock, which will lead to deadlock.
            // As a workaround, this is using a native query instead, without including the version for optimistic locking.
            if (key == null) return false;
            UUID uuid = StringKeyConverter.UUIDKey.INSTANCE.fromStringSafe(key);
            if (uuid == null) return false;
            removeFromCache(key);
            // will throw an exception if the entity doesn't exist in the Hibernate session or in the database.
            JpaRootAuthenticationSessionEntity rootAuth = em.getReference(JpaRootAuthenticationSessionEntity.class, uuid);
            // will use cascading delete to all child entities
            //noinspection JpaQueryApiInspection
            Query deleteById =
                    em.createNamedQuery("deleteRootAuthenticationSessionByIdNoOptimisticLocking");
            deleteById.unwrap(NativeQuery.class).addSynchronizedQuerySpace(JpaRootAuthenticationSessionEntity.TABLE_NAME,
                    JpaAuthenticationSessionEntity.TABLE_NAME);
            deleteById.setParameter("id", key);
            int deleteCount = deleteById.executeUpdate();
            rootAuth.getAuthenticationSessions().forEach(e -> em.detach(e));
            em.detach(rootAuth);
            if (deleteCount == 1) {
                return true;
            } else if (deleteCount == 0) {
                throw new ModelException("Unable to find root authentication session");
            } else {
                throw new ModelException("Deleted " + deleteCount + " root authentication session when expecting to delete one");
            }
        } else {
            return super.delete(key);
        }
    }
}
