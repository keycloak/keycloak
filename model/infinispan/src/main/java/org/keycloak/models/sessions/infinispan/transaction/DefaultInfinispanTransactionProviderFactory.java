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

package org.keycloak.models.sessions.infinispan.transaction;

import java.sql.Connection;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.Provider;

import org.hibernate.Session;
import org.jboss.logging.Logger;

public class DefaultInfinispanTransactionProviderFactory implements InfinispanTransactionProviderFactory {

    public static final String ID = "default";
    protected static Logger log = Logger.getLogger(DefaultInfinispanTransactionProviderFactory.class);
    private boolean prepareEnabled;

    @Override
    public InfinispanTransactionProvider create(KeycloakSession session) {
        var provider = new DefaultInfinispanTransactionProvider(session);

        if (prepareEnabled) {
            // In the prepare step, check if database entities can be locked. If this is true, no new transactions need to be started in the after-completion phase.
            session.getTransactionManager().enlistPrepare(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    provider.prepareStep();
                }

                @Override
                protected void rollbackImpl() {
                }
            });
        }

        session.getTransactionManager().enlistAfterCompletion(provider);
        return provider;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Only when read-committed is enabled, it is safe to opportunistically upgrade a read lock to a write lock.
        // Should work for PostgreSQL, Oracle and MSSQL that use read committed by default.
        // In all other isolation levels this can lead to a deadlock as seen for example with MariaDB (SnapshotIsolationException / "Record has changed since last read")
        prepareEnabled = KeycloakModelUtils.runJobInTransactionWithResult(factory, session -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            return em.unwrap(Session.class).doReturningWork(connection -> connection.getTransactionIsolation() == Connection.TRANSACTION_READ_COMMITTED);
        });
    }

    @Override
    public void close() {
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(JpaConnectionProvider.class);
    }

    @Override
    public String getId() {
        return ID;
    }
}
