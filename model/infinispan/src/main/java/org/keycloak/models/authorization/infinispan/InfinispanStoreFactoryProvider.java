/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.authorization.infinispan;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.cache.authorization.CachedStoreFactoryProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class InfinispanStoreFactoryProvider implements CachedStoreFactoryProvider {

    private final CacheTransaction transaction;
    private final CachedResourceStore resourceStore;
    private final CachedScopeStore scopeStore;
    private final CachedPolicyStore policyStore;
    private ResourceServerStore resourceServerStore;

    public InfinispanStoreFactoryProvider(KeycloakSession session) {
        this.transaction = new CacheTransaction();
        session.getTransactionManager().enlistAfterCompletion(transaction);
        StoreFactory delegate = session.getProvider(StoreFactory.class);
        resourceStore = new CachedResourceStore(session, this, this.transaction, delegate);
        resourceServerStore = new CachedResourceServerStore(session, this.transaction, delegate);
        scopeStore = new CachedScopeStore(session, this, this.transaction, delegate);
        policyStore = new CachedPolicyStore(session, this, this.transaction, delegate);
    }

    @Override
    public ResourceStore getResourceStore() {
        return resourceStore;
    }

    @Override
    public ResourceServerStore getResourceServerStore() {
        return resourceServerStore;
    }

    @Override
    public ScopeStore getScopeStore() {
        return scopeStore;
    }

    @Override
    public PolicyStore getPolicyStore() {
        return policyStore;
    }

    @Override
    public void close() {

    }

    static class CacheTransaction implements KeycloakTransaction {

        private List<Runnable> completeTasks = new ArrayList<>();
        private List<Runnable> rollbackTasks = new ArrayList<>();

        @Override
        public void begin() {

        }

        @Override
        public void commit() {
            this.completeTasks.forEach(task -> task.run());
        }

        @Override
        public void rollback() {
            this.rollbackTasks.forEach(task -> task.run());
        }

        @Override
        public void setRollbackOnly() {

        }

        @Override
        public boolean getRollbackOnly() {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }

        protected void whenCommit(Runnable task) {
            this.completeTasks.add(task);
        }

        protected void whenRollback(Runnable task) {
            this.rollbackTasks.add(task);
        }
    }
}
