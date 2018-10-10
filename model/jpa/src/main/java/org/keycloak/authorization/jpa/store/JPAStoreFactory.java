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

package org.keycloak.authorization.jpa.store;

import javax.persistence.EntityManager;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JPAStoreFactory implements StoreFactory {

    private final PolicyStore policyStore;
    private final ResourceServerStore resourceServerStore;
    private final ResourceStore resourceStore;
    private final ScopeStore scopeStore;
    private final JPAPermissionTicketStore permissionTicketStore;
    private boolean readOnly;

    public JPAStoreFactory(EntityManager entityManager, AuthorizationProvider provider) {
        policyStore = new JPAPolicyStore(entityManager, provider);
        resourceServerStore = new JPAResourceServerStore(entityManager, provider);
        resourceStore = new JPAResourceStore(entityManager, provider);
        scopeStore = new JPAScopeStore(entityManager, provider);
        permissionTicketStore = new JPAPermissionTicketStore(entityManager, provider);
    }

    @Override
    public PolicyStore getPolicyStore() {
        return policyStore;
    }

    @Override
    public ResourceServerStore getResourceServerStore() {
        return resourceServerStore;
    }

    @Override
    public ResourceStore getResourceStore() {
        return resourceStore;
    }

    @Override
    public ScopeStore getScopeStore() {
        return scopeStore;
    }

    @Override
    public PermissionTicketStore getPermissionTicketStore() {
        return permissionTicketStore;
    }

    @Override
    public void close() {

    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }
}
